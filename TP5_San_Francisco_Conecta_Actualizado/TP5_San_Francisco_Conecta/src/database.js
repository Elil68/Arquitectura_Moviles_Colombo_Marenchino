'use strict';
const fs=require('node:fs'),path=require('node:path');
let DatabaseSync;
try{({DatabaseSync}=require('node:sqlite'));}catch{throw Error('Esta instalación de Node.js no incluye el módulo SQLite integrado. Se necesita una instalación compatible con node:sqlite; no se descargan paquetes automáticamente.');}
class Database{
 constructor(file){fs.mkdirSync(path.dirname(file),{recursive:true});this.raw=new DatabaseSync(file);this.raw.exec('PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL; PRAGMA synchronous=FULL; PRAGMA busy_timeout=5000;');this.migrate();}
 run(sql,...args){return this.raw.prepare(sql).run(...args);}
 get(sql,...args){return this.raw.prepare(sql).get(...args);}
 all(sql,...args){return this.raw.prepare(sql).all(...args);}
 transaction(fn){this.raw.exec('BEGIN IMMEDIATE');try{const value=fn();this.raw.exec('COMMIT');return value;}catch(e){this.raw.exec('ROLLBACK');throw e;}}
 touch(){this.run('UPDATE metadata SET revision=revision+1 WHERE id=1');}
 revision(){return this.get('SELECT revision FROM metadata WHERE id=1').revision;}
 close(){this.raw.close();}
 migrate(){this.raw.exec(`
 CREATE TABLE IF NOT EXISTS metadata(id INTEGER PRIMARY KEY CHECK(id=1),revision INTEGER NOT NULL DEFAULT 1);
 INSERT OR IGNORE INTO metadata(id) VALUES(1);
 CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY,username TEXT NOT NULL UNIQUE,name TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN('vecino','demo')),account TEXT NOT NULL,salt TEXT NOT NULL,password_hash TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),expires_at INTEGER NOT NULL);
 CREATE INDEX IF NOT EXISTS sessions_expiry ON sessions(expires_at);
 CREATE TABLE IF NOT EXISTS login_attempts(source TEXT PRIMARY KEY,attempts INTEGER NOT NULL,reset_at INTEGER NOT NULL);
 CREATE TABLE IF NOT EXISTS bikes(id TEXT PRIMARY KEY,name TEXT NOT NULL,zone TEXT NOT NULL,x REAL NOT NULL,y REAL NOT NULL,capacity INTEGER NOT NULL,bikes INTEGER NOT NULL CHECK(bikes>=0 AND bikes<=capacity));
 CREATE TABLE IF NOT EXISTS alarms(id TEXT PRIMARY KEY,name TEXT NOT NULL,zone TEXT NOT NULL,x REAL NOT NULL,y REAL NOT NULL,active INTEGER NOT NULL DEFAULT 0 CHECK(active IN(0,1)));
 CREATE TABLE IF NOT EXISTS contacts(id TEXT PRIMARY KEY,payload TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS parking_zones(id TEXT PRIMARY KEY,name TEXT NOT NULL,zone TEXT NOT NULL,x REAL NOT NULL,y REAL NOT NULL,rate_cents INTEGER NOT NULL,hours TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS invoices(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),concept TEXT NOT NULL,period TEXT NOT NULL,amount_cents INTEGER NOT NULL CHECK(amount_cents>0),due_at INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'pendiente' CHECK(status IN('pendiente','pagada')),paid_at INTEGER,receipt TEXT,reminded INTEGER NOT NULL DEFAULT 0);
 CREATE INDEX IF NOT EXISTS invoices_owner ON invoices(user_id,due_at,id);
 CREATE INDEX IF NOT EXISTS invoices_reminders ON invoices(status,reminded,due_at);
 CREATE TABLE IF NOT EXISTS claims(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),category TEXT NOT NULL,zone TEXT NOT NULL,address TEXT NOT NULL,detail TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN('recibido','en proceso','resuelto')),created_at INTEGER NOT NULL);
 CREATE INDEX IF NOT EXISTS claims_owner ON claims(user_id,created_at DESC,id);
 CREATE INDEX IF NOT EXISTS claims_status ON claims(status,created_at DESC);
 CREATE TABLE IF NOT EXISTS claim_history(id INTEGER PRIMARY KEY,claim_id TEXT NOT NULL REFERENCES claims(id) ON DELETE CASCADE,status TEXT NOT NULL,comment TEXT NOT NULL,created_at INTEGER NOT NULL);
 CREATE INDEX IF NOT EXISTS history_claim ON claim_history(claim_id,created_at,id);
 CREATE TABLE IF NOT EXISTS attachments(id TEXT PRIMARY KEY,claim_id TEXT NOT NULL REFERENCES claims(id) ON DELETE CASCADE,name TEXT NOT NULL,mime TEXT NOT NULL,size INTEGER NOT NULL,content BLOB NOT NULL);
 CREATE INDEX IF NOT EXISTS attachments_claim ON attachments(claim_id);
 CREATE TABLE IF NOT EXISTS parking(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),zone_id TEXT NOT NULL REFERENCES parking_zones(id),plate TEXT NOT NULL,start_at INTEGER NOT NULL,end_at INTEGER NOT NULL,minutes INTEGER NOT NULL,amount_cents INTEGER NOT NULL,status TEXT NOT NULL CHECK(status IN('activo','finalizado')));
 CREATE UNIQUE INDEX IF NOT EXISTS parking_active_plate ON parking(plate) WHERE status='activo';
 CREATE INDEX IF NOT EXISTS parking_owner ON parking(user_id,start_at DESC,id);
 CREATE INDEX IF NOT EXISTS parking_expiry ON parking(status,end_at);
 CREATE TABLE IF NOT EXISTS payment_intents(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),purpose TEXT NOT NULL,target TEXT NOT NULL,amount_cents INTEGER NOT NULL,request_key TEXT NOT NULL UNIQUE,payload TEXT NOT NULL,status TEXT NOT NULL DEFAULT 'pendiente',provider_ref TEXT,created_at INTEGER NOT NULL);
 CREATE TABLE IF NOT EXISTS payment_locks(resource TEXT PRIMARY KEY,intent_id TEXT NOT NULL REFERENCES payment_intents(id) ON DELETE CASCADE);
 CREATE TABLE IF NOT EXISTS payments(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),intent_id TEXT UNIQUE REFERENCES payment_intents(id),kind TEXT NOT NULL,ref TEXT NOT NULL,concept TEXT NOT NULL,amount_cents INTEGER NOT NULL,created_at INTEGER NOT NULL,provider_ref TEXT NOT NULL UNIQUE);
 CREATE INDEX IF NOT EXISTS payments_owner ON payments(user_id,kind,created_at DESC,id);
 CREATE TABLE IF NOT EXISTS preferences(user_id TEXT PRIMARY KEY REFERENCES users(id),types TEXT NOT NULL,zones TEXT NOT NULL,frequency TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS notifications(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),event_key TEXT NOT NULL,kind TEXT NOT NULL,zone TEXT NOT NULL,title TEXT NOT NULL,body TEXT NOT NULL,target TEXT NOT NULL,ref TEXT,created_at INTEGER NOT NULL,available_at INTEGER NOT NULL,read_at INTEGER,delivery TEXT NOT NULL DEFAULT 'pendiente',attempts INTEGER NOT NULL DEFAULT 0,retry_at INTEGER NOT NULL DEFAULT 0,UNIQUE(user_id,event_key));
 CREATE INDEX IF NOT EXISTS notifications_owner ON notifications(user_id,available_at DESC,id);
 CREATE INDEX IF NOT EXISTS notifications_delivery ON notifications(delivery,available_at);
 CREATE TABLE IF NOT EXISTS event_keys(key TEXT PRIMARY KEY);
 CREATE TABLE IF NOT EXISTS bike_reports(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),station_id TEXT NOT NULL REFERENCES bikes(id),detail TEXT NOT NULL,created_at INTEGER NOT NULL);
 CREATE INDEX IF NOT EXISTS reports_time ON bike_reports(created_at DESC,id);
 CREATE TABLE IF NOT EXISTS activity(id INTEGER PRIMARY KEY,message TEXT NOT NULL,created_at INTEGER NOT NULL);
 CREATE INDEX IF NOT EXISTS activity_time ON activity(created_at DESC,id);
 `);}
}
module.exports=Database;

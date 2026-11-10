'use strict';
const crypto=require('node:crypto');
const fail=(message,status=400)=>{const e=new Error(message);e.status=status;throw e;};
const id=p=>p+'-'+crypto.randomBytes(8).toString('hex').toUpperCase();
const hashToken=t=>crypto.createHash('sha256').update(t).digest('hex');
function passwordHash(password,salt){return crypto.scryptSync(password,salt,64).toString('hex');}
function createUser(db,{id:userId,username,name,role,account,password}){const salt=crypto.randomBytes(16).toString('hex');db.run('INSERT INTO users VALUES(?,?,?,?,?,?,?)',userId,username,name,role,account,salt,passwordHash(password,salt));}
class Auth{
 constructor(db,config){this.db=db;this.config=config;}
 login(username,password,source){
  const now=Date.now(),a=this.db.get('SELECT * FROM login_attempts WHERE source=?',source);
  if(a&&a.reset_at>now&&a.attempts>=8)fail('Demasiados intentos. Volvé a intentar en unos minutos.',429);
  const u=this.db.get('SELECT * FROM users WHERE username=?',String(username||'').slice(0,100));
  const salt=u?u.salt:'simulated-invalid-salt',candidate=passwordHash(String(password||'').slice(0,200),salt);
  if(!u||!crypto.timingSafeEqual(Buffer.from(candidate,'hex'),Buffer.from(u.password_hash,'hex'))){
   this.db.run('INSERT INTO login_attempts VALUES(?,1,?) ON CONFLICT(source) DO UPDATE SET attempts=CASE WHEN reset_at>? THEN attempts+1 ELSE 1 END,reset_at=CASE WHEN reset_at>? THEN reset_at ELSE ? END',source,now+900000,now,now,now+900000);fail('Usuario o contraseña incorrectos.',401);
  }
  this.db.run('DELETE FROM login_attempts WHERE source=?',source);this.db.run('DELETE FROM sessions WHERE expires_at<=?',now);
  const token=crypto.randomBytes(32).toString('hex');this.db.run('INSERT INTO sessions VALUES(?,?,?)',hashToken(token),u.id,now+this.config.sessionMs);return {token,user:this.publicUser(u)};
 }
 publicUser(u){return {id:u.id,username:u.username,name:u.name,role:u.role,account:u.account};}
 session(token){if(!token)return null;const u=this.db.get('SELECT u.* FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>?',hashToken(token),Date.now());return u?this.publicUser(u):null;}
 logout(token){if(token)this.db.run('DELETE FROM sessions WHERE token_hash=?',hashToken(token));}
 register(admin,b){
  if(admin.role!=='demo')fail('Sin permisos.',403);
  if(typeof b.username!=='string'||!/^[-a-zA-Z0-9_]{3,40}$/.test(b.username)||typeof b.password!=='string'||b.password.length<12||b.password.length>100||typeof b.name!=='string'||!b.name.trim()||b.name.length>100)fail('Completá nombre, usuario (3 a 40 caracteres) y contraseña (12 a 100 caracteres).');
  if(this.db.get('SELECT id FROM users WHERE username=?',b.username))fail('Ese usuario ya existe.',409);
  this.db.transaction(()=>{const uid=id('USR');createUser(this.db,{id:uid,username:b.username,name:b.name.trim(),role:'vecino',account:uid,password:b.password});if(this.config.demo)require('./initialize').seedResident(this.db,uid);else this.db.run('INSERT INTO preferences VALUES(?,?,?,?)',uid,'[]','[]','inmediata');this.db.touch();});
 }
}
module.exports={Auth,createUser,fail,id};

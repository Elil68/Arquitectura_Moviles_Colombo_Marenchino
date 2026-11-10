'use strict';
const seed=require('./seed');
const {createUser}=require('./security');
const demoAccounts=[{id:'USR-1',username:'vecino',name:'Matías Marenchino',role:'vecino',account:'VEC-001',password:'VecinoDemo!2026'},{id:'USR-2',username:'vecina',name:'Elías Colombo',role:'vecino',account:'VEC-002',password:'VecinaDemo!2026'},{id:'USR-ADMIN',username:'admin',name:'Administración municipal',role:'demo',account:'ADM-001',password:'AdminDemo!2026'}];
function businessSeed(db){const s=seed();
 for(const b of s.bikes)db.run('INSERT INTO bikes VALUES(?,?,?,?,?,?,?)',b.id,b.name,b.zone,b.x,b.y,b.capacity,b.bikes);
 for(const a of s.alarms)db.run('INSERT INTO alarms VALUES(?,?,?,?,?,?)',a.id,a.name,a.zone,a.x,a.y,Number(a.active));
 for(const c of s.contacts)db.run('INSERT INTO contacts VALUES(?,?)',c.id,JSON.stringify(c));
 for(const z of s.parkingZones)db.run('INSERT INTO parking_zones VALUES(?,?,?,?,?,?,?)',z.id,z.name,z.zone,z.x,z.y,Math.round(z.rate*100),z.hours);
 for(const u of db.all("SELECT id FROM users WHERE role='vecino'"))seedResident(db,u.id);
}
function seedResident(db,userId){const s=seed(),prefix=userId==='USR-1'?'CED-':'CED-'+userId+'-';
 db.run('INSERT OR IGNORE INTO preferences VALUES(?,?,?,?)',userId,JSON.stringify(s.preferences.types),JSON.stringify(s.zones),'inmediata');
 for(const [idx,i]of s.invoices.entries()){const id=prefix+String(idx+1).padStart(3,'0'),receipt=i.status==='pagada'?'PAG-'+userId+'-INICIAL':null;db.run('INSERT INTO invoices(id,user_id,concept,period,amount_cents,due_at,status,paid_at,receipt) VALUES(?,?,?,?,?,?,?,?,?)',id,userId,i.concept,i.period,Math.round(i.amount*100),new Date(i.dueAt).getTime(),i.status,i.paidAt?new Date(i.paidAt).getTime():null,receipt);if(receipt)db.run('INSERT INTO payments(id,user_id,kind,ref,concept,amount_cents,created_at,provider_ref) VALUES(?,?,?,?,?,?,?,?)',receipt,userId,'tributaria',id,i.concept,Math.round(i.amount*100),new Date(i.paidAt).getTime(),receipt);}
}
function initialize(db,config){if(db.get("SELECT value FROM settings WHERE key='initialized'"))return;
 db.transaction(()=>{if(config.demo){for(const u of demoAccounts)createUser(db,u);}else{if(!config.bootstrap||config.bootstrap.password.length<12)throw Error('Para crear la primera cuenta de producción se requieren ADMIN_USER y ADMIN_PASSWORD (mínimo 12 caracteres).');createUser(db,{id:'USR-ADMIN',...config.bootstrap,name:'Administración municipal',role:'demo',account:'ADM-001'});}businessSeed(db);db.run("INSERT INTO settings VALUES('initialized','1')");});
}
function resetBusiness(db){for(const table of ['notifications','event_keys','bike_reports','claim_history','attachments','claims','payments','payment_intents','parking','invoices','preferences','bikes','alarms','contacts','parking_zones','activity'])db.run('DELETE FROM '+table);businessSeed(db);}
module.exports={initialize,demoAccounts,resetBusiness,seedResident};

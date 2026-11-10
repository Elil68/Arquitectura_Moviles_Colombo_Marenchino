'use strict';
const {fail}=require('./security');
const iso=n=>n==null?null:new Date(n).toISOString();
const paging=(page=1,limit=10)=>({page:Math.max(1,Math.min(100000,Math.trunc(Number(page)||1))),limit:Math.max(1,Math.min(50,Math.trunc(Number(limit)||10)))});
const inv=r=>({id:r.id,concept:r.concept,period:r.period,amount:r.amount_cents/100,dueAt:iso(r.due_at),status:r.status,paidAt:iso(r.paid_at),receipt:r.receipt});
const pay=r=>({id:r.id,type:r.kind,ref:r.ref,concept:r.concept,amount:r.amount_cents/100,at:iso(r.created_at)});
const claim=r=>({id:r.id,category:r.category,zone:r.zone,address:r.address,detail:r.detail,status:r.status,at:iso(r.created_at)});
const notice=r=>({id:r.id,type:r.kind,zone:r.zone,title:r.title,body:r.body,target:r.target,ref:r.ref,at:iso(r.created_at),read:r.read_at!==null});
class Repository{
 constructor(db){this.db=db;}
 page(select,count,args,page=1,limit=10,map=x=>x){const p=paging(page,limit),total=this.db.get(count,...args).total;return {items:this.db.all(select+' LIMIT ? OFFSET ?',...args,p.limit,(p.page-1)*p.limit).map(map),...p,total,pages:Math.max(1,Math.ceil(total/p.limit))};}
 bikes(){return this.db.all('SELECT * FROM bikes ORDER BY id');}
 alarms(){return this.db.all('SELECT * FROM alarms ORDER BY id').map(a=>({...a,active:!!a.active}));}
 zones(){return this.db.all('SELECT * FROM parking_zones ORDER BY id').map(z=>({...z,rate:z.rate_cents/100}));}
 contacts(){return this.db.all('SELECT payload FROM contacts ORDER BY id').map(c=>JSON.parse(c.payload));}
 preferences(userId){const r=this.db.get('SELECT * FROM preferences WHERE user_id=?',userId);return r?{types:JSON.parse(r.types),zones:JSON.parse(r.zones),frequency:r.frequency}:{types:[],zones:[],frequency:'inmediata'};}
 invoices(userId,page){return this.page('SELECT * FROM invoices WHERE user_id=? ORDER BY due_at,id','SELECT count(*) total FROM invoices WHERE user_id=?',[userId],page,10,inv);}
 invoice(userId,id){const r=this.db.get('SELECT * FROM invoices WHERE user_id=? AND id=?',userId,id);if(!r)fail('Cedulón no encontrado.',404);return inv(r);}
 payments(userId,kind,page){return this.page('SELECT * FROM payments WHERE user_id=? AND kind=? ORDER BY created_at DESC,id','SELECT count(*) total FROM payments WHERE user_id=? AND kind=?',[userId,kind],page,10,pay);}
 payment(userId,id){const r=this.db.get('SELECT * FROM payments WHERE user_id=? AND id=?',userId,id);if(!r)fail('Comprobante no encontrado.',404);return pay(r);}
 claims(userId,page,q='',all=false){const owner=all?'1=1':'user_id=?',args=all?[]:[userId],needle=String(q).trim().slice(0,150);let where=owner;if(needle){where+=' AND (id LIKE ? ESCAPE \'!\' OR category LIKE ? ESCAPE \'!\' OR address LIKE ? ESCAPE \'!\' OR detail LIKE ? ESCAPE \'!\')';const search='%'+needle.replace(/[!%_]/g,'!$&')+'%';args.push(search,search,search,search);}return this.page('SELECT * FROM claims WHERE '+where+' ORDER BY created_at DESC,id','SELECT count(*) total FROM claims WHERE '+where,args,page,10,claim);}
 claim(user,id){const r=this.db.get('SELECT * FROM claims WHERE id=?'+(user.role==='demo'?'':' AND user_id=?'),...[id,...(user.role==='demo'?[]:[user.id])]);if(!r)fail('Reclamo no encontrado.',404);return {...claim(r),history:this.db.all('SELECT * FROM claim_history WHERE claim_id=? ORDER BY created_at,id',id).map(h=>({status:h.status,comment:h.comment,at:iso(h.created_at)})),files:this.db.all('SELECT id,name,mime type,size FROM attachments WHERE claim_id=?',id)};}
 attachment(user,id){const r=this.db.get('SELECT a.* FROM attachments a JOIN claims c ON c.id=a.claim_id WHERE a.id=?'+(user.role==='demo'?'':' AND c.user_id=?'),...[id,...(user.role==='demo'?[]:[user.id])]);if(!r)fail('Adjunto no encontrado.',404);return r;}
 parkingRow(r){return {id:r.id,zoneId:r.zone_id,zone:r.zone,zoneName:r.zone_name,plate:r.plate,startAt:iso(r.start_at),endAt:iso(r.end_at),minutes:r.minutes,amount:r.amount_cents/100,status:r.status};}
 parking(userId,page,all=false){const where=all?'1=1':'p.user_id=?',args=all?[]:[userId];return this.page('SELECT p.*,z.zone,z.name zone_name FROM parking p JOIN parking_zones z ON z.id=p.zone_id WHERE '+where+' ORDER BY p.start_at DESC,p.id','SELECT count(*) total FROM parking p WHERE '+where,args,page,10,r=>this.parkingRow(r));}
 activeParking(userId,all=false){return this.db.all('SELECT p.*,z.zone,z.name zone_name FROM parking p JOIN parking_zones z ON z.id=p.zone_id WHERE p.status=\'activo\''+(all?'':' AND p.user_id=?')+' ORDER BY p.end_at LIMIT 50',...(all?[]:[userId])).map(r=>this.parkingRow(r));}
 notices(userId,page){const now=Date.now();return this.page('SELECT * FROM notifications WHERE user_id=? AND available_at<=? ORDER BY available_at DESC,id','SELECT count(*) total FROM notifications WHERE user_id=? AND available_at<=?',[userId,now],page,10,notice);}
 unread(userId){return this.db.get('SELECT count(*) n FROM notifications WHERE user_id=? AND available_at<=? AND read_at IS NULL',userId,Date.now()).n;}
 summary(userId){return {debt:this.db.get("SELECT coalesce(sum(amount_cents),0) n FROM invoices WHERE user_id=? AND status='pendiente'",userId).n/100,openClaims:this.db.get("SELECT count(*) n FROM claims WHERE user_id=? AND status!='resuelto'",userId).n,parking:this.activeParking(userId)[0]||null};}
 reports(page){return this.page('SELECT r.*,b.name station_name FROM bike_reports r JOIN bikes b ON b.id=r.station_id ORDER BY r.created_at DESC,r.id','SELECT count(*) total FROM bike_reports',[],page,10,r=>({id:r.id,station:r.station_id,stationName:r.station_name,detail:r.detail,at:iso(r.created_at)}));}
 activity(){return this.db.all('SELECT * FROM activity ORDER BY created_at DESC,id DESC LIMIT 15').map(a=>({message:a.message,at:iso(a.created_at)}));}
 unit(fn){return this.db.transaction(fn);}
 touch(){this.db.touch();}
 revision(){return this.db.revision();}
 resetDemo(){require('./initialize').resetBusiness(this.db);}
 audit(message){this.db.run('INSERT INTO activity(message,created_at) VALUES(?,?)',message,Date.now());}
 row(table,id){const allowed=['invoices','parking','bikes','alarms','claims','parking_zones'];if(!allowed.includes(table))throw Error('Tabla inválida.');return this.db.get('SELECT * FROM '+table+' WHERE id=?',id);}
 owned(table,user,id){const row=this.row(table,id);if(!row||row.user_id!==user.id)fail('Registro no encontrado.',404);return row;}
 claimCreate(user,c,files){this.db.run('INSERT INTO claims VALUES(?,?,?,?,?,?,?,?)',c.id,user.id,c.category,c.zone,c.address,c.detail,'recibido',c.at);this.db.run('INSERT INTO claim_history(claim_id,status,comment,created_at) VALUES(?,?,?,?)',c.id,'recibido','Reclamo recibido.',c.at);for(const f of files)this.db.run('INSERT INTO attachments VALUES(?,?,?,?,?,?)',f.id,c.id,f.name,f.type,f.content.length,f.content);}
 claimProgress(id,status,comment){this.db.run('UPDATE claims SET status=? WHERE id=?',status,id);this.db.run('INSERT INTO claim_history(claim_id,status,comment,created_at) VALUES(?,?,?,?)',id,status,comment,Date.now());}
 bikeReport(user,station,detail,id){this.db.run('INSERT INTO bike_reports VALUES(?,?,?,?,?)',id,user.id,station,detail,Date.now());}
 bikeSet(id,count){this.db.run('UPDATE bikes SET bikes=? WHERE id=?',count,id);}
 alarmSet(id,active){this.db.run('UPDATE alarms SET active=? WHERE id=?',Number(active),id);}
 preferenceSet(user,p){this.db.run('INSERT INTO preferences VALUES(?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET types=excluded.types,zones=excluded.zones,frequency=excluded.frequency',user.id,JSON.stringify(p.types),JSON.stringify(p.zones),p.frequency);}
 notificationRead(user,ref,all=false){this.db.run('UPDATE notifications SET read_at=? WHERE user_id=? AND available_at<=?'+(all?'':' AND id=?'),Date.now(),user.id,Date.now(),...(all?[]:[ref]));}
 eventOnce(key){return this.db.run('INSERT OR IGNORE INTO event_keys VALUES(?)',key).changes>0;}
 subscribers(){return this.db.all("SELECT p.* FROM preferences p JOIN users u ON u.id=p.user_id WHERE u.role='vecino'");}
 notify(userId,event,key,availableAt){this.db.run('INSERT OR IGNORE INTO notifications(id,user_id,event_key,kind,zone,title,body,target,ref,created_at,available_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)',event.id,userId,key,event.type,event.zone,event.title,event.body,event.target,event.ref||null,Date.now(),availableAt);}
 deliveryPending(){return this.db.all("SELECT * FROM notifications WHERE delivery='pendiente' AND available_at<=? AND retry_at<=? AND attempts<8 ORDER BY available_at LIMIT 30",Date.now(),Date.now());}
 deliveryResult(n,ok){this.db.run('UPDATE notifications SET delivery=?,attempts=attempts+1,retry_at=? WHERE id=?',ok?'entregado':'pendiente',ok?0:Date.now()+Math.min(60000,1000*2**n.attempts),n.id);}
 taxReminders(){return this.db.all("SELECT * FROM invoices WHERE status='pendiente' AND reminded=0 AND due_at<=? LIMIT 100",Date.now()+7*86400000);}
 markReminded(id){this.db.run('UPDATE invoices SET reminded=1 WHERE id=?',id);}
 expiring(){return this.db.all("SELECT p.*,z.zone,z.name zone_name FROM parking p JOIN parking_zones z ON z.id=p.zone_id WHERE p.status='activo' AND p.end_at<=? ORDER BY p.end_at LIMIT 100",Date.now()+60000);}
 parkingEnd(id){this.db.run("UPDATE parking SET status='finalizado' WHERE id=?",id);}
 activePlate(plate){return this.db.get("SELECT id FROM parking WHERE plate=? AND status='activo'",plate);}
 parkingClock(id,endAt){this.db.run('UPDATE parking SET end_at=? WHERE id=?',endAt,id);}
 intent(key){return this.db.get('SELECT * FROM payment_intents WHERE request_key=?',key);}
 newIntent(i,resource=null){this.unit(()=>{if(resource&&this.db.get('SELECT resource FROM payment_locks WHERE resource=?',resource))fail('Hay una autorización de pago pendiente para esta patente. Reintentá la operación original.',409);this.db.run('INSERT INTO payment_intents(id,user_id,purpose,target,amount_cents,request_key,payload,created_at) VALUES(?,?,?,?,?,?,?,?)',i.id,i.userId,i.purpose,i.target,i.amount,i.key,JSON.stringify(i.payload),Date.now());if(resource)this.db.run('INSERT INTO payment_locks VALUES(?,?)',resource,i.id);});}
 rejectIntent(id){this.unit(()=>{this.db.run("UPDATE payment_intents SET status='rechazado' WHERE id=?",id);this.db.run('DELETE FROM payment_locks WHERE intent_id=?',id);});}
 completeIntent(i,payment,providerRef){this.db.run("UPDATE payment_intents SET status='confirmado',provider_ref=? WHERE id=?",providerRef,i.id);this.db.run('INSERT INTO payments VALUES(?,?,?,?,?,?,?,?,?)',payment.id,i.user_id,i.id,payment.kind,payment.ref,payment.concept,i.amount_cents,Date.now(),providerRef);this.db.run('DELETE FROM payment_locks WHERE intent_id=?',i.id);}
 taxPaid(id,receipt){this.db.run("UPDATE invoices SET status='pagada',paid_at=?,receipt=? WHERE id=? AND status='pendiente'",Date.now(),receipt,id);}
 parkingCreate(p){this.db.run('INSERT INTO parking VALUES(?,?,?,?,?,?,?,?,?)',p.id,p.userId,p.zone,p.plate,p.start,p.end,p.minutes,p.amount,'activo');}
 parkingExtend(p,minutes,amount){this.db.run("UPDATE parking SET end_at=?,minutes=minutes+?,amount_cents=amount_cents+?,status='activo' WHERE id=?",Math.max(Date.now(),p.end_at)+minutes*60000,minutes,amount,p.id);}
}
module.exports={Repository,paging};

'use strict';
const {fail,id}=require('./security');
const TYPES=['agua','obras','eventos','alarmas','tributaria','estacionamiento','reclamos','bicicletas'],ZONES=['Centro','Norte','Sur','Este'];
const text=(v,label,max=400)=>{if(typeof v!=='string'||!v.trim()||v.trim().length>max)fail(label+': completá entre 1 y '+max+' caracteres.');return v.trim();};
const choose=(v,options,label)=>{if(!options.includes(v))fail(label+' inválido.');return v;};
const integer=(v,min,max,label)=>{if(v===null||v===undefined||v==='')fail(label+' inválido.');const n=Number(v);if(!Number.isInteger(n)||n<min||n>max)fail(label+' inválido.');return n;};
const info=p=>{const {items,...meta}=p;return meta;};
const admin=u=>{if(u.role!=='demo')fail('No tenés permisos de administración.',403);};
function files(list=[]){if(!Array.isArray(list)||list.length>2)fail('Podés adjuntar hasta dos archivos de 2 MB.');return list.map(f=>{
 const name=text(f.name,'Nombre',100).replace(/[\\/<>:"|?*\u0000-\u001f]/g,'_'),type=choose(f.type,['image/jpeg','image/png','application/pdf','text/plain'],'Formato');
 if(typeof f.base64!=='string'||!f.base64.length||f.base64.length>2800000||!/^[A-Za-z0-9+/]*={0,2}$/.test(f.base64))fail('Archivo inválido.');
 const b=Buffer.from(f.base64,'base64');if(!b.length||b.length>2097152)fail('Cada archivo debe pesar entre 1 byte y 2 MB.');
 if(type==='image/png'&&!b.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])))fail('El contenido no es PNG.');
 if(type==='image/jpeg'&&(b[0]!==255||b[1]!==216||b[2]!==255))fail('El contenido no es JPEG.');
 if(type==='application/pdf'&&b.subarray(0,5).toString()!=='%PDF-')fail('El contenido no es PDF.');
 if(type==='text/plain'&&b.includes(0))fail('El contenido no es texto.');return {id:id('ADJ'),name,type,content:b};
});}
class NotificationService{
 constructor(repo,provider){this.r=repo;this.provider=provider;this.flushing=false;}
 event(event,{key=id('EVT'),userId=null}={}){
  if(!this.r.eventOnce(key))return;
  for(const p of this.r.subscribers())if((!userId||p.user_id===userId)&&JSON.parse(p.types).includes(event.type)&&JSON.parse(p.zones).includes(event.zone)){
   this.r.notify(p.user_id,{...event,id:id('AVI')},key,Date.now()+(p.frequency==='resumen'?60000:0));
  }
 }
 async flush(){if(this.flushing)return;this.flushing=true;try{const results=[];for(const n of this.r.deliveryPending()){try{const out=await this.provider.deliver({id:n.id,userId:n.user_id,title:n.title,body:n.body,type:n.kind,zone:n.zone,target:n.target});results.push([n,out.status==='delivered']);}catch{results.push([n,false]);}}if(results.length)this.r.unit(()=>{for(const [n,ok]of results)this.r.deliveryResult(n,ok);});}finally{this.flushing=false;}}
}
class PaymentService{
 constructor(repo,provider,notices){this.r=repo;this.provider=provider;this.n=notices;}
 async execute(user,purpose,target,amount,payload,key,resource=null){
  let intent=this.r.intent(key);
  if(intent&&(intent.user_id!==user.id||intent.payload!==JSON.stringify(payload)))fail('La referencia de pago ya fue usada para otra operación.',409);
  if(!intent){this.r.newIntent({id:id('INT'),userId:user.id,purpose,target,amount,payload,key},resource);intent=this.r.intent(key);}
  if(intent.status==='confirmado')return {confirmed:true};
  if(intent.status==='rechazado')fail('El pago fue rechazado. Iniciá una nueva operación.',402);
  const result=await this.provider.charge({amountCents:intent.amount_cents,currency:'ARS',reference:intent.target,idempotencyKey:intent.request_key});
  if(result.status==='declined'){this.r.rejectIntent(intent.id);fail('El proveedor rechazó el pago.',402);}
  if(result.status!=='approved')return {pending:true,message:'El proveedor todavía no confirmó el pago. Reintentá desde este mismo formulario.'};
  if(typeof result.providerRef!=='string'||!result.providerRef)throw Error('Respuesta de pago sin comprobante del proveedor.');
  return this.r.unit(()=>{
   intent=this.r.intent(key);if(intent.status==='confirmado')return {confirmed:true};
   const body=JSON.parse(intent.payload),payment={id:id('PAG'),kind:purpose==='tax'?'tributaria':'estacionamiento'};
   if(purpose==='tax'){
    const inv=this.r.owned('invoices',user,target);if(inv.status!=='pendiente')fail('El cedulón ya fue pagado.',409);
    payment.ref=inv.id;payment.concept=inv.concept;this.r.taxPaid(inv.id,payment.id);
    this.n.event({type:'tributaria',zone:'Centro',title:'Pago simulado confirmado',body:inv.id+' · Comprobante disponible en tu historial.',target:'tributaria',ref:inv.id},{userId:user.id});
   }else if(purpose==='start'){
    if(this.r.activePlate(body.plate))fail('Esta patente ya tiene una sesión activa.',409);
    const now=Date.now(),zone=this.r.row('parking_zones',body.zone);payment.ref=intent.target;payment.concept='Estacionamiento · '+zone.name;
    this.r.parkingCreate({id:intent.target,userId:user.id,zone:zone.id,plate:body.plate,start:now,end:now+body.minutes*60000,minutes:body.minutes,amount:intent.amount_cents});
   }else{
    const p=this.r.owned('parking',user,target);if(p.minutes+body.minutes>480)fail('El estacionamiento no admite esa extensión.');
    const zone=this.r.row('parking_zones',p.zone_id);payment.ref=p.id;payment.concept='Extensión · '+zone.name;this.r.parkingExtend(p,body.minutes,intent.amount_cents);
   }
   this.r.completeIntent(intent,payment,result.providerRef);this.r.audit('Pago confirmado: '+payment.id);this.r.touch();return {confirmed:true};
  });
 }
 async tax(user,b){const inv=this.r.owned('invoices',user,b.id);if(inv.status==='pagada')fail('Este cedulón ya fue pagado.',409);return this.execute(user,'tax',inv.id,inv.amount_cents,{invoice:inv.id},'tax:'+user.id+':'+inv.id);}
 async start(user,b){const zone=this.r.row('parking_zones',b.zone);if(!zone)fail('Zona no encontrada.',404);const minutes=integer(b.minutes,1,240,'Minutos'),plate=text(b.plate,'Patente',8).toUpperCase();if(!/^[A-Z0-9]{6,8}$/.test(plate))fail('Patente inválida.');const key='start:'+user.id+':'+text(b.requestKey,'Referencia de operación',100),previous=this.r.intent(key);if(this.r.activePlate(plate)&&!previous)fail('La patente ya tiene un estacionamiento activo.',409);return this.execute(user,'start',previous?.target||id('EST'),Math.round(zone.rate_cents*minutes/60),{zone:zone.id,minutes,plate},key,'plate:'+plate);}
 async extend(user,b){const p=this.r.owned('parking',user,b.id),minutes=integer(b.minutes,1,240,'Minutos'),key='extend:'+user.id+':'+text(b.requestKey,'Referencia de operación',100);if(!this.r.intent(key)&&(p.status!=='activo'||p.end_at<=Date.now()||p.minutes+minutes>480))fail('No se puede extender ese estacionamiento.');const z=this.r.row('parking_zones',p.zone_id);return this.execute(user,'extend',p.id,Math.round(z.rate_cents*minutes/60),{minutes,parking:p.id},key,'plate:'+p.plate);}
}
class Services{
 constructor(repo,adapters,config){this.r=repo;this.a=adapters;this.config=config;this.n=new NotificationService(repo,adapters.notifications);this.payments=new PaymentService(repo,adapters.payments,this.n);this.tickCount=0;}
 me(user){return {role:user.role,user,profile:{name:user.name,account:user.account,position:{x:46,y:54},address:'Domicilio de demostración'},zones:ZONES,unread:this.r.unread(user.id),revision:this.r.revision()};}
 async view(section,user,q){const page=q.get('page')||1;
  if(section==='home')return {summary:this.r.summary(user.id),notices:this.r.notices(user.id,1).items.slice(0,3)};
  if(section==='bikes')return {bikes:await this.a.bicycles.list(),location:await this.a.location.locate()};
  if(section==='taxes'){const invoices=await this.a.taxes.list({userId:user.id,page:q.get('invoicesPage')||1}),payments=this.r.payments(user.id,'tributaria',q.get('paymentsPage')||1);return {invoices:invoices.items,payments:payments.items,pagination:{invoices:info(invoices),payments:info(payments)}};}
  if(section==='claims'){const claims=this.r.claims(user.id,page,q.get('q')||'');return {claims:claims.items,pagination:{claims:info(claims)}};}
  if(section==='contacts')return {contacts:await this.a.directory.list()};
  if(section==='alarms')return {alarms:await this.a.alarms.list(),preferences:this.r.preferences(user.id)};
  if(section==='parking'){const parking=this.r.parking(user.id,page),payments=this.r.payments(user.id,'estacionamiento',q.get('paymentsPage')||1),active=this.r.activeParking(user.id);return {parkingZones:this.r.zones(),parking:[...active,...parking.items.filter(p=>!active.some(a=>a.id===p.id))],payments:payments.items,pagination:{parking:info(parking),payments:info(payments)}};}
  if(section==='notices'){const notices=this.r.notices(user.id,page);return {notices:notices.items,preferences:this.r.preferences(user.id),pagination:{notices:info(notices)}};}
  if(section==='demo'){admin(user);const claims=this.r.claims(user.id,q.get('claimsPage')||1,'',true),reports=this.r.reports(q.get('reportsPage')||1);return {bikes:await this.a.bicycles.list(),alarms:await this.a.alarms.list(),claims:claims.items,bikeReports:reports.items,parking:this.r.activeParking(user.id,true),activity:this.r.activity(),pagination:{claims:info(claims),reports:info(reports)}};}
  fail('Servicio no encontrado.',404);
 }
 async tick(force=false){
  this.r.unit(()=>{
   let changed=false;
   if(force||++this.tickCount%60===0)for(const inv of this.r.taxReminders()){
    this.n.event({type:'tributaria',zone:'Centro',title:inv.due_at<Date.now()?'Tenés un cedulón vencido':'Un cedulón está por vencer',body:inv.id+' · Consultá el importe y el vencimiento.',target:'tributaria',ref:inv.id},{key:'reminder:'+inv.id,userId:inv.user_id});this.r.markReminded(inv.id);changed=true;
   }
   for(const p of this.r.expiring()){
    if(p.end_at<=Date.now()){this.r.parkingEnd(p.id);this.n.event({type:'estacionamiento',zone:p.zone,title:'Finalizó tu estacionamiento',body:p.plate+' · '+p.zone_name,target:'estacionamiento',ref:p.id},{key:'expired:'+p.id,userId:p.user_id});changed=true;}
    else{const key='warning:'+p.id+':'+p.end_at;this.n.event({type:'estacionamiento',zone:p.zone,title:'Tu estacionamiento está por vencer',body:p.plate+' · Queda menos de un minuto.',target:'estacionamiento',ref:p.id},{key,userId:p.user_id});}
   }
   if(changed)this.r.touch();
  });await this.n.flush();
 }
 async mutate(url,b,user){let result={};
  if(url==='/api/tax/pay')result=await this.payments.tax(user,b);
  else if(url==='/api/parking/start')result=await this.payments.start(user,b);
  else if(url==='/api/parking/extend')result=await this.payments.extend(user,b);
  else this.r.unit(()=>{
   if(url==='/api/claims'){
    const c={id:id('REC'),category:choose(b.category,['Alumbrado','Limpieza','Calles','Agua','Otros'],'Categoría'),zone:choose(b.zone,ZONES,'Zona'),address:text(b.address,'Dirección',150),detail:text(b.detail,'Descripción',1000),at:Date.now()},attachments=files(b.files);this.r.claimCreate(user,c,attachments);result.id=c.id;
    this.n.event({type:'reclamos',zone:c.zone,title:'Recibimos tu reclamo',body:c.id+' · '+c.category,target:'reclamos',ref:c.id},{userId:user.id});this.r.audit('Reclamo creado: '+c.id);
   }else if(url==='/api/bike-reports'){
    if(!this.r.row('bikes',b.station))fail('Estación no encontrada.',404);this.r.bikeReport(user,b.station,text(b.detail,'Descripción',500),id('REP'));
   }else if(url==='/api/preferences'){
    if(!Array.isArray(b.types)||!Array.isArray(b.zones)||b.types.some(t=>!TYPES.includes(t))||b.zones.some(z=>!ZONES.includes(z)))fail('Preferencias inválidas.');this.r.preferenceSet(user,{types:[...new Set(b.types)],zones:[...new Set(b.zones)],frequency:choose(b.frequency,['inmediata','resumen'],'Frecuencia')});
   }else if(url==='/api/notices/read')this.r.notificationRead(user,b.id,!!b.all);
   else if(url==='/api/demo/bikes'){
    admin(user);if(!this.config.demo)fail('Control simulado deshabilitado.',403);const station=this.r.row('bikes',b.id);if(!station)fail('Estación no encontrada.',404);this.r.bikeSet(station.id,integer(b.bikes,0,station.capacity,'Bicicletas'));this.r.audit('Disponibilidad actualizada: '+station.id);
   }else if(url==='/api/demo/alarm'){
    admin(user);if(!this.config.demo)fail('Control simulado deshabilitado.',403);const alarm=this.r.row('alarms',b.id);if(!alarm)fail('Alarma no encontrada.',404);if(typeof b.active!=='boolean')fail('Estado inválido.');this.r.alarmSet(alarm.id,b.active);if(b.active&&!alarm.active)this.n.event({type:'alarmas',zone:alarm.zone,title:'Alarma activada en '+alarm.zone,body:alarm.name+' · Activación de demostración.',target:'alarmas',ref:alarm.id});this.r.audit('Alarma actualizada: '+alarm.id);
   }else if(url==='/api/demo/claim'){
    admin(user);const c=this.r.row('claims',b.id),next=choose(b.status,['en proceso','resuelto'],'Estado');if(!c)fail('Reclamo no encontrado.',404);if((c.status==='recibido'&&next!=='en proceso')||(c.status==='en proceso'&&next!=='resuelto')||c.status==='resuelto')fail('El reclamo debe avanzar de recibido a en proceso y luego a resuelto.');const comment=text(b.comment,'Comentario');this.r.claimProgress(c.id,next,comment);this.n.event({type:'reclamos',zone:c.zone,title:'Tu reclamo está '+next,body:c.id+' · '+comment,target:'reclamos',ref:c.id},{userId:c.user_id});
   }else if(url==='/api/demo/event'){
    admin(user);this.n.event({type:choose(b.type,['agua','obras','eventos'],'Tipo'),zone:choose(b.zone,ZONES,'Zona'),title:text(b.title,'Título',90),body:text(b.body,'Mensaje',600),target:'avisos'});this.r.audit('Aviso vecinal publicado.');
   }else if(url==='/api/demo/parking'){
    admin(user);if(!this.config.demo)fail('El reloj simulado no está habilitado.',403);const p=this.r.row('parking',b.id);if(!p||p.status!=='activo')fail('Estacionamiento no disponible.');choose(b.action,['aviso','vencer'],'Acción');this.r.parkingClock(p.id,Date.now()+(b.action==='aviso'?45000:-1000));
   }else if(url==='/api/demo/reset'){
    admin(user);if(!this.config.demo)fail('Restablecimiento de demostración deshabilitado.',403);this.r.resetDemo();
   }else fail('Acción no encontrada.',404);
   this.r.touch();
  });await this.tick();return result;
 }
}
module.exports={Services,NotificationService,PaymentService,ZONES,TYPES};

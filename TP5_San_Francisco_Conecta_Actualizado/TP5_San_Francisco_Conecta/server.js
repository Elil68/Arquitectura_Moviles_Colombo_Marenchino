'use strict';
const http=require('node:http'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {spawn}=require('node:child_process');
const Database=require('./src/database'),config=require('./src/config')();
const {Repository}=require('./src/repository'),{Auth,fail}=require('./src/security');
const {initialize,demoAccounts}=require('./src/initialize'),{adapters}=require('./src/adapters'),{Services}=require('./src/services');
const db=new Database(config.db);initialize(db,config);
const repository=new Repository(db),auth=new Auth(db,config),services=new Services(repository,adapters(repository),config);
const stats={requests:0,errors:0,durations:[]};let stopping=false,ticking=false;
function json(res,status,value){res.writeHead(status,{'Content-Type':'application/json; charset=utf-8'});res.end(JSON.stringify(value));}
async function body(req){let length=0,chunks=[];for await(const c of req){length+=c.length;if(length>6500000)fail('El mensaje supera el tamaño permitido.',413);chunks.push(c);}try{return JSON.parse(Buffer.concat(chunks).toString('utf8')||'{}');}catch{fail('Datos JSON inválidos.');}}
async function tick(force=false){if(ticking||stopping)return;ticking=true;try{await services.tick(force);}catch(e){console.error(JSON.stringify({level:'error',event:'background-task',message:e.message}));}finally{ticking=false;}}
const timer=setInterval(()=>tick(),1000);timer.unref();
const server=http.createServer(async(req,res)=>{
 const start=performance.now(),requestId=crypto.randomUUID();stats.requests++;
 res.setHeader('X-Request-ID',requestId);res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','no-referrer');
 res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
 if(config.production)res.setHeader('Strict-Transport-Security','max-age=31536000');
 res.once('finish',()=>{const elapsed=performance.now()-start;stats.durations.push(elapsed);if(stats.durations.length>300)stats.durations.shift();if(res.statusCode>=500)stats.errors++;if(config.production)console.log(JSON.stringify({level:'info',requestId,method:req.method,path:req.url.split('?')[0],status:res.statusCode,ms:Math.round(elapsed*100)/100}));});
 try{
  if(stopping)fail('El servicio se está deteniendo.',503);
  const host=req.headers.host||'',expected=config.origin?new URL(config.origin).host:null;
  if(!(config.production?(host===expected||/^(localhost|127\.0\.0\.1)(:\d+)?$/.test(host)):/^(localhost|127\.0\.0\.1)(:\d+)?$/.test(host)))fail('Host no permitido.',403);
  const origin=config.origin||'http://'+host;if(req.headers.origin&&req.headers.origin!==origin)fail('Origen no permitido.',403);
  const url=new URL(req.url,'http://localhost'),route=url.pathname;
  if(req.method==='GET'&&(route==='/healthz'||route==='/api/health'))return json(res,200,{ok:true,app:'san-francisco-conecta'});
  if(req.method==='GET'&&route==='/readyz'){db.get('SELECT 1');return json(res,200,{ready:true});}
  if(req.method==='GET'&&route==='/api/config')return json(res,200,{demo:config.demo,...(config.demo?{accounts:demoAccounts.map(({username,password,name,role})=>({username,password,name,role}))}:{})});
  if(req.method==='POST'&&route==='/api/login'){const b=await body(req);return json(res,200,auth.login(b.username,b.password,req.socket.remoteAddress));}
  const token=(req.headers.authorization||'').replace(/^Bearer /,''),user=auth.session(token);
  if(route.startsWith('/api/')){
   if(!user)fail('Ingresá con tu cuenta para continuar.',401);
   if(route==='/api/logout'&&req.method==='POST'){auth.logout(token);return json(res,200,{ok:true});}
   if(route==='/api/me'&&req.method==='GET')return json(res,200,services.me(user));
   if(route==='/api/changes'&&req.method==='GET')return json(res,200,{revision:db.revision(),unread:repository.unread(user.id)});
   const section=route.replace('/api/','');
   if(req.method==='GET'&&['home','bikes','taxes','claims','contacts','alarms','parking','notices','demo'].includes(section))return json(res,200,await services.view(section,user,url.searchParams));
   if(req.method==='GET'&&route==='/api/claim')return json(res,200,repository.claim(user,url.searchParams.get('id')));
   if(req.method==='GET'&&route==='/api/invoice')return json(res,200,repository.invoice(user.id,url.searchParams.get('id')));
   if(req.method==='GET'&&route==='/api/receipt')return json(res,200,repository.payment(user.id,url.searchParams.get('id')));
   if(req.method==='GET'&&route==='/api/attachment'){const f=repository.attachment(user,url.searchParams.get('id'));res.writeHead(200,{'Content-Type':f.mime,'Content-Disposition':"attachment; filename*=UTF-8''"+encodeURIComponent(f.name)});return res.end(Buffer.from(f.content));}
   if(route==='/api/ops'&&req.method==='GET'){if(user.role!=='demo')fail('Sin permisos.',403);const ordered=stats.durations.slice().sort((a,b)=>a-b);return json(res,200,{requests:stats.requests,errors:stats.errors,p95ms:Math.round((ordered[Math.floor(ordered.length*.95)]||0)*100)/100,uptimeSeconds:Math.round(process.uptime()),database:'SQLite',instances:1});}
   if(route==='/api/users'&&req.method==='POST'){auth.register(user,await body(req));return json(res,201,{ok:true});}
   if(req.method==='POST'){const result=await services.mutate(route,await body(req),user);return json(res,result.pending?202:200,{ok:true,...result});}
   fail('Recurso no encontrado.',404);
  }
  if(!['GET','HEAD'].includes(req.method))fail('Método no permitido.',405);
  const allowed={'/':'index.html','/app.js':'app.js','/style.css':'style.css','/favicon.svg':'favicon.svg'};if(!allowed[route])fail('Archivo no encontrado.',404);
  const f=path.join(__dirname,'public',allowed[route]),types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml'};res.writeHead(200,{'Content-Type':types[path.extname(f)]});res.end(req.method==='HEAD'?undefined:fs.readFileSync(f));
 }catch(e){if(!res.headersSent)json(res,e.status||500,{error:e.status?e.message:'No se pudo procesar la operación.',requestId});else res.end();if(!e.status)console.error(JSON.stringify({level:'error',requestId,message:e.message}));}
});
server.requestTimeout=15000;server.headersTimeout=10000;
let port=config.port;
server.on('error',e=>{if(!config.production&&e.code==='EADDRINUSE'&&port<config.port+20){port++;server.listen(port,config.host);}else{console.error(e.message);process.exitCode=1;}});
server.on('listening',()=>{const url=config.origin||'http://localhost:'+port;console.log('\nSAN FRANCISCO CONECTA\n'+url+'\nDatos: '+config.dir+'\nMantener esta ventana abierta. Para detener: Ctrl+C.\n');if(process.argv.includes('--open')&&process.platform==='win32'){const c=spawn('rundll32.exe',['url.dll,FileProtocolHandler',url],{stdio:'ignore',detached:true,windowsHide:true});c.on('error',()=>{});c.unref();}});
async function shutdown(){if(stopping)return;stopping=true;clearInterval(timer);server.close(()=>{try{db.close();}finally{process.exit(0);}});setTimeout(()=>process.exit(1),10000).unref();}
process.on('SIGTERM',shutdown);process.on('SIGINT',shutdown);
tick(true).then(()=>server.listen(port,config.host));

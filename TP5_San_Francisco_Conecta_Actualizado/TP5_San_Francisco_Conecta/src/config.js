'use strict';
const path=require('node:path');
module.exports=function config(env=process.env){
 const production=env.NODE_ENV==='production',dir=path.resolve(env.DATA_DIR||path.join(__dirname,'../datos'));
 if(production&&!env.PUBLIC_ORIGIN)throw Error('En producción se requiere PUBLIC_ORIGIN con una URL HTTPS.');
 if(production&&!/^https:\/\/[^/]+$/.test(env.PUBLIC_ORIGIN))throw Error('PUBLIC_ORIGIN debe ser un origen HTTPS sin ruta.');
 return {production,dir,db:path.join(dir,'conecta.sqlite'),host:env.HOST||(production?'0.0.0.0':'127.0.0.1'),port:Number(env.PORT)||3000,
  origin:env.PUBLIC_ORIGIN||null,demo:!production,sessionMs:12*3600000,pageSize:10,
  bootstrap:env.ADMIN_USER&&env.ADMIN_PASSWORD?{username:env.ADMIN_USER,password:env.ADMIN_PASSWORD}:null};
};

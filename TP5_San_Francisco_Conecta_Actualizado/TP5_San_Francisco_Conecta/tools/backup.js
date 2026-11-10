'use strict';
const fs=require('node:fs'),path=require('node:path');
const {DatabaseSync}=require('node:sqlite');
const config=require('../src/config')(),dir=path.join(config.dir,'backups');fs.mkdirSync(dir,{recursive:true});
if(!fs.existsSync(config.db))throw Error('No existe una base de datos inicializada para respaldar.');
const db=new DatabaseSync(config.db),file=path.join(dir,'conecta-'+Date.now()+'.sqlite');
db.exec("VACUUM INTO '"+file.replace(/'/g,"''")+"'");db.close();console.log('Copia consistente creada: '+file);

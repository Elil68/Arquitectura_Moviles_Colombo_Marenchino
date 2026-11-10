'use strict';
module.exports = function seed() {
  const now=Date.now(), day=86400000;
  return {
    version:1, updatedAt:new Date(now).toISOString(),
    profile:{name:'Matías Marenchino',account:'VEC-001',address:'Sector Centro · domicilio de ejemplo',position:{x:46,y:54}},
    zones:['Centro','Norte','Sur','Este'],
    bikes:[
      {id:'BIC-01',name:'Estación Centro',zone:'Centro',x:40,y:45,capacity:12,bikes:8},
      {id:'BIC-02',name:'Estación Norte',zone:'Norte',x:63,y:20,capacity:10,bikes:3},
      {id:'BIC-03',name:'Estación Sur',zone:'Sur',x:36,y:78,capacity:14,bikes:0},
      {id:'BIC-04',name:'Estación Este',zone:'Este',x:81,y:51,capacity:8,bikes:5}
    ],
    invoices:[
      {id:'CED-001',concept:'Tasa de servicios municipales',period:'Cuota de demostración 01',amount:8500,dueAt:new Date(now+2*day).toISOString(),status:'pendiente'},
      {id:'CED-002',concept:'Contribución sobre inmuebles',period:'Cuota de demostración 02',amount:12400,dueAt:new Date(now-4*day).toISOString(),status:'pendiente'},
      {id:'CED-003',concept:'Tasa de servicios municipales',period:'Cuota de demostración 00',amount:7900,dueAt:new Date(now-30*day).toISOString(),status:'pagada',paidAt:new Date(now-32*day).toISOString(),receipt:'PAG-DEMO-001'}
    ],
    payments:[{id:'PAG-DEMO-001',type:'tributaria',ref:'CED-003',concept:'Tasa de servicios municipales',amount:7900,at:new Date(now-32*day).toISOString()}],
    claims:[], bikeReports:[],
    contacts:[
      {id:'CON-01',name:'Atención ciudadana',description:'Consultas generales y orientación.',phone:'03564000101',email:'atencion@example.invalid',hours:'Lunes a viernes · 08:00 a 18:00'},
      {id:'CON-02',name:'Servicios públicos',description:'Alumbrado, limpieza y mantenimiento.',phone:'03564000102',email:'servicios@example.invalid',hours:'Lunes a viernes · 07:00 a 14:00'},
      {id:'CON-03',name:'Dirección General Tributaria',description:'Cedulones y consultas tributarias.',phone:'03564000103',email:'tributaria@example.invalid',hours:'Lunes a viernes · 07:00 a 13:00'},
      {id:'CON-04',name:'Movilidad urbana',description:'Bicicletas públicas y estacionamiento.',phone:'03564000104',email:'movilidad@example.invalid',hours:'Lunes a viernes · 08:00 a 16:00'}
    ],
    alarms:[
      {id:'ALA-01',name:'Alarma Centro',zone:'Centro',x:46,y:38,active:false},
      {id:'ALA-02',name:'Alarma Norte',zone:'Norte',x:66,y:22,active:false},
      {id:'ALA-03',name:'Alarma Sur',zone:'Sur',x:31,y:80,active:false},
      {id:'ALA-04',name:'Alarma Este',zone:'Este',x:80,y:62,active:false}
    ],
    parkingZones:[
      {id:'ZON-01',name:'Zona Centro',zone:'Centro',x:53,y:47,rate:900,hours:'Lunes a viernes · 08:00 a 20:00'},
      {id:'ZON-02',name:'Zona Norte',zone:'Norte',x:67,y:30,rate:600,hours:'Lunes a viernes · 08:00 a 18:00'},
      {id:'ZON-03',name:'Zona Sur',zone:'Sur',x:43,y:72,rate:500,hours:'Lunes a viernes · 08:00 a 18:00'}
    ],
    parking:[],
    preferences:{types:['agua','obras','eventos','alarmas','tributaria','estacionamiento','reclamos','bicicletas'],zones:['Centro','Norte','Sur','Este'],frequency:'inmediata'},
    notices:[],
    events:[{id:'EVT-001',type:'eventos',zone:'Centro',title:'Una ciudad que conecta',body:'Consultá los servicios municipales desde un mismo lugar. Todos los datos de esta demostración son ficticios.',target:'avisos',at:new Date(now).toISOString()}],
    activity:[], reminders:[]
  };
};

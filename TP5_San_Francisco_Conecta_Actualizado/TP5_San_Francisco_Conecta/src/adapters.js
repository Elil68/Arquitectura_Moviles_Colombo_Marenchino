'use strict';
// Contratos asincronos: los proveedores reales implementan estos mismos metodos.
// Los adaptadores reciben solo los datos de su contrato, no objetos HTTP ni vistas.
class SimulatedPaymentGateway{
 async charge({amountCents,currency,reference,idempotencyKey}){
  if(!Number.isInteger(amountCents)||amountCents<=0||currency!=='ARS'||!reference||!idempotencyKey)throw Error('Solicitud de pago inválida.');
  return {status:'approved',providerRef:'SIM-'+idempotencyKey};
 }
}
class FixedLocationProvider{async locate(){return {x:46,y:54,simulated:true};}}
class LocalBicycleProvider{constructor(repository){this.repository=repository;}async list(){return this.repository.bikes();}}
class LocalAlarmProvider{constructor(repository){this.repository=repository;}async list(){return this.repository.alarms();}}
class LocalTaxProvider{
 constructor(repository){this.repository=repository;}
 async list({userId,page}){return this.repository.invoices(userId,page);}
 async invoice({userId,id}){return this.repository.invoice(userId,id);}
}
class LocalDirectoryProvider{constructor(repository){this.repository=repository;}async list(){return this.repository.contacts();}}
class InAppNotificationProvider{
 async deliver({id,userId,title,body}){if(!id||!userId||!title||!body)throw Error('Aviso inválido.');return {status:'delivered',channel:'in-app',providerRef:'LOCAL-'+id};}
}
function adapters(repository,overrides={}){return {payments:new SimulatedPaymentGateway(),location:new FixedLocationProvider(),bicycles:new LocalBicycleProvider(repository),alarms:new LocalAlarmProvider(repository),taxes:new LocalTaxProvider(repository),directory:new LocalDirectoryProvider(repository),notifications:new InAppNotificationProvider(),...overrides};}
module.exports={adapters,SimulatedPaymentGateway,FixedLocationProvider,LocalBicycleProvider,LocalAlarmProvider,LocalTaxProvider,LocalDirectoryProvider,InAppNotificationProvider};

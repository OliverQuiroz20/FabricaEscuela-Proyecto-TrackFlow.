-- La dirección del destinatario existe desde que se crea el envío
-- (shipments.recipient_address), pero nunca salía de ahí: EnvioCreadoEvent no la
-- llevaba, así que ni logistics ni reports la conocían. Se necesita para mostrarla
-- cuando el envío queda ENTREGADO — antes de eso no tiene sentido mostrarla, porque
-- el paquete todavía no llegó ahí.
alter table reports_shipment_tracking add column recipient_address varchar(255);

update reports_shipment_tracking r
set recipient_address = s.recipient_address
from shipments s
where s.tracking_number = r.tracking_number;

-- Mismo criterio que V5: una fila huérfana (sin envío) se descarta en vez de
-- bloquear la migración; se reconstruye con POST /api/admin/reconstruir-proyecciones.
delete from reports_shipment_tracking where recipient_address is null;

alter table reports_shipment_tracking alter column recipient_address set not null;

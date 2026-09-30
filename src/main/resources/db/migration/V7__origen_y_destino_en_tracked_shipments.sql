-- logistics no puede importar clases de shipments para saber la ciudad de origen y
-- destino de un envío (regla dura del monolito modular): tiene que enterarse por el
-- mismo mecanismo con el que ya sabe qué envíos existen, TrackedShipment, alimentado
-- por EnvioCreadoEvent. Ese evento ya trae originCityId/destinationCityId desde un
-- cambio anterior; solo faltaba que logistics los guardara.
--
-- Se necesitan para validar que un ARRIVED_AT_DESTINATION_CENTER ocurra en un centro
-- de la ciudad de destino, y que el primer RECEIVED_AT_CENTER ocurra en uno de la
-- ciudad de origen.

alter table logistics_tracked_shipments add column origin_city_id bigint;
alter table logistics_tracked_shipments add column destination_city_id bigint;

-- Igual que V5 con reports_shipment_tracking: se respalda uniendo con shipments en
-- vez de esperar a que alguien ejecute POST /api/admin/reconstruir-proyecciones.
-- Ese endpoint sigue siendo el camino de recuperación si una fila queda huérfana.
update logistics_tracked_shipments t
set origin_city_id = s.sender_city_id,
    destination_city_id = s.recipient_city_id
from shipments s
where s.tracking_number = t.tracking_number;

-- Un tracked_shipment sin envío detrás ya sería una inconsistencia hoy (se alimentan
-- del mismo evento); si aparece, se descarta en lugar de bloquear la migración.
delete from logistics_tracked_shipments where origin_city_id is null;

alter table logistics_tracked_shipments alter column origin_city_id set not null;
alter table logistics_tracked_shipments alter column destination_city_id set not null;

alter table logistics_tracked_shipments add constraint fk_tracked_shipments_origin_city
    foreign key (origin_city_id) references cities (id);
alter table logistics_tracked_shipments add constraint fk_tracked_shipments_destination_city
    foreign key (destination_city_id) references cities (id);

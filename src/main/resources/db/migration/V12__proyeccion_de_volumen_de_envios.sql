-- Reporte de volumen de envíos: cuántos envíos se registraron en un periodo y por
-- qué punto de la red entraron. Hoy esa cifra se arma a mano cruzando fuentes; aquí
-- se mantiene como un modelo de lectura propio de reports, con una fila por envío.
--
-- Por qué una tabla nueva y no más columnas en reports_shipment_tracking: aquella es
-- lo que ve el cliente al consultar su envío, esta es lo que ve el supervisor al
-- dimensionar la operación. Cambian por razones distintas y se consultan distinto
-- (una por tracking_number, la otra agregando por rango de fechas).
--
-- El punto de ingreso es el centro del PRIMER "Recibido en centro": ahí el envío
-- entra a la red y empieza a pesar en la operación. Mientras no se haya recibido en
-- ningún centro, entry_* queda vacío y el reporte lo agrupa como pendiente de
-- recepción en su ciudad de origen; así el desglose siempre suma el total.
--
-- entry_point y entry_city se congelan con el valor del momento, igual que en
-- logistics_events (V8): si un centro se renombra, el histórico no cambia.

create table reports_shipment_volume (
    tracking_number varchar(255) not null,
    registered_at timestamp(6) with time zone not null,
    origin_city_id bigint not null,
    origin_city varchar(255) not null,
    entry_center_id bigint,
    entry_point varchar(255),
    entry_city varchar(255),
    received_at timestamp(6) with time zone,
    delivered_at timestamp(6) with time zone,
    primary key (tracking_number),
    constraint fk_reports_volume_origin_city foreign key (origin_city_id) references cities (id)
);

-- El reporte filtra por rango de fechas de registro y de entrega.
create index idx_reports_volume_registered_at on reports_shipment_volume (registered_at);
create index idx_reports_volume_delivered_at on reports_shipment_volume (delivered_at);

-- Respaldo de los envíos existentes, como hicieron V5, V7 y V11: se une con la fuente
-- de verdad en vez de esperar a que alguien ejecute la reconstrucción. Ese endpoint
-- (POST /api/admin/reconstruir-proyecciones) sigue siendo el camino de recuperación.
insert into reports_shipment_volume (tracking_number, registered_at, origin_city_id, origin_city)
select s.tracking_number, s.registered_at, c.id, c.name || ' - ' || c.department
from shipments s
    join cities c on c.id = s.sender_city_id;

update reports_shipment_volume v
set entry_center_id = e.center_id,
    entry_point = e.point,
    entry_city = e.city_name,
    received_at = e.occurred_at
from (
    select distinct on (tracking_number) tracking_number, center_id, point, city_name, occurred_at
    from logistics_events
    where type = 'RECEIVED_AT_CENTER'
    order by tracking_number, occurred_at asc
) e
where e.tracking_number = v.tracking_number;

update reports_shipment_volume v
set delivered_at = e.occurred_at
from (
    select tracking_number, min(occurred_at) as occurred_at
    from logistics_events
    where type = 'DELIVERED'
    group by tracking_number
) e
where e.tracking_number = v.tracking_number;

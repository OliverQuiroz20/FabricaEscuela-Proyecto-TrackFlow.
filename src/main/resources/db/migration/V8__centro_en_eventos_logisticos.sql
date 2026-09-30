-- El punto del evento logístico deja de ser solo texto libre: cuando el operador
-- indica un centro del catálogo, se guarda su identificador además del nombre ya
-- resuelto en ese instante. La columna "point" existente sigue cumpliendo el mismo
-- papel de siempre (el nombre que ve el cliente), solo que ahora puede venir de un
-- centro en lugar de haberse escrito a mano.
--
-- center_id y city_name son nullable a propósito: un evento histórico registrado
-- antes de este cambio (o uno nuevo que use el texto libre de respaldo) no tiene un
-- centro asociado, y no hay que inventarle uno. Tampoco se resuelve center_id como
-- una referencia viva: si mañana se renombra o desactiva un centro, los eventos ya
-- guardados no deben cambiar, así que aquí no se recalcula nada; point y city_name
-- quedan congelados con el valor del momento en que se registró el evento.

alter table logistics_events add column center_id bigint;
alter table logistics_events add column city_name varchar(255);

alter table logistics_events add constraint fk_logistics_events_center
    foreign key (center_id) references logistics_centers (id);

create index idx_logistics_events_center on logistics_events (center_id);

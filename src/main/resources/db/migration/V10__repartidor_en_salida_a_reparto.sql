-- Hasta ahora "salió a reparto" no dejaba constancia de quién llevaba el paquete
-- en la calle. Nullable porque solo aplica a OUT_FOR_DELIVERY: el resto de eventos
-- no tiene repartidor y esta columna queda vacía para ellos, igual que center_id
-- queda vacío para los que usaron el respaldo de texto libre (V8).
alter table logistics_events add column deliverer_name varchar(160);

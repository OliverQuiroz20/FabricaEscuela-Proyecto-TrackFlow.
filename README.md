# TrackFlow — Sistema de tracking logístico

OnRender: https://trackflow-mxll.onrender.com/swagger-ui/index.html

## Reporte de volumen de envíos

`GET /api/reportes/volumen-envios?desde=AAAA-MM-DD&hasta=AAAA-MM-DD`

Para el supervisor de operaciones: cuántos envíos se registraron en un periodo y por qué
punto de la red entraron. Requiere token de operador cuando la protección está activa.

- `desde` y `hasta` son opcionales y se interpretan como días en hora de Colombia, ambos
  inclusive. Sin ellos, el periodo es el día en curso. Si el periodo incluye hoy, se
  cuenta hasta el momento de la consulta (`enCurso = true`).
- `totalEnvios` son los envíos **registrados** en el periodo. `porPunto` los desglosa por
  el centro donde ingresaron a la red (el primer "Recibido en centro") y siempre suma el
  total. Los que aún no se reciben en ningún centro aparecen como pendientes de recepción
  en su ciudad de origen.
- `totalEntregados` es una cifra aparte: los envíos entregados en el periodo, aunque se
  hayan registrado antes.
- Un periodo sin envíos responde 200 con total 0 y un `mensaje`. Un periodo invertido, que
  empieza en el futuro o con una fecha mal escrita responde 400.

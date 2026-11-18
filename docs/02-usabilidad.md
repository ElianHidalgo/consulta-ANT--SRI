# Prueba de usabilidad

Prueba moderada con un participante que no conocía el sistema. Se definieron cinco tareas y en cada una se registró el éxito, el tiempo y los errores, junto con las dudas expresadas en voz alta.

## 1. Preparación

- Sistema levantado en modo real con `iniciar.bat` (http://localhost:5173). Para las tareas de la ANT se usó el simulador, como se explica en la sección siguiente.
- Datos reales usados con autorización de su titular: correo, RUC de persona natural, cédula y placa.
- Antes de iniciar se pulsó **Nueva consulta** para reiniciar el registro automático de tiempos.
- Se usó cronómetro y grabación de pantalla con consentimiento del participante.
- Consigna inicial: *"Estamos probando el sistema, no a ti. Piensa en voz alta y puedes parar cuando quieras."*

## Disponibilidad de la ANT y uso del simulador

Durante el desarrollo y la prueba (16 y 17/11/2026) la web de la ANT (`consultaweb.ant.gob.ec`) no estuvo disponible:

- Los registros del Servicio ANT muestran que cada consulta recibió **502 Bad Gateway**, incluso después de los dos intentos configurados. Las consultas directas a la misma URL devolvieron **401** o **502**.
- En la interfaz, la consulta de puntos con datos reales mostró el mensaje de servicio no disponible (`SERVICIO_EXTERNO_NO_DISPONIBLE`) para las cédulas probadas ([evidencia](evidencias/ANT-real-no-disponible.png)).
- Como la ANT nunca devolvió una respuesta válida, no existía ningún dato real en la caché que el sistema pudiera mostrar como respaldo.

Este comportamiento es el esperado: el sistema no falló, informó la caída con un error estructurado y un mensaje claro. Sin embargo, impedía evaluar la visualización de los puntos y el aviso de dato no reciente. Por eso, para las tareas de la ANT se usó el simulador del proyecto (`iniciar-simulador.bat`):

- El simulador **solo reemplaza a la ANT**. El RUC, el correo y la placa usados se siguieron consultando en el **SRI real**.
- La ANT simulada devuelve un valor fijo de **28 de 30 puntos** con la misma estructura HTML que procesa el Servicio ANT, de modo que el flujo interno (consulta, extracción de puntos, guardado en caché y respuesta) es el mismo que con la ANT real. Ese valor no corresponde a los puntos reales del titular.
- Los datos simulados se guardan en la caché con el prefijo `sim:` y no se mezclan con los datos reales.
- El simulador permite apagar la ANT a voluntad (`/__control?ant=down`) y así reproducir la caída con un dato previo guardado.

| Parte evaluada | Fuente de datos |
|---|---|
| T1 Contribuyente, T3 Vehículo, T5 Validación | SRI real |
| T2 Puntos, T4 Aviso de dato no reciente | ANT simulada (la ANT real no estaba disponible) |
| Mensaje de servicio no disponible | ANT real |

## 2. Tareas

El moderador lee solo el enunciado, no explica la pantalla y no ofrece ayuda salvo que el participante la pida.

| # | Enunciado | Criterio de éxito |
|---|---|---|
| T1 | "Averigua si este RUC está registrado como contribuyente. Usa este correo y este RUC." | Ve la tarjeta del contribuyente con el nombre correcto |
| T2 | "¿Cuántos puntos tiene la licencia de esta cédula?" | Encuentra la pestaña ANT sin ayuda y dice los puntos o explica que la ANT no está disponible |
| T3 | "Consulta los datos del vehículo con esta matrícula." | Ve la tarjeta del vehículo (marca y modelo) |
| T4 | "Mira el resultado de los puntos. ¿Qué te dice la pantalla sobre ese dato y qué harías?" | Con aviso amarillo: entiende que el dato no es reciente y su fecha. Con error: entiende que la ANT no respondió y que debe intentarlo más tarde |
| T5 | "Ingresa este RUC: 12345. ¿Qué harías?" | Entiende el mensaje de error y lo corrige |

La web de la ANT tiene baja disponibilidad. Si respondió alguna vez para la cédula consultada, el sistema muestra el último dato guardado con el aviso de "Dato no reciente"; si nunca respondió, muestra un mensaje de servicio no disponible. En T2 y T4 se registra cuál de los dos casos ocurrió.

**Criterios de medición**
- *Éxito*: Sí (sin ayuda), Con ayuda (necesitó una pista), No (abandonó).
- *Tiempo*: desde que termina la lectura del enunciado hasta que se cumple el criterio. Se contrasta con el CSV que genera la interfaz (**Descargar registro de la prueba (CSV)**).
- *Errores*: clics en lugares equivocados, mensajes de validación, campos mal llenados o solicitudes de ayuda.

## 3. Registro

| Dato | Valor |
|---|---|
| Participante | |
| Edad / perfil | |
| Fecha | |
| Moderador | |

| Tarea | Éxito | Tiempo (s) | Errores | Observaciones |
|---|---|---|---|---|
| T1 | | | | |
| T2 | | | | |
| T3 | | | | |
| T4 | | | | |
| T5 | | | | |

**Preguntas finales**

| Pregunta | Respuesta |
|---|---|
| ¿Qué fue lo más confuso? | |
| ¿Qué fue lo más fácil? | |
| Del 1 (muy difícil) al 5 (muy fácil), ¿qué tan fácil fue usarlo? | |
| ¿Entendió el aviso de dato no reciente o el mensaje de servicio no disponible? | |

## 4. Resumen de resultados

| Indicador | Valor |
|---|---|
| Tareas completadas sin ayuda | / 5 |
| Tiempo total (s) | |
| Tiempo promedio por tarea (s) | |
| Total de errores | |

**Hallazgos principales**

1. 
2. 
3. 

## 5. Mejoras propuestas

Cada mejora parte de una observación concreta e indica cómo se verificará en una nueva prueba.

| # | Observación | Mejora | Prioridad | Verificación |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |

## 6. Evidencias

Archivos en [`docs/evidencias/`](evidencias/):

| Archivo | Contenido | Fuente |
|---|---|---|
| [`T1-contribuyente-sri-real.png`](evidencias/T1-contribuyente-sri-real.png) | Contribuyente persona natural, activo | SRI real |
| [`T2-puntos-ant-simulada.png`](evidencias/T2-puntos-ant-simulada.png) | Puntos de la licencia (28 de 30) | ANT simulada |
| [`T3-vehiculo-sri-real.png`](evidencias/T3-vehiculo-sri-real.png) | Datos del vehículo GSG4244 | SRI real |
| [`T5-validacion-ruc-correo.png`](evidencias/T5-validacion-ruc-correo.png) | Mensajes de validación de RUC incompleto y correo vacío | Interfaz |
| [`ANT-real-no-disponible.png`](evidencias/ANT-real-no-disponible.png) | Mensaje de servicio no disponible con la ANT real | ANT real |

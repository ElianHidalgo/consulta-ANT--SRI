# Prueba de usabilidad (1 persona, tareas definidas)

Objetivo de la rúbrica: hacer la prueba con **una persona**, con **tareas definidas**, registrar **éxito, tiempo y errores**, y proponer **mejoras concretas**. Esta plantilla está lista para llenar; **los resultados los debes registrar tú con la prueba real** (no inventes datos: la rúbrica pide evidencia de lo observado).

## 1. Preparación (15 min)

1. Elige a **una persona que no haya visto el sistema** (compañero, familiar). Mejor si tiene un perfil parecido al usuario real (conduce, tiene vehículo).
2. Levanta el sistema con el simulador (así no usas datos personales reales y no dependes de la ANT, que suele estar caída): doble clic en **`iniciar.bat`** y espera a que se abra http://localhost:5173.
   - Datos de prueba: RUC `1713175071001` y matrícula `PCQ1234` (pestaña SRI); cédula `1713175071` (pestaña ANT); correo cualquiera con formato válido.
   - Para la tarea 4 necesitarás apagar la ANT: abre en otra pestaña http://localhost:9000/__control?ant=down (y http://localhost:9000/__control?ant=up para encenderla). Hazlo sin que la persona lo vea.
3. Abre la interfaz en el navegador, en una ventana limpia, y pulsa **"Nueva consulta"** (reinicia el registro automático de tiempos).
4. Prepara cronómetro (o graba pantalla con permiso de la persona) y esta hoja.
5. Pide consentimiento verbal: *"Estamos probando el sistema, no a ti. Puedes parar cuando quieras."*

## 2. Guion para el moderador

Lee solo el enunciado de la tarea. **No ayudes** ni expliques la pantalla. Pide que piense en voz alta. Anota todo lo que haga.

| # | Tarea (lo que lees) | Criterio de éxito |
|---|---|---|
| T1 | "Averigua si tu RUC está registrado como contribuyente. Usa este RUC y este correo." | Ve la tarjeta del contribuyente con su nombre |
| T2 | "¿Cuántos puntos tiene la licencia de la cédula 1713175071?" | Encuentra la pestaña ANT y dice 28 puntos sin ayuda |
| T3 | "Consulta los datos del vehículo con esta matrícula." | Ve la tarjeta del vehículo (marca/modelo) |
| T4 | (con la ANT apagada) "Vuelve a consultar los puntos de la misma cédula. ¿Qué te dice la pantalla sobre ese dato?" | Entiende que el dato no es reciente y cuál es su fecha |
| T5 | "Ingresa un RUC mal escrito (ej. 12345) y dime qué harías." | Entiende el mensaje de error y lo corrige |

Para T4: apaga la ANT y pide que repita la consulta en la pestaña **ANT** con la misma cédula **después** de haber consultado antes con la ANT encendida (si no, no habrá dato guardado y verás error 503, que también es un resultado válido para anotar). `iniciar.bat` ya arranca con `ANT_CACHE_FRESH=0s`, así que cada consulta intenta primero la ANT y se ve el respaldo de la caché.

## 3. Hoja de registro (llenar durante la prueba)

Participante: ______ (edad/perfil): ______ Fecha: ______ Moderador: ______

| Tarea | ¿Éxito? (Sí / Con ayuda / No) | Tiempo (s) | N.º de errores | Errores / dudas observadas (literal) |
|---|---|---|---|---|
| T1 | | | | |
| T2 | | | | |
| T3 | | | | |
| T4 | | | | |
| T5 | | | | |

**Cómo medir**
- *Tiempo*: desde que termina de leer la tarea hasta que logra el criterio. El CSV de la interfaz (botón **"Descargar registro de la prueba (CSV)"**) trae las marcas de tiempo de envíos y resultados; úsalo para contrastar con tu cronómetro.
- *Errores*: clics en lugar equivocado, mensajes de validación, campos mal llenados, pedir ayuda.
- *Éxito*: Sí (sin ayuda), Con ayuda (necesitó pista), No (abandonó).

**Preguntas finales (2 min)**
1. ¿Qué fue lo más confuso? ______
2. ¿Qué fue lo más fácil? ______
3. Del 1 (muy difícil) al 5 (muy fácil), ¿qué tan fácil fue usarlo? ______
4. ¿Entendió el aviso amarillo de "dato no reciente"? ______

## 4. Resumen de resultados (llenar al terminar)

- Tareas completadas sin ayuda: __ de 5
- Tiempo total: __ s | Tiempo promedio por tarea: __ s
- Total de errores: __
- Hallazgos principales (2–4): ______

## 5. Mejoras propuestas (deben ser concretas y derivadas de lo observado)

Cada mejora debe responder: **qué se vio → qué se cambia → cómo se medirá que mejoró.**

| # | Observación (evidencia) | Mejora concreta | Prioridad | Cómo verificar |
|---|---|---|---|---|
| 1 | Ej.: "el usuario no vio el campo de placa tras consultar el RUC" | Hacer scroll automático / resaltar la sección 2 al mostrar el contribuyente | Alta | T3 en menos de X s |
| 2 | | | | |
| 3 | | | | |

Ejemplos del **estilo** esperado (reemplázalos por lo que realmente observes):
- Si dudó en qué escribir en "Matrícula": agregar texto de ayuda con los formatos aceptados (placa, CAMV o CPN).
- Si no entendió el aviso de dato no reciente: cambiar el texto a "Este dato es del <fecha>. La ANT no está respondiendo ahora".
- Si tardó en notar el error: mover el mensaje de error junto al campo y enfocar el campo con error.

## 6. Qué entregar

Guarda las capturas y el CSV en `docs/evidencias/` (por ejemplo `usabilidad-registro.csv`, `T4-aviso-dato-no-reciente.png`) y enlázalos aquí.

Esta hoja llenada + (opcional) el CSV descargado + 1–2 capturas o un video corto + la tabla de mejoras.

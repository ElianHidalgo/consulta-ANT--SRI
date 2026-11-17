# Consulta de contribuyente, vehículo y licencia (SRI + ANT)

Sistema web que, a partir de un correo y un RUC de persona natural, verifica al contribuyente en el SRI, consulta su vehículo por matrícula y obtiene los puntos de su licencia en la ANT. Como la web de la ANT tiene baja disponibilidad, **cada respuesta buena se guarda en una caché en la nube (Upstash Redis)** y se devuelve desde allí cuando el servicio externo falla, avisando que el dato no es reciente.

| Entregable | Enlace |
|---|---|
| Modelo C4 en IcePanel (solo lectura) | **PEGAR_AQUI_EL_ENLACE_DE_ICEPANEL** |
| Repositorio | https://github.com/ElianHidalgo/consulta-ANT--SRI |
| Prueba de usabilidad | [`docs/02-usabilidad.md`](docs/02-usabilidad.md) |

## Arquitectura

```
Usuario ─▶ Frontend (React + Nginx) ─┬─▶ sri-service (Java 21, Spring Boot, :8081) ─┬─▶ SRI (REST JSON)
                                     │                                              └─▶ Redis en la nube
                                     └─▶ ant-service (Java 21, Spring Boot, :8082) ─┬─▶ ANT (HTML)
                                                                                    └─▶ Redis en la nube
```

| Carpeta | Contenedor C4 | Tecnología |
|---|---|---|
| `frontend/` | Frontend web | React 18 + Vite + TypeScript, servido por Nginx (también enruta `/api`) |
| `backend/sri-service/` | Servicio SRI | Java 21, Spring Boot 3.3, RestClient |
| `backend/ant-service/` | Servicio ANT | Java 21, Spring Boot 3.3, Jsoup |
| (externo) | Caché en la nube | Upstash Redis (TLS) |
| `tools/mock-upstream/` | — | Simulador local del SRI y la ANT (solo desarrollo) |

El modelo C4 y cómo crearlo en IcePanel están en [`docs/01-icepanel.md`](docs/01-icepanel.md).

## Flujo de uso

La interfaz tiene **dos módulos independientes** (pestañas) en el mismo frontend; cada uno llama a su microservicio y se puede usar sin pasar por el otro:

**Módulo SRI** (`sri-service`)
1. El usuario ingresa **correo y RUC** → `GET /api/v1/contribuyentes/{ruc}?correo=` verifica que sea contribuyente (`existePorNumeroRuc`), que sea **persona natural** y devuelve sus datos (`obtenerPorNumerosRuc`).
2. A continuación se pide la **matrícula** → `GET /api/v1/vehiculos/{placa}`.

**Módulo ANT** (`ant-service`)
1. El usuario ingresa su **cédula** (y opcionalmente la placa) → `GET /api/v1/licencias/{cedula}/puntos?placa=` devuelve los puntos de la licencia, desde la ANT o desde la caché si la ANT no responde.

Al cambiar de pestaña no se pierde lo consultado; **"Nueva consulta"** limpia ambos módulos.

## Contrato de la API

Éxito (200):

```json
{
  "datos": { "...": "..." },
  "meta": {
    "origen": "SERVICIO_EXTERNO | CACHE",
    "desactualizado": false,
    "consultadoEn": "2026-11-15T18:00:00Z",
    "advertencia": "solo si desactualizado = true"
  }
}
```

Error (siempre con esta forma):

```json
{ "error": { "codigo": "RUC_INVALIDO", "mensaje": "…", "detalles": [], "timestamp": "…", "ruta": "/api/v1/contribuyentes/123" } }
```

| Endpoint | Verbo | Códigos |
|---|---|---|
| `/api/v1/contribuyentes/{ruc}?correo=` | GET | 200, 400 `RUC_INVALIDO` / `CORREO_INVALIDO` / `PARAMETRO_REQUERIDO`, 404 `NO_CONTRIBUYENTE`, 422 `NO_PERSONA_NATURAL`, 503 `SERVICIO_EXTERNO_NO_DISPONIBLE` |
| `/api/v1/vehiculos/{placa}` | GET | 200, 400 `PLACA_INVALIDA`, 404 `VEHICULO_NO_ENCONTRADO`, 503 |
| `/api/v1/licencias/{cedula}/puntos?placa=` | GET | 200, 400 `CEDULA_INVALIDA` / `PLACA_INVALIDA`, 503 |

Otros: 405 `METODO_NO_PERMITIDO`, 404 `RUTA_NO_ENCONTRADA`, 500 `ERROR_INTERNO`. El 503 incluye `Retry-After: 60`. Un dato servido desde la caché por falla del servicio externo incluye la cabecera `Warning: 110 - "Response is Stale"`.

Documentación interactiva (Swagger UI): `http://localhost:8081/swagger-ui.html` y `http://localhost:8082/swagger-ui.html`.

## Caché y resiliencia

Patrón **"última respuesta buena"** (`ResilientFetcher`):

1. Si hay un dato guardado con menos de `fresh` de antigüedad, se sirve de la caché (protege a la ANT de consultas repetidas).
2. Si no, se consulta al servicio externo (timeout de conexión 3 s, lectura 8–10 s, 2 intentos). Si la respuesta es adecuada, **se guarda** con su fecha.
3. Si el servicio externo falla (timeout, 5xx, HTML sin los puntos, valor fuera de rango) y hay dato guardado → se devuelve ese dato con `desactualizado: true`, `consultadoEn` (fecha real del dato) y una `advertencia`; el frontend muestra un aviso amarillo.
4. Si falla y no hay dato guardado → 503 estructurado.

Solo las respuestas adecuadas llegan a la caché: los errores de negocio (404, 422) y las páginas inválidas de la ANT no se guardan. Si Redis falla, el sistema sigue funcionando sin caché (no propaga el error).

**Tiempos de vida (TTL) y su justificación** — configurables en `application.yml`:

| Dato | TTL máximo | `fresh` | Justificación |
|---|---|---|---|
| Puntos de licencia (ANT) | 7 días | 5 min | Los puntos cambian solo cuando se procesa una citación (días). La ANT cae con frecuencia, así que se conserva una semana; más allá, el dato es demasiado viejo para ser útil. Se muestra siempre la fecha de consulta. |
| Contribuyente (SRI) | 30 días | 2 min | Datos del catastro que cambian muy rara vez (actualización de RUC). |
| Vehículo (SRI) | 7 días | 2 min | Cambia con traspasos o pago de matrícula; una semana limita el riesgo de mostrar un propietario/estado viejo. |

## Cómo ejecutarlo

Requisitos: **JDK 21**, Maven 3.9+, Node 20+ y un archivo `.env` con `REDIS_URL` de Upstash (copia `.env.example`). Guía completa en [`docs/03-despliegue.md`](docs/03-despliegue.md).

### Inicio rápido en Windows (doble clic)

| Archivo | Qué hace |
|---|---|
| `iniciar.bat` | Levanta todo con el **simulador** del SRI/ANT (recomendado para la demo y la prueba de usabilidad) |
| `iniciar-real.bat` | Levanta todo contra el **SRI y la ANT reales** |
| `detener.bat` | Detiene todos los servicios |

Abre http://localhost:5173. Los logs quedan en `logs/`. El script busca un JDK 21 instalado aunque `JAVA_HOME` apunte a otra versión. En Linux/macOS: `./dev.sh [--mock]` y `./dev.sh --stop`.

### Con Docker

```bash
cp .env.example .env                 # pegar REDIS_URL de Upstash (rediss://default:PASSWORD@HOST:6379)
docker compose up --build            # servicios reales, web en http://localhost:8080
docker compose -f docker-compose.yml -f docker-compose.mock.yml up --build   # con el simulador
```

### Demostrar la caché (con el simulador levantado)

```bash
curl -i localhost:8082/api/v1/licencias/1713175071/puntos   # origen SERVICIO_EXTERNO
curl "localhost:9000/__control?ant=down"                    # apaga la ANT simulada
curl -i localhost:8082/api/v1/licencias/1713175071/puntos   # 200, origen CACHE, desactualizado=true, Warning: 110
curl -i localhost:8082/api/v1/licencias/1710034065/puntos   # cédula nunca consultada: 503 + Retry-After
curl "localhost:9000/__control?ant=up"                      # vuelve a encender la ANT
```

Tests del backend: `cd backend && mvn test` (39 pruebas). Build del frontend: `cd frontend && npm run build`.

## Estado frente a los servicios reales y pendientes

- **SRI**: la consulta de contribuyente se probó contra el servicio real y responde correctamente. Falta confirmar con una placa real los nombres de campos del vehículo (ver abajo).
- **ANT**: durante las pruebas (16 y 17/11/2026) la web real respondió siempre **502 Bad Gateway**, lo que confirma su baja disponibilidad. El sistema responde 503 estructurado (sin dato previo) o el último dato bueno de la caché.
- **Parser de la ANT** (`AntParser`, propiedad `ant.puntos-regex`): la ANT no tiene API, se lee HTML. Abre la URL real con una cédula en el navegador, localiza dónde aparece el texto de los puntos y ajusta la expresión regular.
- **Campos del vehículo** (`SriMapper.vehiculo`): se mapean varios nombres posibles; revisa el JSON real y ajusta si hace falta. Todos los campos igual se muestran desde `detalle`.
- **Certificado de la ANT**: si el servicio falla con error SSL (`PKIX path building failed`), el sitio de la ANT suele tener la cadena de certificados incompleta; importa el certificado intermedio en el truststore de Java.

## Prueba de usabilidad

Plantilla y procedimiento en [`docs/02-usabilidad.md`](docs/02-usabilidad.md). La interfaz incluye el botón **"Descargar registro de la prueba (CSV)"** que anota tiempos, éxitos y errores.

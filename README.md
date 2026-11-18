# Consulta de contribuyente, vehículo y licencia (SRI + ANT)

Sistema web que verifica a un contribuyente persona natural en el SRI, consulta la información de su vehículo por matrícula y obtiene los puntos de su licencia en la ANT. Como la web de la ANT tiene baja disponibilidad, cada respuesta válida se guarda en una caché en la nube (Upstash Redis) y se devuelve desde allí cuando el servicio externo falla, indicando que el dato no es reciente.

| Recurso | Enlace |
|---|---|
| Modelo C4 en IcePanel | [Nivel 1 – Contexto](https://s.icepanel.io/kSUBIBBkZIVwsj/HMm9) · [Nivel 2 – Contenedores](https://s.icepanel.io/kSUBIBBkZIVwsj/iANb) |
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
| `frontend/` | Frontend web | React 18, Vite, TypeScript; servido por Nginx, que también enruta `/api` |
| `backend/sri-service/` | Servicio SRI | Java 21, Spring Boot 3.3, RestClient |
| `backend/ant-service/` | Servicio ANT | Java 21, Spring Boot 3.3, Jsoup |
| (externo) | Caché en la nube | Upstash Redis (TLS) |
| `tools/mock-upstream/` | — | Simulador local del SRI y la ANT para desarrollo |

## Modelo C4 y justificación

C4 es un modelo para describir la arquitectura de un sistema de software en cuatro niveles de detalle: **Contexto**, **Contenedores**, **Componentes** y **Código**. Cada nivel amplía una parte del anterior, de modo que el sistema puede explicarse tanto a personas no técnicas (nivel 1) como a desarrolladores (niveles 2 a 4). El modelo de este proyecto está en IcePanel y su descripción detallada en [`docs/01-icepanel.md`](docs/01-icepanel.md).

**Niveles del modelo**

| Nivel | Qué muestra en este sistema | Dónde está | Justificación |
|---|---|---|---|
| 1. Contexto | El usuario, el Sistema de Consulta SRI-ANT y los sistemas externos SRI y ANT | IcePanel | Delimita qué se construyó y qué depende de terceros. Hace visible que el SRI y la ANT están fuera de nuestro control, que es la razón de diseñar el patrón de caché |
| 2. Contenedores | Frontend web, Servicio SRI, Servicio ANT y Caché en la nube | IcePanel | Muestra las unidades que se ejecutan y despliegan por separado, sus tecnologías y cómo se comunican. Coincide uno a uno con las carpetas del repositorio |
| 3. Componentes | Las piezas internas de cada microservicio y cómo colaboran | Este README | Explica cómo cada servicio implementa la validación, la integración con el proveedor externo y el patrón de caché, que es la decisión central del diseño |
| 4. Código | Las clases que implementan cada componente | Repositorio de GitHub | El propio código es este nivel. Cada contenedor de IcePanel está enlazado a su carpeta y la tabla del nivel 4 lleva de cada componente a su clase, por lo que no se duplica en un diagrama |

### Nivel 3 – Componentes del Servicio SRI

```mermaid
flowchart LR
    web["Frontend web<br/>(React)"]
    subgraph sri["Servicio SRI · Spring Boot"]
        cc["ContribuyenteController<br/>GET /api/v1/contribuyentes/{ruc}"]
        vc["VehiculoController<br/>GET /api/v1/vehiculos/{placa}"]
        val["Validaciones<br/>RUC, correo y placa"]
        cs["ContribuyenteService<br/>contribuyente y persona natural"]
        vs["VehiculoService<br/>vehículo por placa"]
        rf["ResilientFetcher<br/>última respuesta válida"]
        cli["SriClient<br/>HTTP REST, 2 intentos"]
        map["SriMapper<br/>JSON del SRI a DTO"]
        store["RedisSnapshotStore<br/>dato + fecha + TTL"]
        err["GlobalExceptionHandler<br/>errores estructurados"]
    end
    sriext["SRI<br/>(sistema externo)"]
    cache[("Caché en la nube<br/>Upstash Redis")]

    web -->|REST/JSON| cc
    web -->|REST/JSON| vc
    cc --> val
    vc --> val
    cc --> cs
    vc --> vs
    cs --> rf
    vs --> rf
    rf --> store
    cs --> cli
    vs --> cli
    cs --> map
    vs --> map
    cli -->|HTTPS/JSON| sriext
    store -->|Redis TLS| cache
    cc -.->|excepciones| err
    vc -.->|excepciones| err
```

### Nivel 3 – Componentes del Servicio ANT

```mermaid
flowchart LR
    web["Frontend web<br/>(React)"]
    subgraph ant["Servicio ANT · Spring Boot"]
        lc["LicenciaController<br/>GET /api/v1/licencias/{cedula}/puntos"]
        val["Validaciones<br/>cédula y placa"]
        ps["PuntosService<br/>puntos de la licencia"]
        rf["ResilientFetcher<br/>última respuesta válida"]
        cli["AntClient<br/>HTTP, 2 intentos"]
        par["AntParser<br/>extrae puntos del HTML"]
        store["RedisSnapshotStore<br/>dato + fecha + TTL"]
        err["GlobalExceptionHandler<br/>errores estructurados"]
    end
    antext["ANT<br/>(sistema externo)"]
    cache[("Caché en la nube<br/>Upstash Redis")]

    web -->|REST/JSON| lc
    lc --> val
    lc --> ps
    ps --> rf
    rf --> store
    ps --> cli
    ps --> par
    cli -->|HTTPS/HTML| antext
    store -->|Redis TLS| cache
    lc -.->|excepciones| err
```

**Justificación de los componentes**

- **Controladores separados de los servicios**: los controladores solo definen el contrato HTTP (rutas, verbos, parámetros) y delegan la lógica, de modo que la API puede probarse sin conectarse a los proveedores externos.
- **`Validaciones` antes de cualquier consulta**: las entradas inválidas se rechazan con 400 sin gastar una llamada al SRI o a la ANT, y los mensajes de error son claros y estables.
- **Cliente externo aislado** (`SriClient`, `AntClient`): concentra timeouts, reintentos y la traducción de cualquier falla de red o HTTP a `UpstreamException`, la única excepción que activa el respaldo desde la caché.
- **`AntParser` separado de `AntClient`**: la ANT entrega HTML, no una API. Separar la descarga de la extracción permite cambiar la expresión de lectura sin tocar el cliente y descartar páginas inválidas antes de guardarlas.
- **`ResilientFetcher` como componente común**: implementa una sola vez el patrón de última respuesta válida (dato fresco, consulta externa, respaldo desactualizado o 503) y lo reutilizan todos los servicios de negocio.
- **`RedisSnapshotStore` detrás de la interfaz `SnapshotStore`**: guarda el dato junto con la fecha en que se obtuvo, necesaria para el aviso de dato no reciente. Si Redis falla, el sistema sigue funcionando sin caché. La interfaz permite probar el patrón con un almacén en memoria.
- **`GlobalExceptionHandler`**: garantiza que todos los errores salgan con la misma estructura (`codigo`, `mensaje`, `detalles`, `timestamp`, `ruta`) y el código HTTP adecuado.

### Nivel 4 – Código

| Componente | Clase |
|---|---|
| Controladores SRI | [`ContribuyenteController`](backend/sri-service/src/main/java/ec/consulta/sri/api/ContribuyenteController.java), [`VehiculoController`](backend/sri-service/src/main/java/ec/consulta/sri/api/VehiculoController.java) |
| Controlador ANT | [`LicenciaController`](backend/ant-service/src/main/java/ec/consulta/ant/api/LicenciaController.java) |
| Servicios de negocio | [`ContribuyenteService`](backend/sri-service/src/main/java/ec/consulta/sri/service/ContribuyenteService.java), [`VehiculoService`](backend/sri-service/src/main/java/ec/consulta/sri/service/VehiculoService.java), [`PuntosService`](backend/ant-service/src/main/java/ec/consulta/ant/service/PuntosService.java) |
| Patrón de caché | [`ResilientFetcher` (SRI)](backend/sri-service/src/main/java/ec/consulta/sri/service/ResilientFetcher.java), [`ResilientFetcher` (ANT)](backend/ant-service/src/main/java/ec/consulta/ant/service/ResilientFetcher.java) |
| Almacén de caché | [`RedisSnapshotStore` (SRI)](backend/sri-service/src/main/java/ec/consulta/sri/cache/RedisSnapshotStore.java), [`RedisSnapshotStore` (ANT)](backend/ant-service/src/main/java/ec/consulta/ant/cache/RedisSnapshotStore.java) |
| Clientes externos | [`SriClient`](backend/sri-service/src/main/java/ec/consulta/sri/client/SriClient.java), [`AntClient`](backend/ant-service/src/main/java/ec/consulta/ant/client/AntClient.java), [`AntParser`](backend/ant-service/src/main/java/ec/consulta/ant/client/AntParser.java) |
| Validación | [`Validaciones` (SRI)](backend/sri-service/src/main/java/ec/consulta/sri/validation/Validaciones.java), [`Validaciones` (ANT)](backend/ant-service/src/main/java/ec/consulta/ant/validation/Validaciones.java) |
| Errores | [`GlobalExceptionHandler` (SRI)](backend/sri-service/src/main/java/ec/consulta/sri/api/GlobalExceptionHandler.java), [`GlobalExceptionHandler` (ANT)](backend/ant-service/src/main/java/ec/consulta/ant/api/GlobalExceptionHandler.java) |
| Frontend | [`ModuloSri`](frontend/src/components/ModuloSri.tsx), [`ModuloAnt`](frontend/src/components/ModuloAnt.tsx), [`api.ts`](frontend/src/api.ts) |

**Decisiones de arquitectura que refleja el modelo**

- **Un microservicio por proveedor externo** (Servicio SRI y Servicio ANT): el SRI ofrece una API REST estable y la ANT solo una página HTML con baja disponibilidad. Separarlos aísla las fallas: si la ANT cae, la consulta del SRI sigue funcionando, y cada servicio usa la tecnología adecuada (RestClient para JSON, Jsoup para HTML).
- **Caché como contenedor propio (Store) en la nube**: es un componente de infraestructura compartido, externo a los servicios y persistente entre reinicios. Representarla como contenedor deja explícito el patrón de última respuesta válida que exige la baja disponibilidad de la ANT.
- **Frontend como único punto de entrada**: Nginx sirve la SPA y enruta `/api` a cada servicio, por lo que el usuario solo se relaciona con el frontend y los servicios no se exponen directamente.
- **SRI y ANT como sistemas externos**: se modelan fuera del límite del sistema porque no se controlan; sus relaciones indican el protocolo real (REST JSON para el SRI, HTML para la ANT).

## Flujo de uso

La interfaz tiene dos módulos independientes, organizados en pestañas. Cada uno llama a su microservicio y puede usarse sin pasar por el otro.

**Módulo SRI** (`sri-service`)
1. El usuario ingresa correo y RUC → `GET /api/v1/contribuyentes/{ruc}?correo=` verifica que sea contribuyente (`existePorNumeroRuc`), que sea persona natural y devuelve sus datos (`obtenerPorNumerosRuc`).
2. A continuación ingresa la matrícula → `GET /api/v1/vehiculos/{placa}`.

**Módulo ANT** (`ant-service`)
1. El usuario ingresa su cédula y, opcionalmente, la placa → `GET /api/v1/licencias/{cedula}/puntos?placa=` devuelve los puntos de la licencia desde la ANT, o desde la caché si la ANT no responde.

Al cambiar de pestaña se conserva lo consultado; el botón **Nueva consulta** limpia ambos módulos.

## Contrato de la API

Respuesta exitosa (200):

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

Respuesta de error (misma estructura en todos los casos):

```json
{ "error": { "codigo": "RUC_INVALIDO", "mensaje": "…", "detalles": [], "timestamp": "…", "ruta": "/api/v1/contribuyentes/123" } }
```

| Endpoint | Verbo | Códigos |
|---|---|---|
| `/api/v1/contribuyentes/{ruc}?correo=` | GET | 200, 400 `RUC_INVALIDO` / `CORREO_INVALIDO` / `PARAMETRO_REQUERIDO`, 404 `NO_CONTRIBUYENTE`, 422 `NO_PERSONA_NATURAL`, 503 `SERVICIO_EXTERNO_NO_DISPONIBLE` |
| `/api/v1/vehiculos/{placa}` | GET | 200, 400 `PLACA_INVALIDA`, 404 `VEHICULO_NO_ENCONTRADO`, 503 |
| `/api/v1/licencias/{cedula}/puntos?placa=` | GET | 200, 400 `CEDULA_INVALIDA` / `PLACA_INVALIDA`, 503 |

Otros: 405 `METODO_NO_PERMITIDO`, 404 `RUTA_NO_ENCONTRADA`, 500 `ERROR_INTERNO`. El 503 incluye la cabecera `Retry-After: 60`. Un dato servido desde la caché por falla del servicio externo incluye `Warning: 110 - "Response is Stale"`.

Documentación interactiva (Swagger UI): `http://localhost:8081/swagger-ui.html` y `http://localhost:8082/swagger-ui.html`.

## Caché y resiliencia

Patrón de última respuesta válida (`ResilientFetcher`):

1. Si existe un dato guardado con antigüedad menor a `fresh`, se responde desde la caché, lo que evita consultas repetidas a la ANT.
2. En caso contrario se consulta al servicio externo (conexión 3 s, lectura 8–10 s, 2 intentos). Si la respuesta es válida, se guarda junto con su fecha.
3. Si el servicio externo falla (timeout, 5xx, HTML sin puntos, valor fuera de rango) y existe un dato guardado, se devuelve con `desactualizado: true`, la fecha real del dato en `consultadoEn` y una `advertencia`. El frontend muestra un aviso amarillo.
4. Si falla y no existe dato guardado, se responde 503 con el error estructurado.

Solo se guardan respuestas válidas: los errores de negocio (404, 422) y las páginas inválidas de la ANT no llegan a la caché. Si Redis no está disponible, el sistema sigue funcionando sin caché.

**Tiempos de vida (TTL)**, configurables en `application.yml`:

| Dato | TTL | `fresh` | Justificación |
|---|---|---|---|
| Puntos de licencia (ANT) | 7 días | 5 min | Los puntos cambian solo cuando se procesa una citación. La ANT cae con frecuencia, por lo que se conserva una semana; pasado ese tiempo el dato pierde utilidad. Siempre se muestra la fecha de consulta. |
| Contribuyente (SRI) | 30 días | 2 min | Datos del catastro que cambian con muy poca frecuencia. |
| Vehículo (SRI) | 7 días | 2 min | Cambia con traspasos o pago de matrícula; una semana limita el riesgo de mostrar un propietario o estado anterior. |

## Ejecución

Requisitos: JDK 21, Maven 3.9+, Node 20+ y un archivo `.env` con `REDIS_URL` de Upstash (a partir de `.env.example`). La guía completa está en [`docs/03-despliegue.md`](docs/03-despliegue.md).

### Windows

| Archivo | Descripción |
|---|---|
| `iniciar.bat` | Levanta los servicios contra el SRI y la ANT reales |
| `iniciar-simulador.bat` | Levanta los servicios con la ANT simulada (se puede apagar para probar la caché). Los RUC y placas de prueba son simulados; cualquier otro se consulta al SRI real |
| `detener.bat` | Detiene todos los servicios |

La interfaz queda disponible en http://localhost:5173 y los logs en `logs/`. El script usa un JDK 21 instalado aunque `JAVA_HOME` apunte a otra versión. En Linux/macOS: `./dev.sh [--mock]` y `./dev.sh --stop`.

### Docker

```bash
cp .env.example .env                 # REDIS_URL de Upstash: rediss://default:PASSWORD@HOST:6379
docker compose up --build            # servicios reales, web en http://localhost:8080
docker compose -f docker-compose.yml -f docker-compose.mock.yml up --build   # con el simulador
```

### Prueba de la caché con el simulador

```bash
curl -i localhost:8082/api/v1/licencias/1713175071/puntos   # origen SERVICIO_EXTERNO
curl "localhost:9000/__control?ant=down"                    # apaga la ANT simulada
curl -i localhost:8082/api/v1/licencias/1713175071/puntos   # 200, origen CACHE, desactualizado=true, Warning: 110
curl -i localhost:8082/api/v1/licencias/1710034065/puntos   # cédula sin dato previo: 503 + Retry-After
curl "localhost:9000/__control?ant=up"                      # enciende la ANT simulada
```

En modo simulador las claves de la caché llevan el prefijo `sim:`, de modo que nunca se mezclan con los datos reales.

### Pruebas

- Backend: `cd backend && mvn test` (39 pruebas: validaciones, patrón de caché, parser de la ANT y contrato HTTP).
- Frontend: `cd frontend && npm run build`.

## Estado frente a los servicios reales

- **SRI**: la verificación de contribuyente y la consulta de datos funcionan contra el servicio real.
- **ANT**: durante las pruebas del 16 y 17/11/2026 la web de la ANT respondió con errores 401 y 502. El sistema respondió con el error estructurado 503 y el mensaje de servicio no disponible, como corresponde cuando no existe un dato previo en la caché. Como la ANT real no devolvió ninguna respuesta válida, la visualización de los puntos y el respaldo desde la caché se verificaron con el simulador, que reemplaza únicamente a la ANT y mantiene el SRI real. El detalle y las evidencias están en [`docs/02-usabilidad.md`](docs/02-usabilidad.md#disponibilidad-de-la-ant-y-uso-del-simulador).

## Limitaciones conocidas

- **Lectura de la ANT** (`AntParser`, propiedad `ant.puntos-regex`): la ANT no ofrece una API y los puntos se extraen del HTML. La expresión regular es configurable por si cambia la estructura de la página.
- **Campos del vehículo** (`SriMapper.vehiculo`): se aceptan varios nombres de campo posibles; el resto de los valores se muestran desde `detalle`.
- **Certificado de la ANT**: si aparece `PKIX path building failed`, el sitio tiene la cadena de certificados incompleta y se debe importar el certificado intermedio en el truststore de Java.

## Prueba de usabilidad

Procedimiento y registro en [`docs/02-usabilidad.md`](docs/02-usabilidad.md). La interfaz incluye el botón **Descargar registro de la prueba (CSV)**, que guarda los tiempos, éxitos y errores de cada acción.

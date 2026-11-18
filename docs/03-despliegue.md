# Instalación y despliegue

Requisitos: **Java 21**, **Maven 3.9+**, **Node 20+**, **Git** y, opcionalmente, **Docker**.

## 1. Caché en la nube (Upstash Redis)

1. Crear una cuenta en https://upstash.com (el plan gratuito es suficiente).
2. En la consola, **Create Database** en la sección Redis.
3. Nombre `consulta-cache`, tipo **Regional**, región cercana a Ecuador (por ejemplo **São Paulo (sa-east-1)** o **N. Virginia (us-east-1)**) y **TLS/SSL** activado.
4. En la página de detalle se obtienen **Endpoint**, **Port** (6379) y **Password**.
5. La URL de conexión tiene la forma:
   `rediss://default:PASSWORD@HOST.upstash.io:6379`
   (`rediss` indica conexión con TLS; el usuario es `default`).
6. Prueba opcional: `redis-cli --tls -u "rediss://default:PASSWORD@HOST:6379" ping` debe responder `PONG`.
7. La URL se guarda en el archivo `.env` de la raíz (a partir de `.env.example`): `REDIS_URL=rediss://...`. El archivo `.env` está excluido del repositorio en `.gitignore`.

## 2. Ejecución en Windows con scripts

| Archivo | Descripción |
|---|---|
| `iniciar.bat` | Levanta `sri-service`, `ant-service` y el frontend contra el SRI y la ANT reales |
| `iniciar-simulador.bat` | Igual, pero con la ANT simulada y los datos de prueba simulados |
| `detener.bat` | Detiene todos los servicios |

Los scripts ejecutan `dev.ps1`, que carga `.env`, selecciona un JDK 21 instalado, cierra procesos anteriores del proyecto que ocupen los puertos 9000, 8081, 8082 o 5173, y guarda los logs en `logs/`. La interfaz queda en http://localhost:5173.

En Linux/macOS: `./dev.sh`, `./dev.sh --mock` y `./dev.sh --stop`.

## 3. Ejecución con Docker

```bash
cp .env.example .env     # configurar REDIS_URL
docker compose up --build
```

- Interfaz: http://localhost:8080
- Swagger SRI: http://localhost:8081/swagger-ui.html · Swagger ANT: http://localhost:8082/swagger-ui.html

Con el simulador:

```bash
docker compose -f docker-compose.yml -f docker-compose.mock.yml up --build
```

La primera construcción tarda varios minutos porque descarga las dependencias de Maven y npm.

## 4. Ejecución manual

Cada comando en una terminal distinta:

```bash
# Servicio SRI
export REDIS_URL="rediss://default:PASSWORD@HOST:6379"
cd backend && mvn -pl sri-service spring-boot:run

# Servicio ANT
export REDIS_URL="rediss://default:PASSWORD@HOST:6379"
cd backend && mvn -pl ant-service spring-boot:run

# Frontend
cd frontend && npm install && npm run dev      # http://localhost:5173
```

En PowerShell la variable se define con `$env:REDIS_URL="rediss://..."`.

## 5. Simulador del SRI y la ANT

`tools/mock-upstream/server.js` escucha en http://localhost:9000:

- RUC `1713175071001` (persona natural), RUC `1790016919001` (sociedad, error 422) y placa `PCQ1234` son datos simulados.
- Cualquier otro RUC o placa se reenvía al SRI real.
- La ANT es siempre simulada y se controla con `/__control?ant=down` y `/__control?ant=up`.

Variables que usan los servicios en este modo (las configura `iniciar-simulador.bat`):

```bash
export SRI_CATASTRO_URL=http://localhost:9000/sri/catastro
export SRI_VEHICULO_URL=http://localhost:9000/sri/vehiculo
export ANT_URL=http://localhost:9000/ant/clp_grid_citaciones.jsp
export SRI_CACHE_FRESH=0s ANT_CACHE_FRESH=0s   # cada consulta intenta primero el servicio externo
export CACHE_PREFIJO=sim:                      # separa la caché simulada de la real
```

## 6. Verificación de la caché

Con el simulador levantado:

```bash
# 1) ANT disponible: origen SERVICIO_EXTERNO, desactualizado=false
curl -i localhost:8082/api/v1/licencias/1713175071/puntos

# 2) Se apaga la ANT
curl "localhost:9000/__control?ant=down"

# 3) Misma consulta: 200 con origen CACHE, desactualizado=true, advertencia y cabecera Warning
curl -i localhost:8082/api/v1/licencias/1713175071/puntos

# 4) Cédula sin dato previo con la ANT apagada: 503 SERVICIO_EXTERNO_NO_DISPONIBLE y Retry-After
curl -i localhost:8082/api/v1/licencias/1710034065/puntos

# 5) Se enciende la ANT
curl "localhost:9000/__control?ant=up"
```

En el **Data Browser** de Upstash se observan las claves `ant:puntos:*`, `sri:contribuyente:*` y `sri:vehiculo:*` con su TTL (con el prefijo `sim:` en modo simulador).

Pruebas del contrato:

```bash
curl -i "localhost:8081/api/v1/contribuyentes/123?correo=a@b.com"           # 400 RUC_INVALIDO
curl -i "localhost:8081/api/v1/contribuyentes/1713175071001"                # 400 PARAMETRO_REQUERIDO
curl -i "localhost:8081/api/v1/contribuyentes/1790016919001?correo=a@b.com" # 422 NO_PERSONA_NATURAL (simulador)
curl -i "localhost:8081/api/v1/vehiculos/ZZZ9999"                           # 404 VEHICULO_NO_ENCONTRADO
```

## 7. Pruebas automáticas

```bash
cd backend && mvn test
```

Cubren las validaciones, el patrón de caché (dato guardado, respaldo desactualizado, sin dato, dato fresco y errores de negocio), el parser de la ANT y el contrato HTTP de los controladores.

## 8. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Unable to connect to Redis` al iniciar | `REDIS_URL` mal escrita o sin `rediss://` | Revisar la URL; Upstash usa `rediss://` y puerto 6379 |
| `release version 21 not supported` | Maven se ejecuta con un JDK anterior | Usar los scripts o definir `JAVA_HOME` con un JDK 21 |
| Todas las respuestas son `CACHE` | Ventana `fresh` activa | Definir `ANT_CACHE_FRESH=0s` / `SRI_CACHE_FRESH=0s` |
| `PKIX path building failed` al llamar a la ANT | Cadena de certificados incompleta en el sitio de la ANT | Importar el certificado intermedio al truststore de Java (`keytool -importcert`) |
| La ANT responde 200 pero la API devuelve 503 | El parser no encuentra los puntos en el HTML | Revisar el HTML y actualizar `ANT_PUNTOS_REGEX` |
| Error de `CORS` en el navegador | Frontend abierto desde otro origen | Configurar `CORS_ALLOWED_ORIGINS` o usar el proxy de Vite / Nginx |

## 9. Publicación en la nube (opcional)

Render o Railway permiten desplegar cada `Dockerfile`: el backend con el argumento `MODULE=sri-service` o `MODULE=ant-service`, y `frontend/`. En cada servicio se configura `REDIS_URL` como variable de entorno y el `nginx.conf` del frontend se apunta a las URL públicas de los servicios.

# Guía de instalación y despliegue

Requisitos en tu máquina: **Java 21**, **Maven 3.9+**, **Node 20+** (22 recomendado), **Git**, y opcionalmente **Docker**.

## 1. Crear la caché en la nube (Upstash Redis)

1. Regístrate en https://upstash.com (puedes entrar con GitHub; el plan gratuito no pide tarjeta).
2. En la consola pulsa **Create Database** (sección Redis).
3. Nombre: `consulta-cache`. Tipo: **Regional**. Región: la más cercana a Ecuador (por ejemplo **São Paulo (sa-east-1)** o **N. Virginia (us-east-1)**). Deja **TLS/SSL** activado.
4. Crea la base. En la página de detalle copia **Endpoint** (algo como `fit-lizard-12345.upstash.io`), **Port** (6379) y **Password**.
5. Arma la URL:  
   `rediss://default:TU_PASSWORD@fit-lizard-12345.upstash.io:6379`  
   (`rediss` con doble *s* = conexión con TLS; el usuario siempre es `default`).
6. (Opcional) probar con redis-cli: `redis-cli --tls -u "rediss://default:TU_PASSWORD@HOST:6379" ping` → debe responder `PONG`.
7. Guárdala en el archivo `.env` de la raíz del proyecto (copia de `.env.example`): `REDIS_URL=rediss://...`. **Nunca subas `.env` a GitHub** (ya está en `.gitignore`).

## 2. Opción A: todo con Docker (lo más simple)

```bash
cp .env.example .env     # edita REDIS_URL
docker compose up --build
```

- Interfaz: http://localhost:8080
- Swagger SRI: http://localhost:8081/swagger-ui.html · Swagger ANT: http://localhost:8082/swagger-ui.html

Primera construcción: tarda varios minutos (descarga dependencias de Maven y npm).

## 3. Opción B: sin Docker (desarrollo)

Cada comando en una terminal distinta:

```bash
# Terminal 1 – servicio SRI
export REDIS_URL="rediss://default:TU_PASSWORD@HOST:6379"
cd backend && mvn -pl sri-service spring-boot:run

# Terminal 2 – servicio ANT
export REDIS_URL="rediss://default:TU_PASSWORD@HOST:6379"
cd backend && mvn -pl ant-service spring-boot:run

# Terminal 3 – frontend
cd frontend && npm install && npm run dev      # http://localhost:5173
```

En Windows PowerShell usa `$env:REDIS_URL="rediss://..."` en lugar de `export`.

## 4. Modo demostración sin depender del SRI/ANT reales

Útil para la prueba de usabilidad y para demostrar la caché:

```bash
node tools/mock-upstream/server.js        # simulador en http://localhost:9000
```

Arranca los servicios apuntando al simulador (variables de entorno):

```bash
export SRI_CATASTRO_URL=http://localhost:9000/sri/catastro
export SRI_VEHICULO_URL=http://localhost:9000/sri/vehiculo
export ANT_URL=http://localhost:9000/ant/clp_grid_citaciones.jsp
export ANT_CACHE_FRESH=0s     # para que cada consulta intente primero la ANT y se vea el respaldo
```

Datos de prueba: RUC `1713175071001` (persona natural), RUC `1790016919001` (sociedad → error 422), matrícula `PCQ1234`.

## 5. Cómo demostrar que la caché funciona (evidencia para la rúbrica)

```bash
# 1) Con la ANT "encendida": origen SERVICIO_EXTERNO, desactualizado=false
curl -i localhost:8082/api/v1/licencias/1713175071/puntos

# 2) Apagar la ANT
curl "localhost:9000/__control?ant=down"

# 3) Misma consulta: 200 con origen CACHE, desactualizado=true, advertencia, cabecera Warning
curl -i localhost:8082/api/v1/licencias/1713175071/puntos

# 4) Cédula nunca consultada con la ANT apagada: 503 SERVICIO_EXTERNO_NO_DISPONIBLE
curl -i localhost:8082/api/v1/licencias/0900123456/puntos
```

En la consola de Upstash (**Data Browser**) verás las claves `ant:puntos:1713175071`, `sri:contribuyente:...`, `sri:vehiculo:...` con su TTL: sirve de captura de evidencia.

Otras pruebas del contrato:

```bash
curl -i "localhost:8081/api/v1/contribuyentes/123?correo=a@b.com"          # 400 RUC_INVALIDO
curl -i "localhost:8081/api/v1/contribuyentes/1713175071001"               # 400 PARAMETRO_REQUERIDO
curl -i "localhost:8081/api/v1/contribuyentes/1790016919001?correo=a@b.com" # 422 NO_PERSONA_NATURAL (con el simulador)
curl -i "localhost:8081/api/v1/vehiculos/ZZZ9999"                          # 404 VEHICULO_NO_ENCONTRADO (con el simulador)
```

## 6. Pruebas automáticas

```bash
cd backend && mvn test
```

Cubren validaciones, el patrón de caché (dato guardado, respaldo desactualizado, sin dato, dato fresco, errores de negocio), el parser de la ANT y el contrato HTTP de los controladores.

## 7. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Unable to connect to Redis` al iniciar | `REDIS_URL` mal escrita o sin `rediss://` | Revisa la URL; con Upstash siempre `rediss://` y puerto 6379 |
| Todas las respuestas son `CACHE` en el simulador | Ventana `fresh` activa | Usa `ANT_CACHE_FRESH=0s` / `SRI_CACHE_FRESH=0s` |
| `PKIX path building failed` al llamar a la ANT | Cadena de certificados incompleta en el sitio de la ANT | Importa el certificado intermedio al truststore de Java (`keytool -importcert`) |
| La ANT devuelve 200 pero siempre 503 en tu API | El parser no encuentra los puntos | Revisa el HTML real y ajusta `ANT_PUNTOS_REGEX` |
| `CORS` en el navegador | Frontend abierto desde otro origen | Ajusta `CORS_ALLOWED_ORIGINS` o usa el proxy de Vite / Nginx |
| `mvn` descarga dependencias eternamente | Primera vez / red lenta | Esperar; las siguientes ejecuciones usan la caché local |

## 8. (Opcional) Publicar en la nube

La rúbrica solo exige la **caché en la nube**; la app puede correr local. Si quieres publicarla: Render o Railway permiten desplegar cada `Dockerfile` (backend con argumento `MODULE=sri-service` / `ant-service`, y `frontend/`), configurando `REDIS_URL` como variable de entorno y apuntando el frontend a las URLs públicas de los servicios en un `nginx.conf` ajustado.

#!/usr/bin/env bash
# Linux/macOS:  ./dev.sh [--mock] | ./dev.sh --stop
set -e
cd "$(dirname "$0")"
PIDS=.dev-pids
stop() { [ -f $PIDS ] && while read -r p; do kill -- -"$p" 2>/dev/null || kill "$p" 2>/dev/null || true; done < $PIDS; rm -f $PIDS; echo "Detenido."; }
[ "$1" = "--stop" ] && { stop; exit 0; }
[ -f $PIDS ] && stop
[ -f .env ] || { echo "Falta .env (copia .env.example y pon REDIS_URL)"; exit 1; }
set -a; . ./.env; set +a
if [ "$1" = "--mock" ]; then
  export SRI_CATASTRO_URL=http://localhost:9000/sri/catastro SRI_VEHICULO_URL=http://localhost:9000/sri/vehiculo \
         ANT_URL=http://localhost:9000/ant/clp_grid_citaciones.jsp SRI_CACHE_FRESH=0s ANT_CACHE_FRESH=0s CACHE_PREFIJO=sim:
fi
mkdir -p logs
run() { name=$1; dir=$2; shift 2; (cd "$dir" && setsid bash -c "$*" > "../logs/$name.log" 2>&1 & echo $! >> "../$PIDS"); echo "Iniciando $name (logs/$name.log)"; }
touch $PIDS
[ "$1" = "--mock" ] && run mock tools/mock-upstream "node server.js"
run sri-service backend  "mvn -q -pl sri-service spring-boot:run"
run ant-service backend  "mvn -q -pl ant-service spring-boot:run"
run frontend    frontend "npm install && npm run dev"
echo "Abre http://localhost:5173 cuando los logs indiquen que arrancó. Detener: ./dev.sh --stop"

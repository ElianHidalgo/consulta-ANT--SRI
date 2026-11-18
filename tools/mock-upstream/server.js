// Simulador local del SRI y de la ANT, para desarrollar y demostrar la caché sin depender de los servicios reales.
//   node server.js            -> escucha en http://localhost:9000
//   curl localhost:9000/__control?ant=down   -> la ANT empieza a responder 503
//   curl localhost:9000/__control?ant=up     -> la ANT vuelve a responder
//   (igual con sri=down|up)
// Datos simulados: RUC 1713175071001 (persona natural), 1790016919001 (sociedad) y placa PCQ1234.
// Cualquier otro RUC o placa se reenvía al SRI real. La ANT siempre es simulada.
const http = require('http');
const { URL } = require('url');

const estado = { ant: 'up', sri: 'up' };
const SRI_REAL = {
  catastro: 'https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente',
  vehiculo: 'https://srienlinea.sri.gob.ec/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo',
};
const RUCS_PRUEBA = ['1713175071001', '1790016919001'];
const PLACAS_PRUEBA = ['PCQ1234'];

/** Reenvía la consulta al SRI real y devuelve su respuesta tal cual (o 502 si no se puede conectar). */
async function reenviarSri(res, base, ruta, query) {
  try {
    const r = await fetch(`${base}${ruta}?${query}`, {
      headers: { Accept: 'application/json, text/plain, */*', 'User-Agent': 'consulta-sri-ant-mock/1.0' },
      signal: AbortSignal.timeout(10000),
    });
    const cuerpo = await r.text();
    console.log(`[SRI real] ${ruta} -> ${r.status}`);
    return enviar(res, r.status, cuerpo, r.headers.get('content-type') || 'text/plain');
  } catch (e) {
    console.log(`[SRI real] ${ruta} -> sin conexión (${e.message})`);
    return enviar(res, 502, { error: 'No se pudo conectar con el SRI real' });
  }
}
const PORT = process.env.PORT || 9000;

const contribuyente = (ruc) => [{
  numeroRuc: ruc,
  razonSocial: 'PEREZ GOMEZ JUAN CARLOS',
  estadoContribuyenteRuc: 'ACTIVO',
  actividadEconomicaPrincipal: 'ACTIVIDADES DE CONSULTORIA INFORMATICA',
  tipoContribuyente: 'PERSONA NATURAL',
  personaSociedad: 'PNL',
  regimen: 'GENERAL',
  obligadoLlevarContabilidad: 'NO',
  informacionFechasContribuyente: { fechaInicioActividades: '2015-03-02', fechaActualizacion: '2024-06-10' },
}];

const sociedad = (ruc) => [{ numeroRuc: ruc, razonSocial: 'EMPRESA DEMO S.A.', tipoContribuyente: 'SOCIEDADES', personaSociedad: 'SCD' }];

const vehiculo = {
  placa: 'PCQ1234', descripcionMarca: 'CHEVROLET', descripcionModelo: 'SAIL', anioModelo: 2019,
  descripcionColor: 'BLANCO', descripcionClase: 'AUTOMOVIL', descripcionServicio: 'USO PARTICULAR',
  cilindraje: 1500, paisFabricacion: 'COLOMBIA', ultimoAnioPagado: 2025,
};

function enviar(res, code, body, tipo = 'application/json') {
  res.writeHead(code, { 'Content-Type': tipo });
  res.end(typeof body === 'string' ? body : JSON.stringify(body));
}

http.createServer(async (req, res) => {
  const u = new URL(req.url, `http://${req.headers.host}`);
  const p = u.pathname;

  if (p === '/__control') {
    for (const k of ['ant', 'sri']) if (u.searchParams.get(k)) estado[k] = u.searchParams.get(k);
    return enviar(res, 200, estado);
  }
  if (p.startsWith('/sri/') && estado.sri === 'down') return enviar(res, 503, { error: 'SRI caído (simulado)' });
  if (p.startsWith('/ant/') && estado.ant === 'down') return enviar(res, 503, 'ANT caída (simulada)', 'text/html');

  if (p === '/sri/catastro/existePorNumeroRuc') {
    const ruc = u.searchParams.get('numeroRuc');
    if (!RUCS_PRUEBA.includes(ruc)) return reenviarSri(res, SRI_REAL.catastro, '/existePorNumeroRuc', u.searchParams);
    return enviar(res, 200, 'true', 'text/plain');
  }
  if (p === '/sri/catastro/obtenerPorNumerosRuc') {
    const ruc = u.searchParams.get('ruc');
    if (!RUCS_PRUEBA.includes(ruc)) return reenviarSri(res, SRI_REAL.catastro, '/obtenerPorNumerosRuc', u.searchParams);
    return enviar(res, 200, ruc.charAt(2) === '9' ? sociedad(ruc) : contribuyente(ruc));
  }
  if (p === '/sri/vehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn') {
    const placa = u.searchParams.get('numeroPlacaCampvCpn');
    if (!PLACAS_PRUEBA.includes(placa)) {
      return reenviarSri(res, SRI_REAL.vehiculo, '/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn', u.searchParams);
    }
    return enviar(res, 200, vehiculo);
  }
  if (p === '/ant/clp_grid_citaciones.jsp') {
    const ced = u.searchParams.get('ps_identificacion');
    return enviar(res, 200,
      `<html><body><h3>Consulta de citaciones</h3><div>Cédula: ${ced}</div><div><b>Puntos en la licencia:</b> <span>28</span></div></body></html>`,
      'text/html');
  }
  enviar(res, 404, { error: 'no encontrado' });
}).listen(PORT, () => console.log(`Mock SRI/ANT escuchando en http://localhost:${PORT}`));

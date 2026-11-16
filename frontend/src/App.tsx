import { useState } from 'react';
import { ApiError, consultarContribuyente, consultarPuntos, consultarVehiculo } from './api';
import { Cargando, ErrorCaja } from './components/Aviso';
import { FormIdentificacion } from './components/FormIdentificacion';
import { FormPlaca } from './components/FormPlaca';
import { TarjetaContribuyente, TarjetaPuntos, TarjetaVehiculo } from './components/Tarjetas';
import type { ApiResponse, Contribuyente, Estado, LicenciaPuntos, Vehiculo } from './types';
import { descargarCsv, registrar, reiniciarRegistro } from './usabilityLog';

const inactivo = { tipo: 'inactivo' } as const;

function aError(e: unknown): { tipo: 'error'; codigo: string; mensaje: string } {
  if (e instanceof ApiError) return { tipo: 'error', codigo: e.codigo, mensaje: e.message };
  return { tipo: 'error', codigo: 'ERROR', mensaje: 'Ocurrió un error inesperado.' };
}

export default function App() {
  const [contribuyente, setContribuyente] = useState<Estado<Contribuyente>>(inactivo);
  const [vehiculo, setVehiculo] = useState<Estado<Vehiculo>>(inactivo);
  const [puntos, setPuntos] = useState<Estado<LicenciaPuntos>>(inactivo);

  async function identificar(correo: string, ruc: string) {
    registrar('envio_identificacion');
    setContribuyente({ tipo: 'cargando' });
    setVehiculo(inactivo);
    setPuntos(inactivo);
    try {
      const r: ApiResponse<Contribuyente> = await consultarContribuyente(ruc, correo);
      setContribuyente({ tipo: 'ok', respuesta: r });
      registrar('exito_identificacion', r.meta.origen + (r.meta.desactualizado ? ' (no reciente)' : ''));
      // La cédula son los 10 primeros dígitos del RUC de una persona natural.
      void cargarPuntos(ruc.substring(0, 10));
    } catch (e) {
      const err = aError(e);
      setContribuyente(err);
      registrar('error_identificacion', err.codigo);
    }
  }

  async function cargarPuntos(cedula: string) {
    setPuntos({ tipo: 'cargando' });
    try {
      const r = await consultarPuntos(cedula);
      setPuntos({ tipo: 'ok', respuesta: r });
      registrar('exito_puntos', r.meta.origen + (r.meta.desactualizado ? ' (no reciente)' : ''));
    } catch (e) {
      const err = aError(e);
      setPuntos(err);
      registrar('error_puntos', err.codigo);
    }
  }

  async function buscarVehiculo(placa: string) {
    registrar('envio_placa');
    setVehiculo({ tipo: 'cargando' });
    try {
      const r = await consultarVehiculo(placa);
      setVehiculo({ tipo: 'ok', respuesta: r });
      registrar('exito_vehiculo', r.meta.origen + (r.meta.desactualizado ? ' (no reciente)' : ''));
    } catch (e) {
      const err = aError(e);
      setVehiculo(err);
      registrar('error_vehiculo', err.codigo);
    }
  }

  function reiniciar() {
    setContribuyente(inactivo);
    setVehiculo(inactivo);
    setPuntos(inactivo);
    reiniciarRegistro();
  }

  const identificado = contribuyente.tipo === 'ok';

  return (
    <main>
      <header>
        <h1>Consulta de contribuyente, vehículo y licencia</h1>
        <p>Verifique su RUC en el SRI, consulte su vehículo y los puntos de su licencia.</p>
      </header>

      <div className="tarjeta">
        <FormIdentificacion cargando={contribuyente.tipo === 'cargando'} onEnviar={identificar} />
        {contribuyente.tipo === 'cargando' && <Cargando texto="Verificando el RUC en el SRI…" />}
        {contribuyente.tipo === 'error' && <ErrorCaja codigo={contribuyente.codigo} mensaje={contribuyente.mensaje} />}
      </div>

      {contribuyente.tipo === 'ok' && <TarjetaContribuyente r={contribuyente.respuesta} />}
      <TarjetaPuntos estado={puntos} />

      {identificado && (
        <>
          <div className="tarjeta">
            <FormPlaca cargando={vehiculo.tipo === 'cargando'} onEnviar={buscarVehiculo} />
          </div>
          <TarjetaVehiculo estado={vehiculo} />
        </>
      )}

      <footer>
        <button type="button" className="secundario" onClick={reiniciar}>
          Nueva consulta
        </button>
        <button type="button" className="secundario" onClick={descargarCsv}>
          Descargar registro de la prueba (CSV)
        </button>
      </footer>
    </main>
  );
}

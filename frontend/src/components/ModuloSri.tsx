import { useState } from 'react';
import { consultarContribuyente, consultarVehiculo } from '../api';
import type { Contribuyente, Estado, Vehiculo } from '../types';
import { registrar } from '../usabilityLog';
import { Cargando, ErrorCaja, aError } from './Aviso';
import { FormIdentificacion } from './FormIdentificacion';
import { FormPlaca } from './FormPlaca';
import { TarjetaContribuyente, TarjetaVehiculo } from './Tarjetas';

const inactivo = { tipo: 'inactivo' } as const;

/** Módulo SRI: verifica al contribuyente (persona natural) y luego consulta su vehículo. */
export function ModuloSri() {
  const [contribuyente, setContribuyente] = useState<Estado<Contribuyente>>(inactivo);
  const [vehiculo, setVehiculo] = useState<Estado<Vehiculo>>(inactivo);

  async function identificar(correo: string, ruc: string) {
    registrar('sri_envio_identificacion');
    setContribuyente({ tipo: 'cargando' });
    setVehiculo(inactivo);
    try {
      const r = await consultarContribuyente(ruc, correo);
      setContribuyente({ tipo: 'ok', respuesta: r });
      registrar('sri_exito_identificacion', r.meta.origen + (r.meta.desactualizado ? ' (no reciente)' : ''));
    } catch (e) {
      const err = aError(e);
      setContribuyente(err);
      registrar('sri_error_identificacion', err.codigo);
    }
  }

  async function buscarVehiculo(placa: string) {
    registrar('sri_envio_placa');
    setVehiculo({ tipo: 'cargando' });
    try {
      const r = await consultarVehiculo(placa);
      setVehiculo({ tipo: 'ok', respuesta: r });
      registrar('sri_exito_vehiculo', r.meta.origen + (r.meta.desactualizado ? ' (no reciente)' : ''));
    } catch (e) {
      const err = aError(e);
      setVehiculo(err);
      registrar('sri_error_vehiculo', err.codigo);
    }
  }

  return (
    <>
      <div className="tarjeta">
        <FormIdentificacion cargando={contribuyente.tipo === 'cargando'} onEnviar={identificar} />
        {contribuyente.tipo === 'cargando' && <Cargando texto="Verificando el RUC en el SRI…" />}
        {contribuyente.tipo === 'error' && <ErrorCaja codigo={contribuyente.codigo} mensaje={contribuyente.mensaje} />}
      </div>

      {contribuyente.tipo === 'ok' && (
        <>
          <TarjetaContribuyente r={contribuyente.respuesta} />
          <div className="tarjeta">
            <FormPlaca cargando={vehiculo.tipo === 'cargando'} onEnviar={buscarVehiculo} />
          </div>
          <TarjetaVehiculo estado={vehiculo} />
        </>
      )}
    </>
  );
}

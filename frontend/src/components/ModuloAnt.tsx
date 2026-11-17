import { FormEvent, useState } from 'react';
import { consultarPuntos } from '../api';
import type { Estado, LicenciaPuntos } from '../types';
import { registrar } from '../usabilityLog';
import { aError } from './Aviso';
import { TarjetaPuntos } from './Tarjetas';

const inactivo = { tipo: 'inactivo' } as const;

/** Módulo ANT: consulta los puntos de la licencia con la cédula, independiente del módulo SRI. */
export function ModuloAnt() {
  const [cedula, setCedula] = useState('');
  const [placa, setPlaca] = useState('');
  const [errores, setErrores] = useState<{ cedula?: string; placa?: string }>({});
  const [puntos, setPuntos] = useState<Estado<LicenciaPuntos>>(inactivo);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const c = cedula.trim();
    const p = placa.trim().toUpperCase().replace(/[-\s]/g, '');
    const nuevos: { cedula?: string; placa?: string } = {};
    if (!/^\d{10}$/.test(c)) nuevos.cedula = 'La cédula debe tener 10 dígitos (solo números)';
    if (p && !/^[A-Z0-9]{5,17}$/.test(p)) nuevos.placa = 'Ingrese una placa válida, por ejemplo PCQ1234, o déjela vacía';
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;

    registrar('ant_envio_cedula');
    setPuntos({ tipo: 'cargando' });
    try {
      const r = await consultarPuntos(c, p || undefined);
      setPuntos({ tipo: 'ok', respuesta: r });
      registrar('ant_exito_puntos', r.meta.origen + (r.meta.desactualizado ? ' (no reciente)' : ''));
    } catch (err) {
      const x = aError(err);
      setPuntos(x);
      registrar('ant_error_puntos', x.codigo);
    }
  }

  const cargando = puntos.tipo === 'cargando';

  return (
    <>
      <div className="tarjeta">
        <form onSubmit={enviar} noValidate>
          <h2>Puntos de la licencia (ANT)</h2>
          <label htmlFor="cedula">Cédula</label>
          <input
            id="cedula"
            inputMode="numeric"
            maxLength={10}
            placeholder="Ej. 1713175071"
            value={cedula}
            onChange={(e) => setCedula(e.target.value.replace(/\D/g, ''))}
            aria-invalid={!!errores.cedula}
            aria-describedby="cedula-error"
          />
          {errores.cedula && (
            <span id="cedula-error" className="campo-error">
              {errores.cedula}
            </span>
          )}

          <label htmlFor="placa-ant">
            Placa <span className="opcional">(opcional)</span>
          </label>
          <input
            id="placa-ant"
            maxLength={20}
            placeholder="Ej. PCQ1234"
            value={placa}
            onChange={(e) => setPlaca(e.target.value.toUpperCase())}
            aria-invalid={!!errores.placa}
            aria-describedby="placa-ant-error"
          />
          {errores.placa && (
            <span id="placa-ant-error" className="campo-error">
              {errores.placa}
            </span>
          )}

          <button type="submit" disabled={cargando}>
            {cargando ? 'Consultando…' : 'Consultar puntos'}
          </button>
        </form>
      </div>
      <TarjetaPuntos estado={puntos} />
    </>
  );
}

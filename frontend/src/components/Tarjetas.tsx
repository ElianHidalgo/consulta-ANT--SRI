import type { ApiResponse, Contribuyente, LicenciaPuntos, Vehiculo, Estado } from '../types';
import { AvisoDatoAntiguo, Cargando, ErrorCaja, fechaLegible } from './Aviso';

function Fila({ k, v }: { k: string; v?: string }) {
  if (!v) return null;
  return (
    <>
      <dt>{k}</dt>
      <dd>{v}</dd>
    </>
  );
}

function Origen({ r }: { r: ApiResponse<unknown> }) {
  if (r.meta.desactualizado) return null;
  return (
    <small className="origen">
      {r.meta.origen === 'CACHE' ? 'Dato reciente guardado' : 'Consultado ahora'} · {fechaLegible(r.meta.consultadoEn)}
    </small>
  );
}

export function TarjetaContribuyente({ r }: { r: ApiResponse<Contribuyente> }) {
  const c = r.datos;
  return (
    <section className="tarjeta">
      <h2>Contribuyente</h2>
      <AvisoDatoAntiguo meta={r.meta} />
      <dl>
        <Fila k="RUC" v={c.ruc} />
        <Fila k="Nombre" v={c.razonSocial} />
        <Fila k="Estado" v={c.estado} />
        <Fila k="Tipo" v={c.tipoContribuyente} />
        <Fila k="Régimen" v={c.regimen} />
        <Fila k="Actividad económica" v={c.actividadEconomica} />
        <Fila k="Obligado a llevar contabilidad" v={c.obligadoContabilidad} />
        <Fila k="Inicio de actividades" v={c.fechaInicioActividades} />
      </dl>
      <Origen r={r} />
    </section>
  );
}

export function TarjetaVehiculo({ estado }: { estado: Estado<Vehiculo> }) {
  if (estado.tipo === 'inactivo') return null;
  if (estado.tipo === 'cargando') return <Cargando texto="Buscando vehículo…" />;
  if (estado.tipo === 'error') return <ErrorCaja codigo={estado.codigo} mensaje={estado.mensaje} />;
  const v = estado.respuesta.datos;
  const principales = ['marca', 'modelo', 'anio', 'color', 'clase', 'servicio'];
  const extra = Object.entries(v.detalle ?? {}).filter(([k]) => !principales.includes(k)).slice(0, 14);
  return (
    <section className="tarjeta">
      <h2>Vehículo {v.placa}</h2>
      <AvisoDatoAntiguo meta={estado.respuesta.meta} />
      <dl>
        <Fila k="Marca" v={v.marca} />
        <Fila k="Modelo" v={v.modelo} />
        <Fila k="Año" v={v.anio} />
        <Fila k="Color" v={v.color} />
        <Fila k="Clase" v={v.clase} />
        <Fila k="Servicio" v={v.servicio} />
        {extra.map(([k, val]) => (
          <Fila key={k} k={k} v={val} />
        ))}
      </dl>
      <Origen r={estado.respuesta} />
    </section>
  );
}

export function TarjetaPuntos({ estado }: { estado: Estado<LicenciaPuntos> }) {
  if (estado.tipo === 'inactivo') return null;
  if (estado.tipo === 'cargando') return <Cargando texto="Consultando puntos de la licencia en la ANT…" />;
  if (estado.tipo === 'error') return <ErrorCaja codigo={estado.codigo} mensaje={estado.mensaje} />;
  const p = estado.respuesta.datos;
  const porcentaje = Math.round((p.puntos / p.puntosMaximos) * 100);
  return (
    <section className="tarjeta">
      <h2>Puntos de la licencia · cédula {p.cedula}</h2>
      <AvisoDatoAntiguo meta={estado.respuesta.meta} />
      <p className="puntos">
        <span>{p.puntos}</span> de {p.puntosMaximos} puntos
      </p>
      <div className="barra" role="img" aria-label={`${p.puntos} de ${p.puntosMaximos} puntos`}>
        <div style={{ width: `${porcentaje}%` }} />
      </div>
      <Origen r={estado.respuesta} />
    </section>
  );
}

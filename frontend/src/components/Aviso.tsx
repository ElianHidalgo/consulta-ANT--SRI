import type { Meta } from '../types';

export function fechaLegible(iso: string): string {
  const d = new Date(iso);
  return isNaN(d.getTime()) ? iso : d.toLocaleString('es-EC');
}

/** Aviso cuando el dato viene de la caché porque el servicio externo falló. */
export function AvisoDatoAntiguo({ meta }: { meta: Meta }) {
  if (!meta.desactualizado) return null;
  return (
    <div className="aviso" role="alert">
      <strong>Dato no reciente.</strong> El servicio externo no respondió; se muestra el último dato obtenido el{' '}
      {fechaLegible(meta.consultadoEn)}. Puede haber cambiado desde entonces.
    </div>
  );
}

export function Cargando({ texto }: { texto: string }) {
  return (
    <p className="cargando" role="status">
      {texto}
    </p>
  );
}

export function ErrorCaja({ codigo, mensaje }: { codigo: string; mensaje: string }) {
  return (
    <div className="error" role="alert">
      <strong>{mensaje}</strong>
      <small>Código: {codigo}</small>
    </div>
  );
}

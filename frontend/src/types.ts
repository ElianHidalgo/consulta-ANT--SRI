export interface Meta {
  origen: 'SERVICIO_EXTERNO' | 'CACHE';
  desactualizado: boolean;
  consultadoEn: string;
  advertencia?: string;
}

export interface ApiResponse<T> {
  datos: T;
  meta: Meta;
}

export interface Contribuyente {
  ruc: string;
  razonSocial?: string;
  estado?: string;
  tipoContribuyente?: string;
  regimen?: string;
  actividadEconomica?: string;
  obligadoContabilidad?: string;
  fechaInicioActividades?: string;
  fechaActualizacion?: string;
}

export interface Vehiculo {
  placa: string;
  marca?: string;
  modelo?: string;
  anio?: string;
  color?: string;
  clase?: string;
  servicio?: string;
  detalle: Record<string, string>;
}

export interface LicenciaPuntos {
  cedula: string;
  puntos: number;
  puntosMaximos: number;
}

export type Estado<T> =
  | { tipo: 'inactivo' }
  | { tipo: 'cargando' }
  | { tipo: 'ok'; respuesta: ApiResponse<T> }
  | { tipo: 'error'; codigo: string; mensaje: string };

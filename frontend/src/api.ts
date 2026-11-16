import type { ApiResponse, Contribuyente, LicenciaPuntos, Vehiculo } from './types';

export class ApiError extends Error {
  constructor(
    public status: number,
    public codigo: string,
    mensaje: string,
  ) {
    super(mensaje);
  }
}

async function get<T>(url: string): Promise<ApiResponse<T>> {
  let res: Response;
  try {
    res = await fetch(url, { headers: { Accept: 'application/json' } });
  } catch {
    throw new ApiError(0, 'SIN_CONEXION', 'No se pudo conectar con el servidor. Revise su conexión.');
  }
  const cuerpo = await res.json().catch(() => null);
  if (!res.ok) {
    const e = cuerpo?.error;
    throw new ApiError(res.status, e?.codigo ?? 'ERROR', e?.mensaje ?? `Error inesperado (${res.status}).`);
  }
  return cuerpo as ApiResponse<T>;
}

export const consultarContribuyente = (ruc: string, correo: string) =>
  get<Contribuyente>(`/api/v1/contribuyentes/${encodeURIComponent(ruc)}?correo=${encodeURIComponent(correo)}`);

export const consultarVehiculo = (placa: string) =>
  get<Vehiculo>(`/api/v1/vehiculos/${encodeURIComponent(placa)}`);

export const consultarPuntos = (cedula: string) =>
  get<LicenciaPuntos>(`/api/v1/licencias/${encodeURIComponent(cedula)}/puntos`);

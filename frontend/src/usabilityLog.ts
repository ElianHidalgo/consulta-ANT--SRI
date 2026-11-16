// Registro simple para la prueba de usabilidad: guarda eventos con tiempo y permite descargarlos en CSV.
interface Evento {
  t: number;
  evento: string;
  detalle: string;
}

let eventos: Evento[] = [];

export function registrar(evento: string, detalle = ''): void {
  eventos.push({ t: Date.now(), evento, detalle });
}

export function reiniciarRegistro(): void {
  eventos = [];
  registrar('inicio_prueba');
}

export function descargarCsv(): void {
  const t0 = eventos[0]?.t ?? Date.now();
  const filas = eventos.map(
    (e) => `${((e.t - t0) / 1000).toFixed(1)},${new Date(e.t).toISOString()},${e.evento},"${e.detalle.replace(/"/g, "'")}"`,
  );
  const csv = ['segundos,hora,evento,detalle', ...filas].join('\n');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = 'registro-prueba-usabilidad.csv';
  a.click();
  URL.revokeObjectURL(url);
}

registrar('inicio_prueba');

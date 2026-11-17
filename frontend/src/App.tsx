import { useState } from 'react';
import { ModuloAnt } from './components/ModuloAnt';
import { ModuloSri } from './components/ModuloSri';
import { descargarCsv, registrar, reiniciarRegistro } from './usabilityLog';

type Modulo = 'sri' | 'ant';

const PESTANAS: { id: Modulo; titulo: string; detalle: string }[] = [
  { id: 'sri', titulo: 'SRI', detalle: 'Contribuyente y vehículo' },
  { id: 'ant', titulo: 'ANT', detalle: 'Puntos de licencia' },
];

export default function App() {
  const [activo, setActivo] = useState<Modulo>('sri');
  // Cambiar la clave vuelve a montar los módulos y limpia sus consultas.
  const [sesion, setSesion] = useState(0);

  function cambiar(m: Modulo) {
    if (m === activo) return;
    setActivo(m);
    registrar('cambio_modulo', m);
  }

  function reiniciar() {
    setSesion((s) => s + 1);
    setActivo('sri');
    reiniciarRegistro();
  }

  return (
    <main>
      <header>
        <h1>Consulta de contribuyente, vehículo y licencia</h1>
        <p>Elija el módulo: el SRI verifica su RUC y su vehículo; la ANT muestra los puntos de su licencia.</p>
      </header>

      <div className="pestanas" role="tablist" aria-label="Módulos de consulta">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            id={`tab-${p.id}`}
            aria-selected={activo === p.id}
            aria-controls={`panel-${p.id}`}
            className={activo === p.id ? 'pestana activa' : 'pestana'}
            onClick={() => cambiar(p.id)}
          >
            <strong>{p.titulo}</strong>
            <span>{p.detalle}</span>
          </button>
        ))}
      </div>

      {/* Ambos módulos quedan montados para no perder lo consultado al cambiar de pestaña. */}
      <div role="tabpanel" id="panel-sri" aria-labelledby="tab-sri" hidden={activo !== 'sri'}>
        <ModuloSri key={`sri-${sesion}`} />
      </div>
      <div role="tabpanel" id="panel-ant" aria-labelledby="tab-ant" hidden={activo !== 'ant'}>
        <ModuloAnt key={`ant-${sesion}`} />
      </div>

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

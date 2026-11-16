import { FormEvent, useState } from 'react';

interface Props {
  cargando: boolean;
  onEnviar: (placa: string) => void;
}

export function FormPlaca({ cargando, onEnviar }: Props) {
  const [placa, setPlaca] = useState('');
  const [error, setError] = useState('');

  function enviar(e: FormEvent) {
    e.preventDefault();
    const limpia = placa.trim().toUpperCase().replace(/[-\s]/g, '');
    if (!/^[A-Z0-9]{5,17}$/.test(limpia)) {
      setError('Ingrese una placa válida, por ejemplo PCQ1234');
      return;
    }
    setError('');
    onEnviar(limpia);
  }

  return (
    <form onSubmit={enviar} noValidate>
      <h2>2. Su vehículo</h2>
      <label htmlFor="placa">Matrícula (placa, CAMV o CPN)</label>
      <input
        id="placa"
        value={placa}
        maxLength={20}
        placeholder="Ej. PCQ1234"
        onChange={(e) => setPlaca(e.target.value.toUpperCase())}
        aria-invalid={!!error}
        aria-describedby="placa-error"
      />
      {error && (
        <span id="placa-error" className="campo-error">
          {error}
        </span>
      )}
      <button type="submit" disabled={cargando}>
        {cargando ? 'Buscando…' : 'Buscar vehículo'}
      </button>
    </form>
  );
}

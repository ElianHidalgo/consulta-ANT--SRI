import { FormEvent, useState } from 'react';

interface Props {
  cargando: boolean;
  onEnviar: (correo: string, ruc: string) => void;
}

const EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export function FormIdentificacion({ cargando, onEnviar }: Props) {
  const [correo, setCorreo] = useState('');
  const [ruc, setRuc] = useState('');
  const [errores, setErrores] = useState<{ correo?: string; ruc?: string }>({});

  function enviar(e: FormEvent) {
    e.preventDefault();
    const nuevos: { correo?: string; ruc?: string } = {};
    if (!EMAIL.test(correo.trim())) nuevos.correo = 'Ingrese un correo válido, por ejemplo nombre@correo.com';
    if (!/^\d{13}$/.test(ruc.trim())) nuevos.ruc = 'El RUC debe tener 13 dígitos (solo números)';
    setErrores(nuevos);
    if (Object.keys(nuevos).length === 0) onEnviar(correo.trim(), ruc.trim());
  }

  return (
    <form onSubmit={enviar} noValidate>
      <h2>1. Sus datos</h2>
      <label htmlFor="correo">Correo electrónico</label>
      <input
        id="correo"
        type="email"
        autoComplete="email"
        value={correo}
        onChange={(e) => setCorreo(e.target.value)}
        aria-invalid={!!errores.correo}
        aria-describedby="correo-error"
      />
      {errores.correo && (
        <span id="correo-error" className="campo-error">
          {errores.correo}
        </span>
      )}

      <label htmlFor="ruc">RUC de persona natural</label>
      <input
        id="ruc"
        inputMode="numeric"
        maxLength={13}
        placeholder="Ej. 1713175071001"
        value={ruc}
        onChange={(e) => setRuc(e.target.value.replace(/\D/g, ''))}
        aria-invalid={!!errores.ruc}
        aria-describedby="ruc-error"
      />
      {errores.ruc && (
        <span id="ruc-error" className="campo-error">
          {errores.ruc}
        </span>
      )}

      <button type="submit" disabled={cargando}>
        {cargando ? 'Consultando…' : 'Consultar'}
      </button>
    </form>
  );
}

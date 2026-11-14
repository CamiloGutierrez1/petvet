import { useState } from "react";
import { obtenerMascota } from "../services/mascotasApi";

function MascotaDetalle() {
  const [id, setId] = useState("");
  const [mascota, setMascota] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function buscarMascota() {
    if (!id.trim()) {
      setError("Enter a pet ID.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setMascota(null);

      const data = await obtenerMascota(id);
      setMascota(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h2>Pet details</h2>

      <input
        value={id}
        onChange={(e) => setId(e.target.value)}
        placeholder="Enter pet ID"
      />

      <button onClick={buscarMascota} disabled={loading}>
        {loading ? "Loading..." : "Search"}
      </button>

      {error && <p>{error}</p>}

      {mascota && (
        <div>
          <h3>{mascota.nombre}</h3>
          <p>Species: {mascota.especie}</p>
          <p>Breed: {mascota.raza}</p>
          <p>Weight: {mascota.peso}</p>

          <h4>Owner information</h4>
          <p>Name: {mascota.duenoNombre}</p>
          <p>Email: {mascota.duenoEmail}</p>

          {mascota.fotoUrl && (
            <img
              src={mascota.fotoUrl}
              alt={mascota.nombre}
              width="180"
            />
          )}
        </div>
      )}
    </div>
  );
}

export default MascotaDetalle;

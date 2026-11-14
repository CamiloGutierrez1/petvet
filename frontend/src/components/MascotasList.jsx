import { useEffect, useState } from "react";
import { listarMascotas } from "../services/mascotasApi";

function MascotasList({ duenoId }) {

    const [mascotas, setMascotas] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function cargarMascotas() {
            try {
                setLoading(true);
                setError("");

                const data = await listarMascotas(duenoId);
                setMascotas(data);

            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        }

        cargarMascotas();
    }, [duenoId]);

    if (loading) return <p>Cargando mascotas...</p>;

    if (error) return <p>Error: {error}</p>;

    return (
        <div>
            <h2>Mascotas del dueño</h2>

            {mascotas.length === 0 ? (
                <p>Este dueño todavía no tiene mascotas.</p>
            ) : (
                mascotas.map(mascota => (
                    <div key={mascota.id}>
                        <h3>{mascota.nombre}</h3>
                        <p>Especie: {mascota.especie}</p>
                        <p>Raza: {mascota.raza}</p>
                    </div>
                ))
            )}
        </div>
    );
}

export default MascotasList;

const API_URL = import.meta.env.VITE_API_MASCOTAS_URL;

async function request(endpoint, options = {}) {
    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers: {
            ...(options.body ? {
                "Content-Type": "application/json"
            } : {}),
            ...options.headers
        }
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `Error HTTP: ${response.status}`);
    }

    if (response.status === 204) return null;

    const contentType = response.headers.get("content-type");

    return contentType?.includes("application/json")
        ? response.json()
        : null;
}

// 1. Listar las mascotas con los dueños
export function listarMascotas(duenoId) {
    return request(`/api/duenos/${duenoId}/mascotas`);
}

// 2. Traer un perfil completa de mascota
export function obtenerFichaMascota(mascotaId) {
    return request(`/api/mascotas/${mascotaId}`);
}

// 3. Registrar un mascota
export function registrarMascota(duenoId, datos) {
    return request(`/api/duenos/${duenoId}/mascotas`, {
        method: "POST",
        body: JSON.stringify(datos)
    });
}

// 4. Editar un mascota
export function actualizarMascota(mascotaId, datos) {
    return request(`/api/mascotas/${mascotaId}`, {
        method: "PUT",
        body: JSON.stringify(datos)
    });
}

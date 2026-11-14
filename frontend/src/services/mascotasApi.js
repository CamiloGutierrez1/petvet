const API_URL = import.meta.env.VITE_API_MASCOTAS_URL;

async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body instanceof FormData
        ? {}
        : { "Content-Type": "application/json" }),
      ...options.headers,
    },
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      data?.detalle || `Error HTTP: ${response.status}`
    );
  }

  return data;
}

export const mascotasApi = {
  listarDuenos() {
    return request("/api/duenos");
  },

  obtenerMascota(id) {
    return request(`/api/mascotas/${id}`);
  },

  registrarDueno(datos) {
    return request("/api/duenos", {
      method: "POST",
      body: JSON.stringify(datos),
    });
  },

  registrarMascota(duenoId, datos) {
    return request(`/api/duenos/${duenoId}/mascotas`, {
      method: "POST",
      body: JSON.stringify(datos),
    });
  },

  subirFoto(mascotaId, archivo) {
    const formData = new FormData();
    formData.append("foto", archivo);

    return request(`/api/mascotas/${mascotaId}/foto`, {
      method: "POST",
      body: formData,
    });
  },
};

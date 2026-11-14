import { apiRequest } from "./api";

const BASE_URL = import.meta.env.VITE_API_MASCOTAS_URL;

// Dueños
export const listarDuenos = () =>
  apiRequest(`${BASE_URL}/api/duenos`);

export const registrarDueno = (dueno) =>
  apiRequest(`${BASE_URL}/api/duenos`, {
    method: "POST",
    body: JSON.stringify(dueno),
  });

// Mascotas
export const listarMascotas = () =>
  apiRequest(`${BASE_URL}/api/mascotas`);

export const obtenerMascota = (id) =>
  apiRequest(`${BASE_URL}/api/mascotas/${id}`);

export const registrarMascota = (mascota) =>
  apiRequest(`${BASE_URL}/api/mascotas`, {
    method: "POST",
    body: JSON.stringify(mascota),
  });

export const actualizarMascota = (id, mascota) =>
  apiRequest(`${BASE_URL}/api/mascotas/${id}`, {
    method: "PUT",
    body: JSON.stringify(mascota),
  });

// Pet photo
export const subirFotoMascota = (id, archivo) => {
  const formData = new FormData();

  formData.append("foto", archivo);

  return apiRequest(`${BASE_URL}/api/mascotas/${id}/foto`, {
    method: "POST",
    body: formData,
  });
};

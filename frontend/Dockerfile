# Frontend de PetVet: se compila con Node y se sirve con nginx.

# ---------- Etapa 1: compilar ----------
FROM node:22-alpine AS build
WORKDIR /app

# Primero solo las dependencias, para aprovechar la caché de Docker.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite incrusta estas variables en el JavaScript al compilar. Son las URLs
# que usa el NAVEGADOR del usuario, por eso apuntan a localhost (los puertos
# que publica docker-compose), no a los nombres internos de los contenedores.
ARG VITE_API_MASCOTAS=http://localhost:8081/api
ARG VITE_API_CITAS=http://localhost:8082/api
ARG VITE_USE_MOCKS=false
ENV VITE_API_MASCOTAS=$VITE_API_MASCOTAS \
    VITE_API_CITAS=$VITE_API_CITAS \
    VITE_USE_MOCKS=$VITE_USE_MOCKS

RUN npm run build

# ---------- Etapa 2: servir ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
# 127.0.0.1 y no "localhost": en alpine localhost resuelve a IPv6 (::1) y
# nginx escucha en IPv4, así que la revisión fallaría.
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

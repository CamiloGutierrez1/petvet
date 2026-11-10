#!/usr/bin/env python3
"""Comprueba la base local después de levantar el perfil app de Docker Compose."""

import http.client
import json
import os
import smtplib
import sys
import time
import urllib.error
import urllib.request


# Cada servicio que se sume al perfil app se agrega aquí con su puerto.
SERVICIOS = {
    "ms-citas-historial": 8082,
}
ORIGEN = os.environ.get("PETVET_CORS_ALLOWED_ORIGINS", "http://localhost:5173").split(",")[0]


def consultar(url, headers=None, method="GET"):
    request = urllib.request.Request(url, headers=headers or {}, method=method)
    with urllib.request.urlopen(request, timeout=5) as response:
        return response.headers, response.read()


def esperar_salud(base):
    limite = time.monotonic() + 90
    ultimo_error = "El servicio no ha respondido"
    while time.monotonic() < limite:
        try:
            _, body = consultar(base + "/actuator/health")
            if json.loads(body).get("status") == "UP":
                return
            ultimo_error = body.decode()
        except (OSError, http.client.HTTPException, ValueError) as error:
            # Recién levantado, el puerto acepta conexiones y las cierra sin responder.
            ultimo_error = str(error)
        time.sleep(2)
    raise RuntimeError(f"{base} no está saludable. {ultimo_error}")


def verificar_servicio(nombre, puerto):
    base = f"http://localhost:{puerto}"
    esperar_salud(base)
    _, body = consultar(base + "/v3/api-docs")
    documento = json.loads(body)
    if not documento.get("openapi", "").startswith("3."):
        raise RuntimeError(f"{nombre} no devuelve OpenAPI 3")
    if "PetVet" not in documento.get("info", {}).get("title", ""):
        raise RuntimeError(f"{nombre} no contiene la documentación esperada")

    _, body = consultar(base + "/swagger-ui/index.html")
    if b"Swagger UI" not in body:
        raise RuntimeError(f"Swagger UI no carga en {nombre}")

    headers, _ = consultar(
        base + "/v3/api-docs",
        headers={
            "Origin": ORIGEN,
            "Access-Control-Request-Method": "GET",
            "Access-Control-Request-Headers": "Content-Type",
        },
        method="OPTIONS",
    )
    if headers.get("Access-Control-Allow-Origin") != ORIGEN:
        raise RuntimeError(f"El origen {ORIGEN} no está habilitado en {nombre}")
    print(f"OK {nombre} — salud, OpenAPI, Swagger UI y CORS", flush=True)


def main():
    for nombre, puerto in SERVICIOS.items():
        verificar_servicio(nombre, puerto)

    _, body = consultar("http://localhost:8025/api/v2/messages")
    if "total" not in json.loads(body):
        raise RuntimeError("La API de MailHog no devuelve el formato esperado")
    with smtplib.SMTP("localhost", 1025, timeout=5) as smtp:
        codigo, _ = smtp.noop()
        if codigo != 250:
            raise RuntimeError("El SMTP de MailHog no responde correctamente")
    print("OK MailHog — API y SMTP local", flush=True)
    print("Salud e infraestructura verificadas. Use probar_citas.py para comprobar la API de negocio en modo demo.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, smtplib.SMTPException) as error:
        print(f"FALLO {error}", file=sys.stderr)
        sys.exit(1)

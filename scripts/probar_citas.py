#!/usr/bin/env python3
"""Ejecuta el flujo de Citas con el servicio simulado y RabbitMQ locales."""

import argparse
import base64
import http.client
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone


CITAS = "http://localhost:8082"
RABBIT = "http://localhost:15672/api"
BOGOTA = timezone(timedelta(hours=-5))
MASCOTA_ID = 3
COLA_CITAS = "petvet.demo.citas"
COLA_VACUNAS = "petvet.demo.vacunas"


def solicitar(base, ruta, metodo="GET", datos=None, esperado=200, rabbit=False):
    headers = {"Accept": "application/json"}
    body = None
    if datos is not None:
        body = json.dumps(datos).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if rabbit:
        usuario = os.environ.get("PETVET_RABBITMQ_USER", "petvet")
        clave = os.environ.get("PETVET_RABBITMQ_PASSWORD", "petvet_local")
        token = base64.b64encode(f"{usuario}:{clave}".encode()).decode()
        headers["Authorization"] = f"Basic {token}"
    request = urllib.request.Request(base + ruta, data=body, method=metodo, headers=headers)
    try:
        response = urllib.request.urlopen(request, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        cuerpo = response.read()
        estados_esperados = (esperado,) if isinstance(esperado, int) else esperado
        if response.status not in estados_esperados:
            raise RuntimeError(
                f"{metodo} {ruta} devolvió {response.status}, se esperaba {esperado}. "
                + cuerpo.decode("utf-8", errors="replace")
            )
    return json.loads(cuerpo) if cuerpo else None


def esperar_citas(segundos=90):
    """Espera a que Citas termine de arrancar. Recién levantado, el puerto acepta
    conexiones y las cierra sin responder."""
    limite = time.monotonic() + segundos
    ultimo_error = "El servicio no ha respondido"
    while time.monotonic() < limite:
        try:
            salud = solicitar(CITAS, "/actuator/health", esperado=(200, 503)) or {}
            if salud.get("status") == "UP":
                return
            ultimo_error = f"Estado {salud.get('status')}"
        except (OSError, http.client.HTTPException, ValueError) as error:
            ultimo_error = str(error)
        time.sleep(2)
    raise RuntimeError(f"Citas e Historial no está saludable. {ultimo_error}")


def preparar_colas():
    for nombre, patron in ((COLA_CITAS, "cita.*"), (COLA_VACUNAS, "vacuna.*")):
        ruta = "/queues/%2F/" + urllib.parse.quote(nombre, safe="")
        # PUT es idempotente. RabbitMQ devuelve 201 cuando crea y 204 si ya existe.
        solicitar(RABBIT, ruta, "PUT", {"durable": True, "auto_delete": False,
                "arguments": {}}, esperado=(201, 204), rabbit=True)
        solicitar(RABBIT, "/bindings/%2F/e/petvet.events/q/" + nombre, "POST",
                {"routing_key": patron, "arguments": {}}, esperado=(201, 204), rabbit=True)


def buscar_turno(veterinarios):
    existentes = solicitar(CITAS, f"/api/citas?mascotaId={MASCOTA_ID}")
    ocupados = {
        datetime.fromisoformat(cita["fechaHora"])
        for cita in existentes if cita["estado"] != "CANCELADA"
    }
    hoy = datetime.now(BOGOTA).date()
    for dias in range(31):
        fecha = (hoy + timedelta(days=dias)).isoformat()
        for veterinario in veterinarios:
            disponibilidad = solicitar(
                CITAS, f"/api/veterinarios/{veterinario['id']}/disponibilidad?fecha={fecha}"
            )
            for horario in disponibilidad["horarios"]:
                if datetime.fromisoformat(horario) not in ocupados:
                    return veterinario["id"], horario
    raise RuntimeError("No hay un turno libre para la mascota demo en los próximos 31 días")


def esperar_eventos(cola, esperados, cita_id=None, vacuna=None, segundos=90):
    limite = time.monotonic() + segundos
    encontrados = set()
    while time.monotonic() < limite:
        mensajes = solicitar(RABBIT, f"/queues/%2F/{cola}/get", "POST",
                {"count": 1000, "ackmode": "ack_requeue_true", "encoding": "auto"}, rabbit=True)
        for mensaje in mensajes:
            contenido = json.loads(mensaje["payload"])
            coincide_cita = cita_id is None or contenido.get("citaId") == cita_id
            coincide_vacuna = vacuna is None or (
                contenido.get("mascotaId") == MASCOTA_ID
                and contenido.get("vacuna") == vacuna["vacuna"]
                and contenido.get("fechaLimite") == vacuna["fechaLimite"]
            )
            if coincide_cita and coincide_vacuna:
                encontrados.add(mensaje["routing_key"])
        if esperados.issubset(encontrados):
            return
        time.sleep(2)
    faltantes = ", ".join(sorted(esperados - encontrados))
    raise RuntimeError(f"No llegaron los eventos {faltantes} a {cola} en {segundos} segundos")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--esperar-vacuna", action="store_true",
            help="Comprueba también vacuna.proxima con el cron cada minuto del perfil demo")
    opciones = parser.parse_args()

    esperar_citas()
    preparar_colas()
    veterinarios = solicitar(CITAS, "/api/veterinarios")
    if not veterinarios:
        raise RuntimeError("Faltan los veterinarios de demostración")
    veterinario_id, horario = buscar_turno(veterinarios)
    print("OK veterinarios y disponibilidad", flush=True)

    nueva_cita = {"mascotaId": MASCOTA_ID, "veterinarioId": veterinario_id,
            "fechaHora": horario, "tipoServicio": "CONSULTA_GENERAL"}
    cita = solicitar(CITAS, "/api/citas", "POST", nueva_cita, esperado=201)
    solicitar(CITAS, "/api/citas", "POST", nueva_cita, esperado=409)
    print(f"OK cita {cita['id']} creada y reserva duplicada rechazada con 409", flush=True)

    consulta = {"tipoConsulta": "GENERAL", "diagnostico": "Consulta de demostración sintética",
            "vacunasAplicadas": [], "tratamientos": [], "examenes": [],
            "observaciones": "Prueba local automática de la parte de Jersson"}
    registro = solicitar(CITAS, f"/api/citas/{cita['id']}/consulta", "POST", consulta, esperado=201)
    historial = solicitar(CITAS, f"/api/mascotas/{MASCOTA_ID}/historial")
    if not any(item["id"] == registro["id"] for item in historial):
        raise RuntimeError("La consulta creada no aparece en el historial")
    vacunas = solicitar(CITAS, f"/api/mascotas/{MASCOTA_ID}/proximas-vacunas")
    print("OK consulta, historial y próximas vacunas", flush=True)

    esperar_eventos(COLA_CITAS, {"cita.agendada", "cita.completada"}, cita_id=cita["id"])
    print("OK eventos cita.agendada y cita.completada recibidos en RabbitMQ", flush=True)
    if opciones.esperar_vacuna:
        hoy = datetime.now(BOGOTA).date()
        proximas = [vacuna for vacuna in vacunas
                if 0 <= (datetime.fromisoformat(vacuna["fechaLimite"]).date() - hoy).days <= 7]
        if not proximas:
            raise RuntimeError("La mascota demo no tiene una vacuna pendiente en los próximos siete días")
        esperar_eventos(COLA_VACUNAS, {"vacuna.proxima"}, vacuna=proximas[0], segundos=90)
        print("OK vacuna.proxima recibida por la tarea programada", flush=True)
    print("Flujo de Citas verificado con datos sintéticos. Los mensajes quedan en las colas demo.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, KeyError) as error:
        print(f"FALLO {error}", file=sys.stderr)
        sys.exit(1)

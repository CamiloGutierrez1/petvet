#!/usr/bin/env python3
"""Recorre el flujo de PetVet con los servicios reales levantados en Docker Compose: Mascotas y
Dueños, Citas e Historial, Notificaciones y la bandeja de MailHog."""

import argparse
import base64
import email
import email.policy
import http.client
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone


MASCOTAS = "http://localhost:8081"
CITAS = "http://localhost:8082"
NOTIFICACIONES = "http://localhost:8083"
MAILHOG = "http://localhost:8025"
RABBIT = "http://localhost:15672/api"
BOGOTA = timezone(timedelta(hours=-5))

NOMBRE_DUENO = "Mariana Peña"
# Con 58 días, la primera polivalente del cachorro vence a los dos meses, entre uno y cuatro días
# después de hoy: dentro de la ventana de siete días de la revisión de vacunas de Citas.
DIAS_DE_NACIDO = 58
VACUNA_RECORDATORIO = "Polivalente"
EXAMENES = ["Hemograma", "Coprológico"]
CIRUGIAS = ["Corrección de hernia umbilical"]
ESPERA_CORREOS = 90
# Inicio de los asuntos que arma Notificaciones, en minúsculas.
CONFIRMACION = "confirmación de la cita"
RESUMEN = "resumen de la consulta"
RECORDATORIO = "recordatorio: vacuna"
COLAS_NOTIFICACIONES = ("notificaciones.citas", "notificaciones.vacunas")
# Colas que creaban las versiones anteriores de este script. Enlazadas a cita.* y vacuna.* y sin
# consumidor, guardan una copia de cada evento y hacen que Citas dé por entregado un evento aunque
# Notificaciones no tenga sus colas. Si quedan, se borran.
COLAS_VIEJAS = ("petvet.demo.citas", "petvet.demo.vacunas")


def solicitar_con_estado(base, ruta, metodo="GET", datos=None, esperado=200, rabbit=False):
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
            pista = ""
            if rabbit and response.status == 401:
                pista = "Revisa PETVET_RABBITMQ_USER y PETVET_RABBITMQ_PASSWORD. "
            raise RuntimeError(
                f"{metodo} {ruta} devolvió {response.status}, se esperaba {esperado}. {pista}"
                + cuerpo.decode("utf-8", errors="replace")
            )
        return response.status, (json.loads(cuerpo) if cuerpo else None)


def solicitar(base, ruta, metodo="GET", datos=None, esperado=200, rabbit=False):
    return solicitar_con_estado(base, ruta, metodo, datos, esperado, rabbit)[1]


def esperar_salud(nombre, base, segundos=90):
    """Espera a que el servicio termine de arrancar. Recién levantado, el puerto acepta
    conexiones y las cierra sin responder."""
    limite = time.monotonic() + segundos
    ultimo_error = "El servicio no ha respondido"
    while time.monotonic() < limite:
        try:
            salud = solicitar(base, "/actuator/health", esperado=(200, 503)) or {}
            if salud.get("status") == "UP":
                return
            ultimo_error = f"Estado {salud.get('status')}"
        except (OSError, http.client.HTTPException, ValueError) as error:
            ultimo_error = str(error)
        time.sleep(2)
    raise RuntimeError(f"{nombre} no está saludable en {base}. {ultimo_error}")


def esperar_mailhog(segundos=30):
    limite = time.monotonic() + segundos
    ultimo_error = "MailHog no ha respondido"
    while time.monotonic() < limite:
        try:
            bandeja = solicitar(MAILHOG, "/api/v2/messages?limit=1")
            if isinstance(bandeja, dict) and "total" in bandeja:
                return
            ultimo_error = "La API de MailHog no devuelve el formato esperado"
        except (OSError, http.client.HTTPException, ValueError) as error:
            ultimo_error = str(error)
        time.sleep(2)
    raise RuntimeError(f"MailHog no responde en {MAILHOG}. {ultimo_error}")


def ruta_cola(nombre):
    return "/queues/%2F/" + urllib.parse.quote(nombre, safe="")


def revisar_colas():
    """Borra las colas de prueba de corridas viejas y comprueba que Notificaciones declaró las suyas."""
    for nombre in COLAS_VIEJAS:
        estado, cola = solicitar_con_estado(RABBIT, ruta_cola(nombre), esperado=(200, 404), rabbit=True)
        if estado == 200:
            solicitar(RABBIT, ruta_cola(nombre), "DELETE", esperado=(204, 404), rabbit=True)
            mensajes = (cola or {}).get("messages") or 0
            print(f"AVISO Se borró la cola {nombre}, que dejó una versión anterior de este script con "
                  f"{mensajes} {'mensaje' if mensajes == 1 else 'mensajes'} sin consumidor. Mientras existía, "
                  "Citas daba por entregados los eventos aunque Notificaciones no tuviera sus colas.", flush=True)
    for nombre in COLAS_NOTIFICACIONES:
        estado, _ = solicitar_con_estado(RABBIT, ruta_cola(nombre), esperado=(200, 404), rabbit=True)
        if estado == 404:
            raise RuntimeError(f"No existe la cola {nombre}. Notificaciones la declara al arrancar: "
                               "revisa docker compose logs ms-notificaciones.")


def registrar_dueno_y_cachorro(hoy):
    """Un dueño con un correo nuevo en cada corrida, para encontrar sus correos en MailHog."""
    sufijo = uuid.uuid4().hex[:8]
    dueno = solicitar(MASCOTAS, "/api/duenos", "POST", {
        "nombre": NOMBRE_DUENO,
        "documento": str(int(sufijo, 16)),
        "email": f"demo.citas.{sufijo}@example.test",
        "telefono": "3001234567",
        "direccion": "Calle 10 # 20-30, Bogotá",
    }, esperado=201)
    mascota = solicitar(MASCOTAS, f"/api/duenos/{dueno['id']}/mascotas", "POST", {
        "nombre": "Max",
        "especie": "PERRO",
        "raza": "Beagle",
        "fechaNacimiento": (hoy - timedelta(days=DIAS_DE_NACIDO)).isoformat(),
        "peso": 4.2,
    }, esperado=201)
    ficha = solicitar(MASCOTAS, f"/api/mascotas/{mascota['id']}")
    if ficha.get("duenoNombre") != dueno["nombre"] or ficha.get("duenoEmail") != dueno["email"]:
        raise RuntimeError(f"La ficha de la mascota {mascota['id']} no trae el nombre y el correo de su dueño")
    return dueno, mascota


def primer_turno_libre(veterinarios, hoy):
    """El turno más temprano de mañana entre todos los veterinarios, o del primer día siguiente con
    turnos. Un turno de mañana no vence mientras corre el script."""
    for dias in range(1, 31):
        fecha = (hoy + timedelta(days=dias)).isoformat()
        turnos = []
        for veterinario in veterinarios:
            disponibilidad = solicitar(
                CITAS, f"/api/veterinarios/{veterinario['id']}/disponibilidad?fecha={fecha}"
            )
            if disponibilidad["horarios"]:
                turnos.append((disponibilidad["horarios"][0], veterinario["id"], veterinario["nombre"]))
        if turnos:
            return min(turnos)
    raise RuntimeError("No hay turnos libres en los próximos 30 días")


def correos_de(destino):
    """Asunto y cuerpo, ya decodificados, de los correos que MailHog recibió para destino."""
    consulta = urllib.parse.urlencode({"kind": "to", "query": destino, "limit": 50})
    bandeja = solicitar(MAILHOG, "/api/v2/search?" + consulta) or {}
    correos = []
    for item in bandeja.get("items") or []:
        crudo = (item.get("Raw") or {}).get("Data")
        if not crudo:
            raise RuntimeError("MailHog devolvió un correo sin su contenido original (Raw.Data)")
        # El asunto llega codificado (RFC 2047) y el cuerpo en quoted-printable o base64.
        mensaje = email.message_from_string(crudo, policy=email.policy.default)
        cuerpo = mensaje.get_body(preferencelist=("plain",))
        correos.append((str(mensaje.get("Subject", "")), cuerpo.get_content() if cuerpo else ""))
    return correos


def esperar_correos(destino, asuntos, segundos):
    """Espera hasta que llegue un correo por cada inicio de asunto. Devuelve {inicio: (asunto, cuerpo)}
    con los que llegaron en el plazo."""
    limite = time.monotonic() + segundos
    encontrados = {}
    while True:
        for asunto, cuerpo in correos_de(destino):
            for inicio in asuntos:
                if asunto.lower().startswith(inicio):
                    encontrados[inicio] = (asunto, cuerpo)
        if set(asuntos) <= set(encontrados) or time.monotonic() >= limite:
            return encontrados
        time.sleep(2)


def exigir_saludo(asunto, cuerpo):
    if NOMBRE_DUENO not in cuerpo:
        raise RuntimeError(f"El correo «{asunto}» no saluda a {NOMBRE_DUENO} por su nombre")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--esperar-vacuna", action="store_true",
            help="Falla si el recordatorio de vacuna no llega a MailHog. Sin esta opción solo se avisa.")
    opciones = parser.parse_args()

    for nombre, base in (("Mascotas y Dueños", MASCOTAS), ("Citas e Historial", CITAS),
                         ("Notificaciones", NOTIFICACIONES)):
        esperar_salud(nombre, base)
    esperar_mailhog()
    print("OK salud de Mascotas y Dueños, Citas e Historial, Notificaciones y MailHog", flush=True)
    revisar_colas()
    print("OK colas de Notificaciones en RabbitMQ, sin colas de prueba", flush=True)

    hoy = datetime.now(BOGOTA).date()
    dueno, mascota = registrar_dueno_y_cachorro(hoy)
    print(f"OK dueño {dueno['id']} ({dueno['email']}) y cachorro {mascota['id']} registrados en Mascotas y Dueños",
          flush=True)

    veterinarios = solicitar(CITAS, "/api/veterinarios")
    if not veterinarios:
        raise RuntimeError("Citas no tiene veterinarios")
    horario, veterinario_id, veterinario_nombre = primer_turno_libre(veterinarios, hoy)
    nueva_cita = {"mascotaId": mascota["id"], "veterinarioId": veterinario_id,
            "fechaHora": horario, "tipoServicio": "CONSULTA_GENERAL"}
    cita = solicitar(CITAS, "/api/citas", "POST", nueva_cita, esperado=201)
    solicitar(CITAS, "/api/citas", "POST", nueva_cita, esperado=409)
    agendada = solicitar(CITAS, f"/api/citas/{cita['id']}")
    if agendada["estado"] != "AGENDADA" or agendada["mascotaId"] != mascota["id"]:
        raise RuntimeError(f"La cita {cita['id']} no quedó agendada para la mascota {mascota['id']}")
    print(f"OK cita {cita['id']} con {veterinario_nombre} el {horario}, turno duplicado rechazado con 409",
          flush=True)

    consulta = {"tipoConsulta": "GENERAL", "diagnostico": "Hernia umbilical pequeña en cachorro sano",
            "vacunasAplicadas": [], "tratamientos": ["Desparasitación interna"],
            "examenes": EXAMENES, "cirugias": CIRUGIAS,
            "observaciones": "Datos sintéticos de scripts/probar_citas.py",
            "proximoControl": (hoy + timedelta(days=14)).isoformat()}
    registro = solicitar(CITAS, f"/api/citas/{cita['id']}/consulta", "POST", consulta, esperado=201)
    if registro["examenes"] != EXAMENES or registro["cirugias"] != CIRUGIAS:
        raise RuntimeError("La consulta no guardó los exámenes y la cirugía enviados")
    if solicitar(CITAS, f"/api/citas/{cita['id']}")["estado"] != "COMPLETADA":
        raise RuntimeError(f"La cita {cita['id']} no quedó completada después de la consulta")
    historial = solicitar(CITAS, f"/api/mascotas/{mascota['id']}/historial")
    if not any(item["id"] == registro["id"] and item["cirugias"] == CIRUGIAS for item in historial):
        raise RuntimeError("La consulta registrada no aparece con su cirugía en el historial")
    vacunas = solicitar(CITAS, f"/api/mascotas/{mascota['id']}/proximas-vacunas")
    polivalente = next((vacuna for vacuna in vacunas if vacuna["vacuna"] == VACUNA_RECORDATORIO), None)
    if polivalente is None:
        raise RuntimeError(f"Citas no calculó la {VACUNA_RECORDATORIO} pendiente del cachorro")
    print(f"OK consulta con exámenes y cirugía, historial y próximas vacunas "
          f"({VACUNA_RECORDATORIO} pendiente hasta el {polivalente['fechaLimite']})", flush=True)

    correos = esperar_correos(dueno["email"], (CONFIRMACION, RESUMEN), ESPERA_CORREOS)
    faltantes = [inicio for inicio in (CONFIRMACION, RESUMEN) if inicio not in correos]
    if faltantes:
        raise RuntimeError(f"En {ESPERA_CORREOS} segundos no llegaron a MailHog los correos de "
                           f"{' y '.join(faltantes)} para {dueno['email']}. Revisa docker compose logs "
                           "ms-notificaciones y ms-citas-historial.")
    for asunto, cuerpo in correos.values():
        exigir_saludo(asunto, cuerpo)
    asunto, cuerpo = correos[RESUMEN]
    if not all(examen in cuerpo for examen in EXAMENES):
        raise RuntimeError(f"El correo «{asunto}» no lista los exámenes de la consulta")
    print("OK confirmación de la cita y resumen de la consulta en MailHog, con el nombre del dueño", flush=True)

    recordatorio = esperar_correos(dueno["email"], (RECORDATORIO,), ESPERA_CORREOS).get(RECORDATORIO)
    if recordatorio is None:
        mensaje = (f"En {ESPERA_CORREOS} segundos no llegó el recordatorio de la vacuna. La revisión de vacunas "
                   "corre cada minuto solo con docker-compose.demo.yml; con el arranque principal corre a las 8:00.")
        if opciones.esperar_vacuna:
            raise RuntimeError(mensaje)
        print(f"AVISO {mensaje}", flush=True)
    else:
        asunto, cuerpo = recordatorio
        if VACUNA_RECORDATORIO not in asunto:
            raise RuntimeError(f"Llegó un recordatorio inesperado: «{asunto}»")
        exigir_saludo(asunto, cuerpo)
        print(f"OK recordatorio de la vacuna {VACUNA_RECORDATORIO} en MailHog", flush=True)

    print(f"Flujo de PetVet verificado con datos sintéticos: mascota {mascota['id']}, cita {cita['id']}. "
          f"Los correos para {dueno['email']} están en http://localhost:8025.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, KeyError, http.client.HTTPException) as error:
        print(f"FALLO {error}", file=sys.stderr)
        sys.exit(1)

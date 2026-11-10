#!/usr/bin/env python3
"""Servicio simulado de Mascotas y Dueños para la demostración local de Citas."""

import calendar
import json
from datetime import date, datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse


BOGOTA = timezone(timedelta(hours=-5))


def mover_meses(fecha, meses):
    total = fecha.year * 12 + fecha.month - 1 + meses
    anio, mes = divmod(total, 12)
    mes += 1
    dia = min(fecha.day, calendar.monthrange(anio, mes)[1])
    return date(anio, mes, dia)


def nacimiento_cachorro(hoy):
    # Garantiza que el hito nacimiento + dos meses caiga en los próximos tres días.
    for dias in (3, 2, 1):
        vencimiento = hoy + timedelta(days=dias)
        nacimiento = mover_meses(vencimiento, -2)
        if mover_meses(nacimiento, 2) == vencimiento:
            return nacimiento
    raise RuntimeError("No se pudo construir la fecha de la mascota de demostración")


def mascotas_demo():
    hoy = datetime.now(BOGOTA).date()
    return {
        1: {
            "id": 1,
            "nombre": "Luna Demo",
            "especie": "GATO",
            "fechaNacimiento": mover_meses(hoy, -36).isoformat(),
            "duenoNombre": "Dueño Demo 1",
            "duenoEmail": "dueno1@example.test",
        },
        2: {
            "id": 2,
            "nombre": "Bolita Demo",
            "especie": "PERRO",
            "fechaNacimiento": mover_meses(hoy, -120).isoformat(),
            "duenoNombre": "Dueño Demo 2",
            "duenoEmail": "dueno2@example.test",
        },
        3: {
            "id": 3,
            "nombre": "Tito Demo",
            "especie": "PERRO",
            "fechaNacimiento": nacimiento_cachorro(hoy).isoformat(),
            "duenoNombre": "Dueño Demo 3",
            "duenoEmail": "dueno3@example.test",
        },
    }


class MascotasDemoHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        ruta = urlparse(self.path).path
        if ruta == "/actuator/health":
            self.responder(200, {"status": "UP"})
            return
        partes = ruta.strip("/").split("/")
        if len(partes) == 3 and partes[:2] == ["api", "mascotas"] and partes[2].isdigit():
            mascota = mascotas_demo().get(int(partes[2]))
            if mascota is not None:
                self.responder(200, mascota)
                return
        self.responder(404, {"error": "No encontrado", "detalle": "No existe esa mascota de demo"})

    def responder(self, estado, contenido):
        cuerpo = json.dumps(contenido, ensure_ascii=False).encode("utf-8")
        self.send_response(estado)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)


if __name__ == "__main__":
    print("Mascotas simuladas disponibles en el puerto 8080", flush=True)
    ThreadingHTTPServer(("0.0.0.0", 8080), MascotasDemoHandler).serve_forever()

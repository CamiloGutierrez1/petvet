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


LAURA = ("Laura Méndez", "laura.mendez@correo.com")
SANTIAGO = ("Santiago Rojas", "santiago.rojas@correo.com")


def ficha(id_mascota, nombre, especie, nacimiento, dueno):
    return {
        "id": id_mascota,
        "nombre": nombre,
        "especie": especie,
        "fechaNacimiento": nacimiento.isoformat(),
        "duenoNombre": dueno[0],
        "duenoEmail": dueno[1],
    }


def mascotas_demo():
    # 1, 2 y 3 son las mascotas de frontend/src/mocks/mascotas.json. Su historial de ejemplo está en
    # ms-citas-historial/src/main/resources/db/demo/R__historial_demo.sql.
    # 4 es un cachorro cuya primera vacuna vence en los próximos días, para mostrar vacuna.proxima.
    hoy = datetime.now(BOGOTA).date()
    fichas = (
        ficha(1, "Tobby", "PERRO", date(2021, 4, 18), LAURA),
        ficha(2, "Misha", "GATO", date(2023, 9, 2), LAURA),
        ficha(3, "Kiwi", "AVE", date(2024, 2, 11), SANTIAGO),
        ficha(4, "Max", "PERRO", nacimiento_cachorro(hoy), SANTIAGO),
    )
    return {mascota["id"]: mascota for mascota in fichas}


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

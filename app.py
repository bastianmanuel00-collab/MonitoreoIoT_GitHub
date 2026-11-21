"""Proxy WSGI de lectura. Servir detrás de un gateway HTTPS con acceso restringido."""

import hmac
import json
import os
import re
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import HTTPRedirectHandler, Request, build_opener


class SinRedirecciones(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def cargar_canales():
    canales = {}
    for tipo in ("SENSOR", "ACTUATOR", "LEGACY"):
        canal = os.environ.get(f"{tipo}_CHANNEL_ID", "")
        clave = os.environ.get(f"{tipo}_READ_API_KEY", "")
        if not clave and (tipo == "LEGACY" or not canal):
            continue
        if not re.fullmatch(r"[1-9][0-9]*", canal) or not clave:
            raise ValueError(f"Configuración incompleta del canal {tipo}")
        if canal in canales and canales[canal] != clave:
            raise ValueError("El mismo ID tiene dos claves distintas")
        canales[canal] = clave
    if not canales:
        raise ValueError("Configura al menos un canal")
    return canales


CANALES = cargar_canales()
TOKEN = os.environ.get("PROXY_ACCESS_TOKEN", "").encode("ascii")
if not re.fullmatch(rb"[A-Za-z0-9_-]{32,128}", TOKEN):
    raise ValueError("PROXY_ACCESS_TOKEN debe contener entre 32 y 128 caracteres URL seguros")
RUTA = re.compile(r"/channels/([1-9][0-9]*)/feeds(?:/(last))?\.json")
HTTP = build_opener(SinRedirecciones())
MAX_BYTES = 1024 * 1024


def respuesta(start_response, estado, contenido):
    start_response(estado, [
        ("Content-Type", "application/json; charset=utf-8"),
        ("Cache-Control", "no-store"),
        ("X-Content-Type-Options", "nosniff"),
        ("Content-Length", str(len(contenido))),
    ])
    return [contenido]


def application(environ, start_response):
    if environ["REQUEST_METHOD"] != "GET":
        return respuesta(start_response, "405 Method Not Allowed", b'{"error":"method"}')
    path = environ.get("PATH_INFO", "")
    if path == "/healthz":
        return respuesta(start_response, "200 OK", b'{"status":"ok"}')
    authorization = environ.get("HTTP_AUTHORIZATION", "").encode("latin-1")
    if not hmac.compare_digest(authorization, b"Bearer " + TOKEN):
        return respuesta(start_response, "401 Unauthorized", b'{"error":"unauthorized"}')
    ruta = RUTA.fullmatch(path)
    if not ruta or ruta[1] not in CANALES:
        return respuesta(start_response, "404 Not Found", b'{"error":"channel"}')
    parametros = {"api_key": CANALES[ruta[1]]}
    if ruta[2] is None:
        if environ.get("QUERY_STRING") != "results=100":
            return respuesta(start_response, "400 Bad Request", b'{"error":"query"}')
        parametros["results"] = "100"
    url = f"https://api.thingspeak.com{ruta[0]}?{urlencode(parametros)}"
    try:
        request = Request(url, headers={"Accept": "application/json"})
        with HTTP.open(request, timeout=5) as remoto:
            datos = remoto.read(MAX_BYTES + 1)
        if len(datos) > MAX_BYTES or not isinstance(json.loads(datos), dict):
            raise ValueError("Respuesta inválida")
    except (HTTPError, URLError, OSError, ValueError, UnicodeError):
        return respuesta(start_response, "502 Bad Gateway", b'{"error":"upstream"}')
    return respuesta(start_response, "200 OK", datos)

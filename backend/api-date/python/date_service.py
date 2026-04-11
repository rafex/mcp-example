from __future__ import annotations

from datetime import datetime, timezone
from zoneinfo import ZoneInfo


DATE_LOCATIONS = {
    "us": ("United States", "America/New_York"),
    "ca": ("Canada", "America/Toronto"),
    "br": ("Brazil", "America/Sao_Paulo"),
    "ar": ("Argentina", "America/Argentina/Buenos_Aires"),
    "cl": ("Chile", "America/Santiago"),
    "co": ("Colombia", "America/Bogota"),
    "pe": ("Peru", "America/Lima"),
    "gb": ("United Kingdom", "Europe/London"),
    "ie": ("Ireland", "Europe/Dublin"),
    "fr": ("France", "Europe/Paris"),
    "de": ("Germany", "Europe/Berlin"),
    "es": ("Spain", "Europe/Madrid"),
    "it": ("Italy", "Europe/Rome"),
    "nl": ("Netherlands", "Europe/Amsterdam"),
    "be": ("Belgium", "Europe/Brussels"),
    "ch": ("Switzerland", "Europe/Zurich"),
    "pt": ("Portugal", "Europe/Lisbon"),
    "se": ("Sweden", "Europe/Stockholm"),
    "no": ("Norway", "Europe/Oslo"),
    "fi": ("Finland", "Europe/Helsinki"),
    "pl": ("Poland", "Europe/Warsaw"),
    "ua": ("Ukraine", "Europe/Kyiv"),
    "tr": ("Turkey", "Europe/Istanbul"),
    "ru": ("Russia", "Europe/Moscow"),
    "sa": ("Saudi Arabia", "Asia/Riyadh"),
    "ae": ("United Arab Emirates", "Asia/Dubai"),
    "eg": ("Egypt", "Africa/Cairo"),
    "za": ("South Africa", "Africa/Johannesburg"),
    "ng": ("Nigeria", "Africa/Lagos"),
    "ke": ("Kenya", "Africa/Nairobi"),
    "in": ("India", "Asia/Kolkata"),
    "pk": ("Pakistan", "Asia/Karachi"),
    "bd": ("Bangladesh", "Asia/Dhaka"),
    "cn": ("China", "Asia/Shanghai"),
    "jp": ("Japan", "Asia/Tokyo"),
    "kr": ("South Korea", "Asia/Seoul"),
    "sg": ("Singapore", "Asia/Singapore"),
    "id": ("Indonesia", "Asia/Jakarta"),
    "th": ("Thailand", "Asia/Bangkok"),
    "vn": ("Vietnam", "Asia/Ho_Chi_Minh"),
    "ph": ("Philippines", "Asia/Manila"),
    "my": ("Malaysia", "Asia/Kuala_Lumpur"),
    "au": ("Australia", "Australia/Sydney"),
    "nz": ("New Zealand", "Pacific/Auckland"),
    "il": ("Israel", "Asia/Jerusalem"),
    "ir": ("Iran", "Asia/Tehran"),
    "ma": ("Morocco", "Africa/Casablanca"),
    "dz": ("Algeria", "Africa/Algiers"),
    "et": ("Ethiopia", "Africa/Addis_Ababa"),
    "gh": ("Ghana", "Africa/Accra"),
    "mx-central": ("Mexico Central", "America/Mexico_City"),
    "mx-northwest": ("Mexico Northwest", "America/Tijuana"),
    "mx-southeast": ("Mexico Southeast", "America/Cancun"),
}


def build_locations_payload() -> dict[str, object]:
    locations = [
        {
            "location": code,
            "label": label,
            "timezone": timezone_name,
        }
        for code, (label, timezone_name) in sorted(DATE_LOCATIONS.items())
    ]
    return {
        "location_count": len(locations),
        "locations": locations,
    }


def build_time_payload(location: str, client_ip: str) -> dict[str, object]:
    normalized = normalize_location(location)
    label, timezone_name = DATE_LOCATIONS[normalized]
    local_now = datetime.now(ZoneInfo(timezone_name))
    utc_now = datetime.now(timezone.utc)
    return {
        "location": normalized,
        "label": label,
        "timezone": timezone_name,
        "local_time": local_now.strftime("%Y-%m-%d %H:%M:%S"),
        "utc_offset": local_now.strftime("%z"),
        "utc_time": utc_now.strftime("%Y-%m-%d %H:%M:%S UTC+00:00"),
        "ip": client_ip,
    }


def build_resources_payload() -> dict[str, object]:
    return {
        "resources": [
            {
                "uri": "date://auth-reference",
                "name": "auth-reference",
                "description": "Resumen de autenticación requerida por el servicio date.",
                "mimeType": "text/plain",
            },
            {
                "uri": "date://location-reference",
                "name": "location-reference",
                "description": "Listado de ubicaciones soportadas y sus zonas horarias.",
                "mimeType": "text/plain",
            },
        ]
    }


def build_resource_contents_payload(resource_uri: str) -> dict[str, object]:
    if resource_uri == "date://auth-reference":
        text = "\n".join(
            [
                "Date API authentication reference",
                "- Header Authorization: Bearer <token>",
                "- Header X-Date-Client: <client-id>",
                "- Endpoints require authentication for GET requests",
            ]
        )
    elif resource_uri == "date://location-reference":
        text = "\n".join(
            f"- {code}: {label} ({timezone_name})"
            for code, (label, timezone_name) in sorted(DATE_LOCATIONS.items())
        )
    else:
        raise KeyError(resource_uri)

    return {
        "contents": [
            {
                "uri": resource_uri,
                "mimeType": "text/plain",
                "text": text,
            }
        ]
    }


def build_prompts_payload() -> dict[str, object]:
    return {
        "prompts": [
            {
                "name": "single-location-time",
                "description": "Pide la hora actual de una ubicación concreta usando get_current_time.",
                "arguments": [
                    {"name": "location", "description": "Código de ubicación soportado.", "required": True},
                ],
            },
            {
                "name": "compare-locations",
                "description": "Pide comparar la hora actual entre dos ubicaciones.",
                "arguments": [
                    {"name": "from_location", "description": "Primera ubicación.", "required": True},
                    {"name": "to_location", "description": "Segunda ubicación.", "required": True},
                ],
            },
        ]
    }


def build_prompt_details_payload(prompt_name: str, from_location: str | None = None, to_location: str | None = None) -> dict[str, object]:
    if prompt_name == "single-location-time":
        resolved_location = normalize_location(from_location or "mx-central")
        return {
            "description": "Prompt para obtener la hora de una sola ubicación.",
            "messages": [
                {
                    "role": "system",
                    "content": {
                        "type": "text",
                        "text": "Usa la herramienta get_current_time y responde con la hora obtenida.",
                    },
                },
                {
                    "role": "user",
                    "content": {
                        "type": "text",
                        "text": f"Dime la hora actual para {resolved_location} usando get_current_time.",
                    },
                },
            ],
        }

    if prompt_name == "compare-locations":
        resolved_from = normalize_location(from_location or "mx-central")
        resolved_to = normalize_location(to_location or "us")
        return {
            "description": "Prompt para comparar dos ubicaciones usando get_current_time.",
            "messages": [
                {
                    "role": "system",
                    "content": {
                        "type": "text",
                        "text": "Usa get_current_time para ambas ubicaciones y compara los resultados.",
                    },
                },
                {
                    "role": "user",
                    "content": {
                        "type": "text",
                        "text": (
                            f"Compara la hora actual entre {resolved_from} y {resolved_to} "
                            "usando get_current_time para cada ubicación."
                        ),
                    },
                },
            ],
        }

    raise KeyError(prompt_name)


def normalize_location(location: str | None) -> str:
    if not location:
        return "mx-central"
    normalized = location.strip().lower()
    if normalized in DATE_LOCATIONS:
        return normalized
    raise KeyError(location)

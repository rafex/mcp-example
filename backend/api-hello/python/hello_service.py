from __future__ import annotations

from datetime import datetime, timezone


GREETINGS = {
    "en": "Hello",
    "zh": "Ni hao",
    "hi": "Namaste",
    "es": "Hola",
    "fr": "Bonjour",
    "ar": "Marhaban",
    "bn": "Nomoskar",
    "pt": "Ola",
    "ru": "Privet",
    "ur": "Assalam o Alaikum",
}

DEFAULT_LANG = "en"


def build_languages_payload() -> dict[str, object]:
    languages = sorted(GREETINGS.keys())
    return {
        "language_count": len(languages),
        "languages": languages,
    }


def build_resources_payload() -> dict[str, object]:
    return {
        "resources": [
            {
                "uri": "hello://service-overview",
                "name": "service-overview",
                "description": "Resumen de los endpoints REST disponibles del ejemplo hello.",
                "mimeType": "text/plain",
            },
            {
                "uri": "hello://language-reference",
                "name": "language-reference",
                "description": "Referencia de idiomas soportados y su saludo base.",
                "mimeType": "text/plain",
            },
        ]
    }


def build_resource_contents_payload(resource_uri: str) -> dict[str, object]:
    if resource_uri == "hello://service-overview":
        text = "\n".join(
            [
                "Hello API service overview",
                "- GET /hello",
                "- POST /hello",
                "- OPTIONS /hello",
                "- GET /hello/languages",
                "- GET /hello/resources",
                "- GET /hello/prompts",
            ]
        )
        return {
            "contents": [
                {
                    "uri": resource_uri,
                    "mimeType": "text/plain",
                    "text": text,
                }
            ]
        }

    if resource_uri == "hello://language-reference":
        lines = [f"- {code}: {greeting}" for code, greeting in sorted(GREETINGS.items())]
        return {
            "contents": [
                {
                    "uri": resource_uri,
                    "mimeType": "text/plain",
                    "text": "\n".join(lines),
                }
            ]
        }

    raise KeyError(resource_uri)


def build_prompts_payload() -> dict[str, object]:
    return {
        "prompts": [
            {
                "name": "greet-user",
                "description": "Genera instrucciones para saludar a una persona en un idioma concreto usando say_hello.",
                "arguments": [
                    {"name": "name", "description": "Nombre de la persona a saludar.", "required": True},
                    {"name": "lang", "description": "Idioma deseado para el saludo.", "required": True},
                ],
            },
            {
                "name": "language-report",
                "description": "Genera instrucciones para listar idiomas y luego saludar a alguien en todos ellos.",
                "arguments": [
                    {"name": "name", "description": "Nombre que se usará en todos los saludos.", "required": True},
                ],
            },
        ]
    }


def build_prompt_details_payload(prompt_name: str, name: str | None = None, lang: str | None = None) -> dict[str, object]:
    if prompt_name == "greet-user":
        resolved_name = name or "Ada Lovelace"
        resolved_lang = normalize_lang(lang)
        return {
            "description": "Prompt para pedir un saludo único usando say_hello.",
            "messages": [
                {
                    "role": "system",
                    "content": {
                        "type": "text",
                        "text": "Usa la herramienta say_hello y responde solo con el resultado obtenido.",
                    },
                },
                {
                    "role": "user",
                    "content": {
                        "type": "text",
                        "text": f"Saluda a {resolved_name} en {resolved_lang} usando say_hello.",
                    },
                },
            ],
        }

    if prompt_name == "language-report":
        resolved_name = name or "Pedro"
        return {
            "description": "Prompt para pedir un reporte de idiomas y saludos usando herramientas MCP.",
            "messages": [
                {
                    "role": "system",
                    "content": {
                        "type": "text",
                        "text": "Usa solo tools disponibles y no inventes idiomas.",
                    },
                },
                {
                    "role": "user",
                    "content": {
                        "type": "text",
                        "text": (
                            f"Usa get_hello_languages para obtener los idiomas soportados y luego "
                            f"usa say_hello para saludar a {resolved_name} en cada idioma."
                        ),
                    },
                },
            ],
        }

    raise KeyError(prompt_name)


def build_hello_payload(name: str | None, lang: str | None, ip: str) -> dict[str, object]:
    normalized_lang = normalize_lang(lang)
    greeting = GREETINGS[normalized_lang]
    timestamp = datetime.now(timezone.utc).strftime("%H:%M:%S UTC+00:00")

    if name:
        return {
            "message": f"{greeting} {name}!",
            "timestamp": timestamp,
            "ip": ip,
            "lang": normalized_lang,
            "has_name": True,
            "name": name,
        }

    return {
        "message": f"{greeting}!",
        "timestamp": timestamp,
        "ip": ip,
        "lang": normalized_lang,
        "has_name": False,
    }


def normalize_lang(lang: str | None) -> str:
    if not lang:
        return DEFAULT_LANG

    normalized = lang.strip().lower()
    if normalized in GREETINGS:
        return normalized
    return DEFAULT_LANG

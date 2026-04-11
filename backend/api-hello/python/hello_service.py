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

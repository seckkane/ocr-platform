"""
Configuration centralisee 12-factor via variables d'environnement.

Equivalent Spring Boot : application.yml + @ConfigurationProperties.

Usage :
    from app.core.config import settings
    print(settings.app_name)

Variables d'env supportees (toutes prefixees pour eviter les collisions) :
    APP_NAME, APP_ENV, APP_LOG_LEVEL, APP_PORT
"""

from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuration applicative chargee depuis les variables d'environnement."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        env_prefix="APP_",  # toutes les vars commencent par APP_
        case_sensitive=False,
        extra="ignore",  # ignore les vars d'env inconnues
    )

    # ----- Identite du service -----
    app_name: str = "ocr-service"
    app_version: str = "0.1.0"
    app_env: Literal["dev", "test", "staging", "prod"] = "dev"

    # ----- Serveur HTTP -----
    app_port: int = 8082  # document-service est sur 8081, on prend 8082
    app_host: str = "0.0.0.0"

    # ----- Logging -----
    app_log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"


@lru_cache
def get_settings() -> Settings:
    """
    Singleton settings. Le cache lru_cache evite de re-parser les
    variables d'env a chaque appel (perf + coherence).
    """
    return Settings()


# Instance globale exportee (import direct pratique)
settings = get_settings()

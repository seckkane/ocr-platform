"""
Point d'entree FastAPI du ocr-service.

Equivalent Spring Boot : DocumentServiceApplication.java.

Run local :
    uv run uvicorn app.main:app --reload --port 8082

Run prod (via Dockerfile) :
    uvicorn app.main:app --host 0.0.0.0 --port 8082
"""

from fastapi import FastAPI

from app.core.config import settings

# ============================================================================
# Application FastAPI
# ============================================================================
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="OCR microservice - extracts text from documents",
    docs_url="/swagger-ui.html",  # coherence avec document-service
    redoc_url="/redoc",
    openapi_url="/v3/api-docs",
)


# ============================================================================
# Endpoints racine
# ============================================================================


@app.get("/", tags=["root"])
async def root() -> dict[str, str]:
    """Endpoint racine - infos basiques sur le service."""
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "env": settings.app_env,
        "status": "running",
    }


@app.get("/health", tags=["actuator"])
async def health() -> dict[str, str]:
    """
    Healthcheck simple.

    Phase 8.1 : juste un OK statique.
    Phase 8.9 : on ajoutera des checks BDD + Kafka + MinIO (deep healthcheck).
    """
    return {"status": "UP"}

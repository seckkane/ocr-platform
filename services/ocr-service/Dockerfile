# ============================================================================
# ocr-service - Dockerfile multi-stage
# ----------------------------------------------------------------------------
# Stage 1 (builder) : install deps avec uv (lourd)
# Stage 2 (runtime) : image minimale avec Python + app uniquement
# ============================================================================

# ============================================================================
# Stage 1 : BUILDER
# ============================================================================
FROM python:3.12-slim-bookworm AS builder

# uv : copie le binaire depuis l'image officielle (rapide, pas de pip install)
COPY --from=ghcr.io/astral-sh/uv:0.5.13 /uv /uvx /bin/

# Variables uv pour build reproductible
ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    UV_PYTHON_DOWNLOADS=never

WORKDIR /app

# Copie d'abord les fichiers de deps (cache Docker maximise)
COPY pyproject.toml uv.lock ./

# Install deps SANS le projet lui-meme (cache layer)
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-install-project --no-dev

# Copie le code source
COPY app ./app

# Install le projet lui-meme (sans deps dev)
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev


# ============================================================================
# Stage 2 : RUNTIME
# ============================================================================
FROM python:3.12-slim-bookworm AS runtime

# Metadonnees image (OCI labels - bonne pratique)
LABEL org.opencontainers.image.title="ocr-service" \
      org.opencontainers.image.description="OCR microservice for OCR Platform" \
      org.opencontainers.image.source="https://github.com/seckkane/ocr-platform" \
      org.opencontainers.image.licenses="Internal"

# Non-root user (securite : ne JAMAIS run en root en prod)
RUN groupadd --system --gid 1001 ocr && \
    useradd --system --uid 1001 --gid ocr --shell /bin/false ocr

WORKDIR /app

# Copie depuis le builder : le venv complet (avec deps installees)
COPY --from=builder --chown=ocr:ocr /app/.venv /app/.venv
COPY --from=builder --chown=ocr:ocr /app/app /app/app

# Path : utilise les binaires du venv en priorite
ENV PATH="/app/.venv/bin:$PATH" \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# Switch vers user non-root
USER ocr

EXPOSE 8082

# Healthcheck Docker (interroge notre endpoint /health)
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8082/health').read()" || exit 1

# Commande de demarrage : uvicorn en mode prod (pas de --reload)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8082"]

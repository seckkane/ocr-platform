"""
Tests des endpoints racine de l'application.

Equivalent Spring : DocumentControllerIT mais en plus simple
(pas besoin de Testcontainers ici, juste TestClient en memoire).
"""

from fastapi.testclient import TestClient

# ============================================================================
# Tests de l'endpoint racine GET /
# ============================================================================


def test_root_returns_200(client: TestClient) -> None:
    """L'endpoint racine doit repondre 200 OK."""
    response = client.get("/")

    assert response.status_code == 200


def test_root_returns_service_info(client: TestClient) -> None:
    """L'endpoint racine doit renvoyer les infos du service."""
    response = client.get("/")

    body = response.json()
    assert body["service"] == "ocr-service"
    assert body["version"] == "0.1.0"
    assert body["status"] == "running"
    assert "env" in body


# ============================================================================
# Tests du healthcheck GET /health
# ============================================================================


def test_health_returns_200(client: TestClient) -> None:
    """Le healthcheck doit repondre 200 OK."""
    response = client.get("/health")

    assert response.status_code == 200


def test_health_returns_up_status(client: TestClient) -> None:
    """Le healthcheck doit renvoyer status=UP (convention Spring Actuator)."""
    response = client.get("/health")

    assert response.json() == {"status": "UP"}


# ============================================================================
# Tests OpenAPI / Swagger UI
# ============================================================================


def test_openapi_spec_is_exposed(client: TestClient) -> None:
    """La spec OpenAPI doit etre accessible sur /v3/api-docs."""
    response = client.get("/v3/api-docs")

    assert response.status_code == 200
    assert response.json()["info"]["title"] == "ocr-service"


def test_swagger_ui_is_accessible(client: TestClient) -> None:
    """Swagger UI doit etre servi sur /swagger-ui.html."""
    response = client.get("/swagger-ui.html")

    assert response.status_code == 200

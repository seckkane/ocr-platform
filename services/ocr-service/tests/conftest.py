"""
Configuration partagee pour tous les tests pytest.

Equivalent Spring : @TestConfiguration + beans de test.

Les fixtures definies ici sont automatiquement disponibles dans tous
les fichiers test_*.py du dossier tests/.
"""

import pytest
from app.main import app
from fastapi.testclient import TestClient


@pytest.fixture
def client() -> TestClient:
    """
    Client HTTP de test pour FastAPI.

    Usage dans un test :
        def test_health(client):
            response = client.get("/health")
            assert response.status_code == 200

    Equivalent Spring : MockMvc / WebTestClient.
    """
    return TestClient(app)

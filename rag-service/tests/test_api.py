import os
import unittest

from fastapi.testclient import TestClient

from app.main import app


class ApiTest(unittest.TestCase):
    def test_health_requires_internal_api_key(self) -> None:
        client = TestClient(app)

        self.assertEqual(422, client.get("/internal/health").status_code)
        response = client.get(
            "/internal/health",
            headers={"X-Internal-Api-Key": os.environ.get("RAG_SERVICE_API_KEY", "local-rag-secret")},
        )

        self.assertEqual(200, response.status_code)
        self.assertEqual("UP", response.json()["status"])


if __name__ == "__main__":
    unittest.main()

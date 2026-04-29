# ============================================================
#  core/ollama_client.py — Ollama local model interface
# ============================================================

import json
import requests
from config import OLLAMA_BASE_URL, OLLAMA_MODEL, OLLAMA_TIMEOUT


class OllamaClient:
    """Handles all communication with the local Ollama server."""

    def __init__(self, model: str = OLLAMA_MODEL):
        self.model   = model
        self.base    = OLLAMA_BASE_URL
        self.timeout = OLLAMA_TIMEOUT

    # ----------------------------------------------------------
    def is_available(self) -> bool:
        """Ping Ollama to verify the server is running."""
        try:
            r = requests.get(f"{self.base}/api/tags", timeout=5)
            return r.status_code == 200
        except Exception:
            return False

    # ----------------------------------------------------------
    def list_models(self) -> list[str]:
        """Return names of all locally installed models."""
        try:
            r = requests.get(f"{self.base}/api/tags", timeout=5)
            r.raise_for_status()
            return [m["name"] for m in r.json().get("models", [])]
        except Exception:
            return []

    # ----------------------------------------------------------
    def generate(self, prompt: str, system: str = "") -> str:
        """
        Send a prompt to Ollama and return the full response text.
        Uses the /api/generate endpoint (no streaming).
        """
        payload = {
            "model":  self.model,
            "prompt": prompt,
            "stream": False,
        }
        if system:
            payload["system"] = system

        try:
            r = requests.post(
                f"{self.base}/api/generate",
                json=payload,
                timeout=self.timeout,
            )
            r.raise_for_status()
            return r.json().get("response", "").strip()

        except requests.exceptions.ConnectionError:
            raise ConnectionError(
                "Cannot reach Ollama. Make sure it is running: `ollama serve`"
            )
        except requests.exceptions.Timeout:
            raise TimeoutError(
                f"Ollama did not respond within {self.timeout}s. "
                "Try a faster model or increase OLLAMA_TIMEOUT in config.py"
            )
        except requests.exceptions.HTTPError as e:
            raise RuntimeError(f"Ollama HTTP error: {e}")

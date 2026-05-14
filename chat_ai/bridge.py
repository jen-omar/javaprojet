#!/usr/bin/env python3
# ============================================================
#  bridge.py — Java ↔ Python bridge for Mythoria Chat AI
#  Called by AiBridge.java via ProcessBuilder.
#  Receives JSON on stdin, returns JSON on stdout.
# ============================================================

import sys
import os
import json

# Ensure imports work from project root
sys.path.insert(0, os.path.dirname(__file__))

from config import OLLAMA_MODEL
from core.ollama_client import OllamaClient
from core.ai_engine import AIEngine


def main():
    """
    Expected input (JSON on stdin):
    {
        "command":  "summary" | "suggest" | "autoreply",
        "messages": [ {"sender": "...", "text": "..."}, ... ],
        "persona":  "..." (optional, for autoreply)
    }

    Output (JSON on stdout):
    {
        "status":  "ok" | "error",
        "result":  <string or list depending on command>,
        "error":   "..." (only when status == "error")
    }
    """
    try:
        raw_input = sys.stdin.read()
        request = json.loads(raw_input)
    except (json.JSONDecodeError, Exception) as e:
        _respond_error(f"Invalid JSON input: {e}")
        return

    command  = request.get("command", "").lower()
    messages = request.get("messages", [])
    persona  = request.get("persona", "friendly professional assistant")

    if not messages:
        _respond_error("No messages provided.")
        return

    # Initialize Ollama client and engine
    try:
        client = OllamaClient()
        if not client.is_available():
            _respond_error("Ollama is not running. Start it with: ollama serve")
            return
        engine = AIEngine(client)
    except Exception as e:
        _respond_error(f"Failed to initialize AI engine: {e}")
        return

    # Dispatch command
    try:
        if command == "summary":
            result = engine.summarize(messages)
            _respond_ok(result)

        elif command == "suggest":
            suggestions = engine.suggest_replies(messages)
            _respond_ok(suggestions)

        elif command == "autoreply":
            reply = engine.auto_reply(messages, persona)
            _respond_ok(reply)

        else:
            _respond_error(f"Unknown command: '{command}'. Use: summary, suggest, autoreply")

    except Exception as e:
        _respond_error(f"AI engine error: {e}")


def _respond_ok(result):
    """Write a success response as JSON to stdout."""
    response = {"status": "ok", "result": result}
    sys.stdout.write(json.dumps(response, ensure_ascii=False))
    sys.stdout.flush()


def _respond_error(message: str):
    """Write an error response as JSON to stdout."""
    response = {"status": "error", "error": message}
    sys.stdout.write(json.dumps(response, ensure_ascii=False))
    sys.stdout.flush()


if __name__ == "__main__":
    main()

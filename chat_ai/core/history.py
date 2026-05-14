# ============================================================
#  core/history.py — Chat history manager
# ============================================================

import json
import os
from datetime import datetime
from config import HISTORY_FILE


class ChatHistory:
    """
    Manages chat sessions: loading, saving, adding messages,
    and switching between named sessions.
    """

    def __init__(self):
        self.sessions: dict[str, list[dict]] = {}
        self.active_session: str = "default"
        os.makedirs(os.path.dirname(HISTORY_FILE), exist_ok=True)
        self._load()

    # ----------------------------------------------------------
    def _load(self):
        if os.path.exists(HISTORY_FILE):
            try:
                with open(HISTORY_FILE, "r", encoding="utf-8") as f:
                    self.sessions = json.load(f)
            except (json.JSONDecodeError, IOError):
                self.sessions = {}
        if self.active_session not in self.sessions:
            self.sessions[self.active_session] = []

    # ----------------------------------------------------------
    def save(self):
        with open(HISTORY_FILE, "w", encoding="utf-8") as f:
            json.dump(self.sessions, f, indent=2, ensure_ascii=False)

    # ----------------------------------------------------------
    def add_message(self, sender: str, text: str):
        msg = {
            "sender": sender,
            "text":   text,
            "time":   datetime.now().strftime("%H:%M"),
            "date":   datetime.now().strftime("%Y-%m-%d"),
        }
        self.sessions[self.active_session].append(msg)
        self.save()

    # ----------------------------------------------------------
    def get_messages(self) -> list[dict]:
        return self.sessions.get(self.active_session, [])

    # ----------------------------------------------------------
    def clear_session(self):
        self.sessions[self.active_session] = []
        self.save()

    # ----------------------------------------------------------
    def switch_session(self, name: str):
        self.active_session = name
        if name not in self.sessions:
            self.sessions[name] = []
        self.save()

    # ----------------------------------------------------------
    def list_sessions(self) -> list[str]:
        return list(self.sessions.keys())

    # ----------------------------------------------------------
    def delete_session(self, name: str) -> bool:
        if name in self.sessions and name != self.active_session:
            del self.sessions[name]
            self.save()
            return True
        return False

    # ----------------------------------------------------------
    def message_count(self) -> int:
        return len(self.get_messages())

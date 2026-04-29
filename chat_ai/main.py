#!/usr/bin/env python3
# ============================================================
#  main.py — Chat AI CLI — Professional Entry Point
# ============================================================

import sys
import os

# Make sure local imports work when running from project root
sys.path.insert(0, os.path.dirname(__file__))

from config           import Color, OLLAMA_MODEL
from core.ollama_client import OllamaClient
from core.ai_engine   import AIEngine
from core.history     import ChatHistory
from utils.display    import (
    print_banner, print_box, print_menu, print_message,
    print_info, print_success, print_warning, print_error,
    print_loading, print_sentiment, print_suggestions,
    print_sessions, divider, clear_screen,
)
from utils.helpers    import prompt, confirm, choose, exit_app


# ============================================================
MENU_OPTIONS = [
    ("/add  <sender> <text>", "Add a message to the conversation"),
    ("/me   <text>",          "Add your own message"),
    ("/show",                 "Display full conversation"),
    ("/summary",              "AI: Summarize the conversation"),
    ("/suggest",              "AI: Get smart reply suggestions"),
    ("/autoreply",            "AI: Generate an auto-reply"),
    ("/sentiment",            "AI: Analyse conversation sentiment"),
    ("/session",              "Manage sessions (list/switch/clear/delete)"),
    ("/model",                "Change Ollama model"),
    ("/clear",                "Clear the screen"),
    ("/help",                 "Show this menu"),
    ("/exit",                 "Quit the application"),
]


# ============================================================
class ChatAICLI:

    def __init__(self):
        self.history = ChatHistory()
        self.client  = OllamaClient()
        self.engine  = AIEngine(self.client)

    # ----------------------------------------------------------
    def startup_check(self):
        """Verify Ollama is running and show status."""
        print_loading("Connecting to Ollama")
        if not self.client.is_available():
            print_error("Ollama is not running!")
            print_info("Start it with:  ollama serve")
            print_info("Then run:       ollama pull llama3")
            sys.exit(1)

        models = self.client.list_models()
        if not models:
            print_warning("No models installed.")
            print_info("Install one with:  ollama pull llama3")
            sys.exit(1)

        if OLLAMA_MODEL not in models:
            print_warning(
                f"Model '{OLLAMA_MODEL}' not found. Available: {', '.join(models)}"
            )
            print_info(f"Edit OLLAMA_MODEL in config.py to one of the above.")
            sys.exit(1)

        print_success(f"Ollama connected  |  Model: {Color.BOLD}{self.client.model}{Color.RESET}")
        print_success(
            f"Session: {Color.BOLD}{self.history.active_session}{Color.RESET}"
            f"  |  Messages: {self.history.message_count()}"
        )

    # ----------------------------------------------------------
    def require_messages(self) -> bool:
        if self.history.message_count() == 0:
            print_warning("No messages in this session. Add some with /add or /me")
            return False
        return True

    # ----------------------------------------------------------
    # COMMAND HANDLERS
    # ----------------------------------------------------------
    def cmd_add(self, args: list[str]):
        if len(args) < 2:
            print_warning("Usage: /add <sender_name> <message text>")
            return
        sender = args[0]
        text   = " ".join(args[1:])
        self.history.add_message(sender, text)
        print_success(f"Message added from '{sender}'")

    def cmd_me(self, args: list[str]):
        if not args:
            print_warning("Usage: /me <message text>")
            return
        text = " ".join(args)
        self.history.add_message("Me", text)
        print_success("Your message added.")

    def cmd_show(self):
        messages = self.history.get_messages()
        if not messages:
            print_warning("No messages yet.")
            return
        divider()
        print(f"  {Color.BOLD}Session: {self.history.active_session}{Color.RESET}"
              f"  ({len(messages)} messages)\n")
        for msg in messages:
            is_me = msg["sender"].lower() in ("me", "myself", "i")
            print_message(msg["sender"], msg["text"], msg.get("time", ""), me=is_me)
        divider()

    def cmd_summary(self):
        if not self.require_messages():
            return
        print_loading("Generating conversation summary")
        try:
            result = self.engine.summarize(self.history.get_messages())
            print_box("CONVERSATION SUMMARY", result, color=Color.CYAN)
        except Exception as e:
            print_error(str(e))

    def cmd_suggest(self):
        if not self.require_messages():
            return
        print_loading("Generating reply suggestions")
        try:
            suggestions = self.engine.suggest_replies(self.history.get_messages())
            print_suggestions(suggestions)

            # Ask if user wants to send one
            idx = choose("Send a suggestion as your reply?", suggestions + ["← Skip"])
            if 0 <= idx < len(suggestions):
                self.history.add_message("Me", suggestions[idx])
                print_success(f"Sent: {suggestions[idx]}")
        except Exception as e:
            print_error(str(e))

    def cmd_autoreply(self):
        if not self.require_messages():
            return
        persona = prompt(
            "Bot persona / role",
            default="friendly professional assistant"
        )
        print_loading("Generating auto-reply")
        try:
            reply = self.engine.auto_reply(self.history.get_messages(), persona)
            print_box("AUTO-REPLY", reply, color=Color.GREEN)
            if confirm("Send this as your reply?"):
                self.history.add_message("Me", reply)
                print_success("Auto-reply added to conversation.")
        except Exception as e:
            print_error(str(e))

    def cmd_sentiment(self):
        if not self.require_messages():
            return
        print_loading("Analysing sentiment")
        try:
            data = self.engine.analyze_sentiment(self.history.get_messages())
            print_sentiment(data)
        except Exception as e:
            print_error(str(e))

    def cmd_session(self):
        options = ["List sessions", "Switch session", "Clear current session", "Delete a session"]
        idx = choose("Session manager", options)
        if idx == 0:
            print_sessions(self.history.list_sessions(), self.history.active_session)
        elif idx == 1:
            sessions = self.history.list_sessions()
            sidx = choose("Switch to:", sessions)
            if sidx >= 0:
                self.history.switch_session(sessions[sidx])
                print_success(f"Switched to session '{sessions[sidx]}'")
            else:
                new_name = prompt("Or enter a new session name")
                if new_name:
                    self.history.switch_session(new_name)
                    print_success(f"Created and switched to '{new_name}'")
        elif idx == 2:
            if confirm(f"Clear all messages in '{self.history.active_session}'?"):
                self.history.clear_session()
                print_success("Session cleared.")
        elif idx == 3:
            sessions = [s for s in self.history.list_sessions()
                        if s != self.history.active_session]
            if not sessions:
                print_warning("No other sessions to delete.")
                return
            sidx = choose("Delete session:", sessions)
            if sidx >= 0 and confirm(f"Delete '{sessions[sidx]}'?"):
                self.history.delete_session(sessions[sidx])
                print_success(f"Deleted session '{sessions[sidx]}'")

    def cmd_model(self):
        models = self.client.list_models()
        if not models:
            print_warning("No models installed.")
            return
        idx = choose("Select model:", models)
        if idx >= 0:
            self.client.model = models[idx]
            print_success(f"Model switched to: {models[idx]}")

    # ----------------------------------------------------------
    # MAIN LOOP
    # ----------------------------------------------------------
    def run(self):
        clear_screen()
        print_banner()
        self.startup_check()
        print()
        print_menu(MENU_OPTIONS)

        while True:
            try:
                raw = input(
                    f"\n{Color.CYAN}{Color.BOLD}chat-ai{Color.RESET}"
                    f"{Color.GREY}({self.history.active_session}){Color.RESET}"
                    f"{Color.CYAN}❯{Color.RESET} "
                ).strip()
            except (KeyboardInterrupt, EOFError):
                exit_app()

            if not raw:
                continue

            parts = raw.split()
            cmd   = parts[0].lower()
            args  = parts[1:]

            dispatch = {
                "/add":       lambda: self.cmd_add(args),
                "/me":        lambda: self.cmd_me(args),
                "/show":      lambda: self.cmd_show(),
                "/summary":   lambda: self.cmd_summary(),
                "/suggest":   lambda: self.cmd_suggest(),
                "/autoreply": lambda: self.cmd_autoreply(),
                "/sentiment": lambda: self.cmd_sentiment(),
                "/session":   lambda: self.cmd_session(),
                "/model":     lambda: self.cmd_model(),
                "/clear":     lambda: (clear_screen(), print_banner()),
                "/help":      lambda: print_menu(MENU_OPTIONS),
                "/exit":      lambda: exit_app(),
                "/quit":      lambda: exit_app(),
            }

            handler = dispatch.get(cmd)
            if handler:
                handler()
            else:
                print_warning(f"Unknown command '{cmd}'. Type /help for the list.")


# ============================================================
if __name__ == "__main__":
    app = ChatAICLI()
    app.run()

# ============================================================
#  utils/helpers.py — Input helpers and validators
# ============================================================

import sys
from config import Color


def prompt(label: str, default: str = "") -> str:
    """Display a styled input prompt and return stripped input."""
    hint = f" [{default}]" if default else ""
    try:
        value = input(f"  {Color.CYAN}{Color.BOLD}❯{Color.RESET} {label}{hint}: ").strip()
        return value if value else default
    except (KeyboardInterrupt, EOFError):
        print()
        return default


def confirm(question: str) -> bool:
    """Yes/No confirmation prompt."""
    try:
        answer = input(
            f"  {Color.YELLOW}?{Color.RESET}  {question} (y/n): "
        ).strip().lower()
        return answer in ("y", "yes")
    except (KeyboardInterrupt, EOFError):
        return False


def choose(question: str, options: list[str]) -> int:
    """
    Numbered choice menu. Returns 0-based index of chosen option.
    Returns -1 if input is invalid.
    """
    print(f"\n  {Color.CYAN}{question}{Color.RESET}")
    for i, opt in enumerate(options, 1):
        print(f"    {Color.YELLOW}{Color.BOLD}{i}.{Color.RESET}  {opt}")
    try:
        raw = input(f"  {Color.CYAN}❯{Color.RESET} Choice: ").strip()
        idx = int(raw) - 1
        if 0 <= idx < len(options):
            return idx
    except (ValueError, KeyboardInterrupt, EOFError):
        pass
    return -1


def exit_app(message: str = "Goodbye! 👋"):
    print(f"\n  {Color.GREEN}{message}{Color.RESET}\n")
    sys.exit(0)

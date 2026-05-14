# ============================================================
#  utils/display.py — Professional terminal UI renderer
# ============================================================

import os
import sys
import textwrap
from config import Color


def _term_width() -> int:
    try:
        return min(os.get_terminal_size().columns, 100)
    except OSError:
        return 88


def _center(text: str, width: int, fill: str = " ") -> str:
    pad = max(0, width - len(text))
    left = pad // 2
    right = pad - left
    return fill * left + text + fill * right


# ----------------------------------------------------------
# BANNER
# ----------------------------------------------------------
def print_banner():
    w = _term_width()
    lines = [
        "",
        f"{Color.CYAN}{Color.BOLD}{'═' * w}{Color.RESET}",
        f"{Color.CYAN}{Color.BOLD}{_center('  CHAT AI — Professional Assistant CLI  ', w)}{Color.RESET}",
        f"{Color.CYAN}{Color.BOLD}{_center('Powered by Ollama · Fully Offline · No API Key', w)}{Color.RESET}",
        f"{Color.CYAN}{Color.BOLD}{'═' * w}{Color.RESET}",
        "",
    ]
    print("\n".join(lines))


# ----------------------------------------------------------
# SECTION BOXES
# ----------------------------------------------------------
def print_box(title: str, content: str, color: str = Color.CYAN):
    w     = _term_width()
    inner = w - 4
    top   = f"{color}╔{'═' * (w - 2)}╗{Color.RESET}"
    mid   = f"{color}║ {Color.BOLD}{title:<{inner}}{Color.RESET}{color} ║{Color.RESET}"
    sep   = f"{color}╠{'═' * (w - 2)}╣{Color.RESET}"
    bot   = f"{color}╚{'═' * (w - 2)}╝{Color.RESET}"

    rows = []
    for line in content.splitlines():
        wrapped = textwrap.wrap(line, width=inner) or [""]
        for wl in wrapped:
            rows.append(f"{color}║ {Color.RESET}{wl:<{inner}}{color} ║{Color.RESET}")

    print(top)
    print(mid)
    print(sep)
    for row in rows:
        print(row)
    print(bot)
    print()


# ----------------------------------------------------------
# CHAT MESSAGES
# ----------------------------------------------------------
def print_message(sender: str, text: str, time: str = "", me: bool = False):
    w     = _term_width()
    inner = w - 6

    if me:
        color  = Color.BLUE
        prefix = f"  {Color.BOLD}{Color.BLUE}▶ {sender}{Color.RESET}"
    else:
        color  = Color.GREEN
        prefix = f"  {Color.BOLD}{Color.GREEN}◀ {sender}{Color.RESET}"

    ts = f"{Color.GREY}  [{time}]{Color.RESET}" if time else ""
    print(f"{prefix}{ts}")

    wrapped = textwrap.wrap(text, width=inner)
    for line in wrapped:
        indent = "     " if me else "     "
        print(f"{indent}{color}{line}{Color.RESET}")
    print()


# ----------------------------------------------------------
# MENU
# ----------------------------------------------------------
def print_menu(options: list[tuple[str, str]]):
    w = _term_width()
    print(f"{Color.CYAN}{'─' * w}{Color.RESET}")
    print(f"{Color.BOLD}  COMMANDS:{Color.RESET}")
    for key, desc in options:
        print(f"  {Color.YELLOW}{Color.BOLD}{key:<16}{Color.RESET}{Color.WHITE}{desc}{Color.RESET}")
    print(f"{Color.CYAN}{'─' * w}{Color.RESET}")
    print()


# ----------------------------------------------------------
# STATUS MESSAGES
# ----------------------------------------------------------
def print_info(msg: str):
    print(f"  {Color.CYAN}ℹ  {msg}{Color.RESET}")

def print_success(msg: str):
    print(f"  {Color.GREEN}✔  {msg}{Color.RESET}")

def print_warning(msg: str):
    print(f"  {Color.YELLOW}⚠  {msg}{Color.RESET}")

def print_error(msg: str):
    print(f"  {Color.RED}✖  {msg}{Color.RESET}")

def print_loading(msg: str):
    print(f"  {Color.MAGENTA}⟳  {msg}...{Color.RESET}", flush=True)


# ----------------------------------------------------------
# SENTIMENT REPORT
# ----------------------------------------------------------
def print_sentiment(data: dict):
    w     = _term_width()
    inner = w - 4
    color_map = {
        "positive": Color.GREEN,
        "negative": Color.RED,
        "neutral":  Color.GREY,
        "mixed":    Color.YELLOW,
        "unknown":  Color.GREY,
    }
    overall = data.get("overall", "unknown")
    score   = data.get("score",   0.0)
    tone    = data.get("tone",    "—")
    col     = color_map.get(overall, Color.WHITE)

    # Score bar
    bar_len  = 30
    filled   = int((score + 1) / 2 * bar_len)
    bar      = "█" * filled + "░" * (bar_len - filled)

    lines = [
        f"Overall Sentiment : {col}{Color.BOLD}{overall.upper()}{Color.RESET}",
        f"Tone              : {tone}",
        f"Score             : {col}{bar}{Color.RESET}  {score:+.2f}",
        "",
    ]

    # Per-speaker
    per_spk = data.get("per_speaker", {})
    if per_spk:
        lines.append(f"{Color.BOLD}Per Speaker:{Color.RESET}")
        for spk, sent in per_spk.items():
            c = color_map.get(sent, Color.WHITE)
            lines.append(f"  {spk:<20} {c}{sent}{Color.RESET}")
        lines.append("")

    # Keywords
    kws = data.get("keywords", [])
    if kws:
        lines.append(f"{Color.BOLD}Emotional Keywords:{Color.RESET}  " +
                     "  ".join(f"{Color.MAGENTA}{k}{Color.RESET}" for k in kws))
        lines.append("")

    # Explanation
    exp = data.get("explanation", "")
    if exp:
        lines.append(f"{Color.BOLD}Analysis:{Color.RESET}")
        for part in textwrap.wrap(exp, width=inner - 2):
            lines.append(f"  {part}")

    print_box("SENTIMENT ANALYSIS", "\n".join(lines), color=Color.MAGENTA)


# ----------------------------------------------------------
# SUGGESTIONS
# ----------------------------------------------------------
def print_suggestions(suggestions: list[str]):
    w = _term_width()
    print(f"\n{Color.YELLOW}{Color.BOLD}  💬 REPLY SUGGESTIONS:{Color.RESET}")
    print(f"  {Color.YELLOW}{'─' * (w - 4)}{Color.RESET}")
    for i, s in enumerate(suggestions, 1):
        print(f"  {Color.BOLD}{Color.YELLOW}[{i}]{Color.RESET}  {s}")
    print()


# ----------------------------------------------------------
# SESSIONS TABLE
# ----------------------------------------------------------
def print_sessions(sessions: list[str], active: str):
    print(f"\n{Color.CYAN}{Color.BOLD}  SESSIONS:{Color.RESET}")
    for s in sessions:
        marker = f"{Color.GREEN} ◀ active{Color.RESET}" if s == active else ""
        print(f"  {Color.WHITE}• {s}{marker}{Color.RESET}")
    print()


# ----------------------------------------------------------
# DIVIDER
# ----------------------------------------------------------
def divider(char: str = "─", color: str = Color.GREY):
    w = _term_width()
    print(f"{color}{char * w}{Color.RESET}")


# ----------------------------------------------------------
# CLEAR
# ----------------------------------------------------------
def clear_screen():
    os.system("cls" if sys.platform == "win32" else "clear")

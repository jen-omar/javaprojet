# ============================================================
#  config.py — Central configuration for Chat AI CLI
# ============================================================

OLLAMA_BASE_URL  = "http://localhost:11434"
OLLAMA_MODEL     = "llama3:latest"   # Change to any installed model
OLLAMA_TIMEOUT   = 120               # seconds

# History file path (stored locally)
HISTORY_FILE     = "data/chat_history.json"

# Sentiment thresholds (used for label mapping)
SENTIMENT_LABELS = {
    "positive": "😊 Positive",
    "negative": "😟 Negative",
    "neutral":  "😐 Neutral",
    "mixed":    "🔀 Mixed",
}

# Number of reply suggestions to generate
NUM_SUGGESTIONS  = 3

# Color codes for terminal output (ANSI)
class Color:
    RESET   = "\033[0m"
    BOLD    = "\033[1m"
    DIM     = "\033[2m"
    CYAN    = "\033[96m"
    GREEN   = "\033[92m"
    YELLOW  = "\033[93m"
    RED     = "\033[91m"
    MAGENTA = "\033[95m"
    BLUE    = "\033[94m"
    WHITE   = "\033[97m"
    GREY    = "\033[90m"
    BG_DARK = "\033[40m"

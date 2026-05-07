# Chat AI — Professional CLI Assistant
> Powered by Ollama · Fully Offline · No API Key Required

---

## Features

| Feature              | Command        | Description                                      |
|----------------------|----------------|--------------------------------------------------|
| Chat Summary         | `/summary`     | Concise AI summary with key topics & action items |
| Smart Suggestions    | `/suggest`     | 3 contextual reply options (tap to send)         |
| Auto-Reply Bot       | `/autoreply`   | One-click AI-generated reply with custom persona |
| Sentiment Analysis   | `/sentiment`   | Tone, score, per-speaker breakdown, keywords     |
| Session Management   | `/session`     | Multiple named conversations, persistent storage |
| Model Switching      | `/model`       | Switch Ollama models without restarting          |

---

## Requirements

- Python 3.10+
- [Ollama](https://ollama.com) installed and running
- At least one model pulled (e.g. `llama3`, `mistral`, `phi3`)

---

## Installation

```bash
# 1. Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# 2. Pull a model
ollama pull llama3

# 3. Start Ollama server
ollama serve

# 4. Install Python dependency
pip install requests

# 5. Run the app
cd chat_ai
python main.py
```

---

## Configuration (`config.py`)

```python
OLLAMA_BASE_URL = "http://localhost:11434"   # Ollama server address
OLLAMA_MODEL    = "llama3"                   # Default model to use
OLLAMA_TIMEOUT  = 120                        # Request timeout (seconds)
NUM_SUGGESTIONS = 3                          # Number of reply suggestions
```

---

## Usage

```
chat-ai(default)❯ /add Ali  Hey! Are we meeting tomorrow?
chat-ai(default)❯ /me       Yes, 3pm at the office!
chat-ai(default)❯ /add Ali  Perfect. Should I bring the mockups?
chat-ai(default)❯ /show          → display full conversation
chat-ai(default)❯ /summary       → AI summary
chat-ai(default)❯ /suggest       → 3 reply options
chat-ai(default)❯ /autoreply     → auto-reply (choose persona)
chat-ai(default)❯ /sentiment     → tone analysis
chat-ai(default)❯ /session       → manage sessions
chat-ai(default)❯ /model         → switch model
chat-ai(default)❯ /help          → show all commands
chat-ai(default)❯ /exit          → quit
```

---

## Project Structure

```
chat_ai/
├── main.py               ← Entry point (CLI loop)
├── config.py             ← All settings in one place
├── requirements.txt
├── data/
│   └── chat_history.json ← Auto-created, persistent sessions
├── core/
│   ├── ollama_client.py  ← Ollama API wrapper
│   ├── ai_engine.py      ← Summary / Suggest / AutoReply / Sentiment
│   └── history.py        ← Session & message management
└── utils/
    ├── display.py        ← Terminal UI (boxes, colors, tables)
    └── helpers.py        ← Input prompts, confirm, choose
```

---

## Tips

- Use `/session` to keep separate conversations (e.g. work, personal)
- Use `/model` to switch between installed Ollama models live
- Edit `config.py` to tune timeout, model, or suggestion count
- History is saved automatically in `data/chat_history.json`

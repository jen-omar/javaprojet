# ============================================================
#  core/ai_engine.py — Summary, Suggestions, Auto-Reply, Sentiment
# ============================================================

import json
import re
from config import NUM_SUGGESTIONS
from core.ollama_client import OllamaClient


class AIEngine:
    """
    Wraps the four AI features: summary, suggestions,
    auto-reply, and sentiment analysis.
    """

    def __init__(self, client: OllamaClient):
        self.client = client

    # ----------------------------------------------------------
    # 1. CHAT SUMMARY
    # ----------------------------------------------------------
    def summarize(self, conversation: list[dict]) -> str:
        """
        Produce a concise professional summary of the conversation.
        conversation: list of {"sender": str, "text": str}
        """
        formatted = self._format_conversation(conversation)

        system = (
            "You are an expert conversation analyst. "
            "You produce clear, concise, professional summaries."
        )
        prompt = (
            f"Analyze the following conversation and provide:\n"
            f"1. A 3-sentence executive summary\n"
            f"2. Key topics discussed (bullet points)\n"
            f"3. Any decisions or action items mentioned\n\n"
            f"CONVERSATION:\n{formatted}\n\n"
            f"Respond in a structured, professional format."
        )
        return self.client.generate(prompt, system=system)

    # ----------------------------------------------------------
    # 2. SMART REPLY SUGGESTIONS
    # ----------------------------------------------------------
    def suggest_replies(self, conversation: list[dict]) -> list[str]:
        """
        Generate NUM_SUGGESTIONS contextual reply options.
        Returns a list of suggestion strings.
        """
        formatted = self._format_conversation(conversation)
        last_msg  = conversation[-1]["text"] if conversation else ""

        system = (
            "You are a smart messaging assistant that suggests "
            "natural, context-aware replies."
        )
        prompt = (
            f"Given this conversation, suggest exactly {NUM_SUGGESTIONS} "
            f"different reply options for the last message.\n\n"
            f"CONVERSATION:\n{formatted}\n\n"
            f"LAST MESSAGE: {last_msg}\n\n"
            f"Rules:\n"
            f"- Each reply must be distinct in tone (e.g. formal, casual, brief)\n"
            f"- Each reply must be on its own line\n"
            f"- Prefix each with its number: 1. 2. 3.\n"
            f"- No extra explanation, just the {NUM_SUGGESTIONS} replies."
        )
        raw = self.client.generate(prompt, system=system)
        return self._parse_numbered_list(raw, NUM_SUGGESTIONS)

    # ----------------------------------------------------------
    # 3. AUTO-REPLY BOT
    # ----------------------------------------------------------
    def auto_reply(self, conversation: list[dict], persona: str = "assistant") -> str:
        """
        Generate a single smart auto-reply to the last message.
        persona: short description of the bot's role/tone.
        """
        formatted = self._format_conversation(conversation)
        last_msg  = conversation[-1] if conversation else {}

        system = (
            f"You are an intelligent auto-reply bot acting as: {persona}. "
            "You write helpful, natural, context-aware replies."
        )
        prompt = (
            f"CONVERSATION HISTORY:\n{formatted}\n\n"
            f"LAST MESSAGE FROM {last_msg.get('sender','User')}: "
            f"{last_msg.get('text','')}\n\n"
            f"Write ONE professional and natural auto-reply. "
            f"Do NOT add any explanation or prefix — just the reply message."
        )
        return self.client.generate(prompt, system=system)

    # ----------------------------------------------------------
    # 4. SENTIMENT ANALYSIS
    # ----------------------------------------------------------
    def analyze_sentiment(self, conversation: list[dict]) -> dict:
        """
        Analyse the emotional tone of the entire conversation.
        Returns a dict with overall sentiment, per-speaker breakdown,
        emotional keywords, and a brief explanation.
        """
        formatted = self._format_conversation(conversation)

        system = (
            "You are an expert sentiment analysis model. "
            "You respond ONLY with valid JSON."
        )
        prompt = (
            f"Perform sentiment analysis on this conversation.\n\n"
            f"CONVERSATION:\n{formatted}\n\n"
            f"Return a JSON object with exactly these keys:\n"
            f'{{\n'
            f'  "overall": "positive|negative|neutral|mixed",\n'
            f'  "score": <float between -1.0 and 1.0>,\n'
            f'  "tone": "<one-word tone description>",\n'
            f'  "per_speaker": {{"<name>": "positive|negative|neutral", ...}},\n'
            f'  "keywords": ["<emotion word>", ...],\n'
            f'  "explanation": "<2-sentence explanation>"\n'
            f'}}\n\n'
            f"Return ONLY the JSON object, no markdown, no extra text."
        )
        raw = self.client.generate(prompt, system=system)
        return self._parse_json_response(raw)

    # ----------------------------------------------------------
    # INTERNAL HELPERS
    # ----------------------------------------------------------
    def _format_conversation(self, conversation: list[dict]) -> str:
        lines = []
        for msg in conversation:
            sender = msg.get("sender", "Unknown")
            text   = msg.get("text", "")
            ts     = msg.get("time", "")
            line   = f"[{ts}] {sender}: {text}" if ts else f"{sender}: {text}"
            lines.append(line)
        return "\n".join(lines)

    def _parse_numbered_list(self, text: str, count: int) -> list[str]:
        """Extract numbered items from model output."""
        results = []
        for line in text.splitlines():
            line = line.strip()
            match = re.match(r"^\d+[\.\)]\s*(.+)", line)
            if match:
                results.append(match.group(1).strip())
        # Fallback: split by newline if regex found nothing
        if not results:
            results = [l.strip() for l in text.splitlines() if l.strip()]
        return results[:count]

    def _parse_json_response(self, text: str) -> dict:
        """Safely extract JSON from model response."""
        # Strip markdown code fences if present
        text = re.sub(r"```(?:json)?", "", text).strip().rstrip("`").strip()
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            # Try to find a JSON block inside the text
            match = re.search(r"\{.*\}", text, re.DOTALL)
            if match:
                try:
                    return json.loads(match.group())
                except Exception:
                    pass
            return {
                "overall": "unknown", "score": 0.0, "tone": "unknown",
                "per_speaker": {}, "keywords": [],
                "explanation": "Could not parse sentiment response.",
            }

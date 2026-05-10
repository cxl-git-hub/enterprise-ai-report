"""Prompt injection detection guard."""

import re
from typing import List


class PromptGuard:
    """Detects prompt injection attempts in user input."""

    # Patterns that indicate prompt injection attempts
    INJECTION_PATTERNS = [
        # System prompt override attempts
        r"ignore\s+(all\s+)?(previous|prior|above)\s+(instructions|prompts|rules)",
        r"disregard\s+(all\s+)?(previous|prior|above)\s+(instructions|prompts|rules)",
        r"forget\s+(all\s+)?(previous|prior|above)\s+(instructions|prompts|rules)",
        r"override\s+(all\s+)?(previous|prior|above)\s+(instructions|prompts|rules)",

        # Role manipulation
        r"you\s+are\s+now\s+",
        r"act\s+as\s+(if\s+)?you\s+are",
        r"pretend\s+you\s+are",
        r"roleplay\s+as",
        r"system\s*:\s*you\s+are",

        # Instruction injection
        r"new\s+instructions?\s*:",
        r"updated\s+instructions?\s*:",
        r"system\s+prompt\s*:",
        r"\[system\]",
        r"\[INST\]",
        r"<<SYS>>",

        # Jailbreak patterns
        r"do\s+anything\s+now",
        r"d\s*a\s*n\s*mode",
        r"developer\s+mode",
        r"jailbreak",
        r"bypass\s+(all\s+)?(safety|security|restrictions|filters)",

        # Data exfiltration attempts
        r"repeat\s+(all\s+)?(the\s+)?(system\s+)?(prompt|instructions)",
        r"show\s+me\s+(your|the)\s+(system\s+)?(prompt|instructions)",
        r"what\s+(are|is)\s+your\s+(system\s+)?(prompt|instructions)",
        r"output\s+(your|the)\s+(system\s+)?(prompt|instructions)",
        r"reveal\s+(your|the)\s+(system\s+)?(prompt|instructions)",

        # Code execution attempts
        r"execute\s+(this\s+)?(code|command|script)",
        r"run\s+(this\s+)?(code|command|script)",
        r"eval\s*\(",
        r"exec\s*\(",
        r"__import__",
        r"subprocess",
        r"os\.system",
    ]

    # Suspicious keywords that may indicate injection when combined
    SUSPICIOUS_KEYWORDS = [
        "ignore", "override", "bypass", "jailbreak", "DAN",
        "developer mode", "system prompt", "instructions",
    ]

    def __init__(self, sensitivity: str = "medium"):
        """
        Initialize the prompt guard.

        Args:
            sensitivity: Detection sensitivity level ('low', 'medium', 'high')
        """
        self.sensitivity = sensitivity
        self._compiled_patterns = [
            re.compile(pattern, re.IGNORECASE) for pattern in self.INJECTION_PATTERNS
        ]

    def detect_injection(self, text: str) -> bool:
        """
        Detect if the input text contains prompt injection attempts.

        Args:
            text: The user input to check

        Returns:
            True if injection is detected, False otherwise
        """
        if not text:
            return False

        # Check against compiled patterns
        for pattern in self._compiled_patterns:
            if pattern.search(text):
                return True

        # High sensitivity: check for suspicious keyword combinations
        if self.sensitivity == "high":
            text_lower = text.lower()
            keyword_count = sum(
                1 for kw in self.SUSPICIOUS_KEYWORDS
                if kw.lower() in text_lower
            )
            if keyword_count >= 2:
                return True

        return False

    def get_injection_details(self, text: str) -> List[str]:
        """
        Get details about detected injection patterns.

        Args:
            text: The user input to check

        Returns:
            List of detected injection pattern descriptions
        """
        if not text:
            return []

        details = []
        for pattern in self._compiled_patterns:
            match = pattern.search(text)
            if match:
                details.append(f"Pattern matched: '{match.group()}' at position {match.start()}")

        return details

    def sanitize_input(self, text: str) -> str:
        """
        Sanitize user input by removing potential injection patterns.

        Args:
            text: The user input to sanitize

        Returns:
            Sanitized text
        """
        if not text:
            return text

        sanitized = text
        for pattern in self._compiled_patterns:
            sanitized = pattern.sub("[REMOVED]", sanitized)

        return sanitized

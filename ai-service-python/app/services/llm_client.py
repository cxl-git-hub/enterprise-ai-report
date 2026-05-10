"""
Unified LLM Client with multi-provider support.

Supports:
- OpenAI (and compatible APIs: DeepSeek, Moonshot, Zhipu GLM, etc.)
- Anthropic / Claude
- Ollama (local models)
- vLLM (local models, OpenAI-compatible)
- Azure OpenAI
- Custom OpenAI-compatible endpoints

Configuration via environment variables:
    LLM_PROVIDER: openai | anthropic | ollama | vllm | azure | custom
    LLM_API_KEY: API key (not needed for Ollama)
    LLM_BASE_URL: API endpoint base URL
    LLM_MODEL: Model name/ID
    LLM_MAX_TOKENS: Max output tokens
    LLM_TEMPERATURE: Sampling temperature
    LLM_API_VERSION: API version (for Azure)
    LLM_EXTRA_HEADERS: JSON string of extra headers
"""

import json
import logging
from typing import Any, Dict, List, Optional, AsyncIterator

import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)

# Provider-specific defaults
PROVIDER_DEFAULTS = {
    "openai": {
        "base_url": "https://api.openai.com/v1",
        "model": "gpt-4o",
        "api_path": "/chat/completions",
    },
    "deepseek": {
        "base_url": "https://api.deepseek.com/v1",
        "model": "deepseek-chat",
        "api_path": "/chat/completions",
    },
    "zhipu": {
        "base_url": "https://open.bigmodel.cn/api/paas/v4",
        "model": "glm-4",
        "api_path": "/chat/completions",
    },
    "moonshot": {
        "base_url": "https://api.moonshot.cn/v1",
        "model": "moonshot-v1-8k",
        "api_path": "/chat/completions",
    },
    "qwen": {
        "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "model": "qwen-max",
        "api_path": "/chat/completions",
    },
    "ollama": {
        "base_url": "http://localhost:11434",
        "model": "llama3",
        "api_path": "/api/chat",
    },
    "vllm": {
        "base_url": "http://localhost:8000/v1",
        "model": "default",
        "api_path": "/chat/completions",
    },
    "azure": {
        "base_url": "https://YOUR_RESOURCE.openai.azure.com",
        "model": "gpt-4",
        "api_path": "/openai/deployments/{model}/chat/completions",
    },
    "anthropic": {
        "base_url": "https://api.anthropic.com",
        "model": "claude-sonnet-4-20250514",
        "api_path": "/v1/messages",
    },
}


class LLMClientError(Exception):
    """LLM client error."""
    pass


class LLMClient:
    """Unified LLM client supporting multiple providers."""

    def __init__(
        self,
        provider: Optional[str] = None,
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
        model: Optional[str] = None,
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        timeout: float = 120.0,
    ):
        self.provider = (provider or settings.LLM_PROVIDER or "openai").lower()
        defaults = PROVIDER_DEFAULTS.get(self.provider, PROVIDER_DEFAULTS["openai"])

        self.api_key = api_key or settings.LLM_API_KEY or ""
        self.base_url = (base_url or settings.LLM_BASE_URL or defaults["base_url"]).rstrip("/")
        self.model = model or settings.LLM_MODEL or defaults["model"]
        self.max_tokens = max_tokens or settings.LLM_MAX_TOKENS or 4096
        self.temperature = temperature if temperature is not None else (settings.LLM_TEMPERATURE or 0.1)
        self.timeout = timeout
        self.api_path = defaults["api_path"]

        # Extra headers from config
        self.extra_headers: Dict[str, str] = {}
        extra = getattr(settings, "LLM_EXTRA_HEADERS", None)
        if extra:
            try:
                self.extra_headers = json.loads(extra) if isinstance(extra, str) else extra
            except (json.JSONDecodeError, TypeError):
                pass

    def _build_headers(self) -> Dict[str, str]:
        """Build request headers based on provider."""
        headers = {"Content-Type": "application/json"}

        if self.provider == "anthropic":
            headers["x-api-key"] = self.api_key
            headers["anthropic-version"] = "2023-06-01"
        elif self.provider == "azure":
            headers["api-key"] = self.api_key
        else:
            # OpenAI-compatible (openai, deepseek, zhipu, moonshot, qwen, ollama, vllm, custom)
            if self.api_key:
                headers["Authorization"] = f"Bearer {self.api_key}"

        headers.update(self.extra_headers)
        return headers

    def _build_url(self) -> str:
        """Build the full API URL."""
        if self.provider == "azure":
            api_version = getattr(settings, "LLM_API_VERSION", "2024-02-01")
            path = self.api_path.replace("{model}", self.model)
            return f"{self.base_url}{path}?api-version={api_version}"
        return f"{self.base_url}{self.api_path}"

    def _build_request_body(
        self,
        messages: List[Dict[str, str]],
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        **kwargs,
    ) -> Dict[str, Any]:
        """Build request body based on provider."""
        max_tokens = max_tokens or self.max_tokens
        temperature = temperature if temperature is not None else self.temperature

        if self.provider == "anthropic":
            # Anthropic uses a different message format
            system_msg = ""
            user_messages = []
            for msg in messages:
                if msg["role"] == "system":
                    system_msg = msg["content"]
                else:
                    user_messages.append(msg)

            body = {
                "model": self.model,
                "max_tokens": max_tokens,
                "temperature": temperature,
                "messages": user_messages,
            }
            if system_msg:
                body["system"] = system_msg

        elif self.provider == "ollama":
            # Ollama format
            body = {
                "model": self.model,
                "messages": messages,
                "stream": False,
                "options": {
                    "num_predict": max_tokens,
                    "temperature": temperature,
                },
            }

        else:
            # OpenAI-compatible format (openai, deepseek, zhipu, moonshot, qwen, vllm, azure, custom)
            body = {
                "model": self.model,
                "messages": messages,
                "max_tokens": max_tokens,
                "temperature": temperature,
            }

        # Apply any extra kwargs
        body.update(kwargs)
        return body

    def _extract_response(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """Extract standardized response from provider-specific format."""
        if self.provider == "anthropic":
            content = data.get("content", [{}])
            text = content[0].get("text", "") if content else ""
            usage = data.get("usage", {})
            return {
                "content": text,
                "model": data.get("model", self.model),
                "prompt_tokens": usage.get("input_tokens", 0),
                "completion_tokens": usage.get("output_tokens", 0),
                "total_tokens": usage.get("input_tokens", 0) + usage.get("output_tokens", 0),
                "finish_reason": data.get("stop_reason", "end_turn"),
                "raw": data,
            }

        elif self.provider == "ollama":
            msg = data.get("message", {})
            return {
                "content": msg.get("content", ""),
                "model": data.get("model", self.model),
                "prompt_tokens": data.get("prompt_eval_count", 0),
                "completion_tokens": data.get("eval_count", 0),
                "total_tokens": data.get("prompt_eval_count", 0) + data.get("eval_count", 0),
                "finish_reason": "stop" if data.get("done") else "length",
                "raw": data,
            }

        else:
            # OpenAI-compatible
            choice = data.get("choices", [{}])[0]
            message = choice.get("message", {})
            usage = data.get("usage", {})
            return {
                "content": message.get("content", ""),
                "model": data.get("model", self.model),
                "prompt_tokens": usage.get("prompt_tokens", 0),
                "completion_tokens": usage.get("completion_tokens", 0),
                "total_tokens": usage.get("total_tokens", 0),
                "finish_reason": choice.get("finish_reason", "stop"),
                "raw": data,
            }

    async def chat(
        self,
        messages: List[Dict[str, str]],
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        **kwargs,
    ) -> Dict[str, Any]:
        """Send a chat completion request."""
        url = self._build_url()
        headers = self._build_headers()
        body = self._build_request_body(messages, max_tokens, temperature, **kwargs)

        logger.debug(f"LLM request: provider={self.provider}, model={self.model}, url={url}")

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.post(url, headers=headers, json=body)
                response.raise_for_status()
                data = response.json()
            except httpx.HTTPStatusError as e:
                logger.error(f"LLM API error: {e.response.status_code} - {e.response.text}")
                raise LLMClientError(f"LLM API error ({e.response.status_code}): {e.response.text[:500]}")
            except httpx.RequestError as e:
                logger.error(f"LLM request error: {e}")
                raise LLMClientError(f"LLM request failed: {str(e)}")

        result = self._extract_response(data)
        logger.debug(
            f"LLM response: tokens={result['total_tokens']}, "
            f"finish={result['finish_reason']}"
        )
        return result

    async def chat_simple(
        self,
        system_prompt: str,
        user_prompt: str,
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
    ) -> str:
        """Simple chat that returns just the text content."""
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]
        result = await self.chat(messages, max_tokens, temperature)
        return result["content"]


def get_llm_client(
    provider: Optional[str] = None,
    **kwargs,
) -> LLMClient:
    """Factory function to get an LLM client instance."""
    return LLMClient(provider=provider, **kwargs)

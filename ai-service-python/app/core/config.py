"""Application configuration using Pydantic Settings."""

from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Application
    APP_NAME: str = "ai-service"
    APP_ENV: str = "development"
    DEBUG: bool = True
    LOG_LEVEL: str = "INFO"

    # Database
    DATABASE_URL: str = "mysql+asyncmy://report:report123456@localhost:3306/ai_report"

    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"

    # MinIO
    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET: str = "ai-reports"
    MINIO_SECURE: bool = False

    # LLM
    LLM_PROVIDER: str = "openai"
    LLM_API_KEY: str = ""
    LLM_BASE_URL: str = "https://api.openai.com/v1"
    LLM_MODEL: str = "gpt-4o"
    LLM_MAX_TOKENS: int = 4096
    LLM_TEMPERATURE: float = 0.1

    # JWT
    JWT_SECRET_KEY: str = "your-secret-key-change-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_MINUTES: int = 1440

    # Safety
    SQL_MAX_ROWS: int = 10000
    SQL_MAX_JOINS: int = 3
    PROMPT_MAX_TOKENS: int = 128000

    # Cost Tracking
    COST_PER_1K_PROMPT_TOKENS: float = 0.005
    COST_PER_1K_COMPLETION_TOKENS: float = 0.015

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()

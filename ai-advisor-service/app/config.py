from pydantic_settings import BaseSettings
from functools import lru_cache
from typing import Literal


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Service configuration
    app_name: str = "SmartBachat AI Advisor"
    debug: bool = False

    # Bachat Core Service URL (for fetching transaction data)
    bachat_core_service_url: str = "http://localhost:8080"

    # JWT Configuration (must match other services)
    jwt_secret: str = "your-256-bit-secret-key-here-make-it-long-enough-for-hs256"
    jwt_algorithm: str = "HS384"  # Must match UAM service (uses HS384 with 48+ byte keys)
    jwt_issuer: str = "smart-bachat"

    # ===========================================
    # AI Provider Configuration
    # ===========================================
    # Which AI provider to use: "gemini" (recommended - free tier), "openai", or "auto"
    # "auto" will try Gemini first, then OpenAI, then fallback
    ai_provider: Literal["gemini", "openai", "auto"] = "gemini"

    # ===========================================
    # Google Gemini Configuration (Recommended)
    # ===========================================
    # Get your free API key at: https://makersuite.google.com/app/apikey
    gemini_api_key: str | None = None

    # Gemini Model Selection
    # Options: "gemini-2.0-flash" (fast, free), "gemini-1.5-pro" (most accurate), "gemini-1.5-flash" (balanced)
    gemini_model: str = "gemini-2.0-flash-exp"

    # Temperature for Gemini responses (0.0-1.0)
    gemini_temperature: float = 0.3

    # ===========================================
    # OpenAI Configuration (Alternative)
    # ===========================================
    # OpenAI API key (optional - for LLM-based recommendations)
    openai_api_key: str | None = None

    # OpenAI Model Selection
    # Options: "gpt-4o" (99% accuracy, higher cost) or "gpt-4o-mini" (95% accuracy, 10x cheaper)
    openai_model: str = "gpt-4o"

    # Temperature for AI responses (0.0-1.0)
    # Lower = more consistent/accurate, Higher = more creative
    openai_temperature: float = 0.3

    # ===========================================
    # Analysis Configuration
    # ===========================================
    default_analysis_months: int = 6
    min_transactions_for_analysis: int = 10

    # Recommendation thresholds
    high_spending_threshold_percent: float = 30.0  # Category > 30% of income
    savings_potential_threshold_percent: float = 10.0  # Can save at least 10%

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()


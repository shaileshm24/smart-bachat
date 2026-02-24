"""
Unified AI Advisor that supports multiple AI providers (Gemini, OpenAI).
Provides automatic fallback between providers.
"""
import logging
from typing import List

from app.config import get_settings
from app.models.schemas import (
    TransactionData, SpendingAnalysisResponse, SavingsCapacityResponse, GoalData
)
from app.services.openai_advisor import openai_advisor, AIInsightResponse
from app.services.gemini_advisor import gemini_advisor

logger = logging.getLogger(__name__)


class UnifiedAIAdvisor:
    """
    Unified AI advisor that supports multiple providers.

    Provider selection:
    - "gemini": Use Gemini only (recommended - free tier available)
    - "openai": Use OpenAI only
    - "auto": Try Gemini first, then OpenAI, then fallback
    """

    def __init__(self):
        self.settings = get_settings()
        self._log_provider_status()

    def _log_provider_status(self):
        """Log which AI providers are available."""
        provider = self.settings.ai_provider
        gemini_available = gemini_advisor.is_available()
        openai_available = openai_advisor.is_available()

        logger.info(f"AI Provider Configuration: {provider}")
        logger.info(f"  - Gemini: {'✓ Available' if gemini_available else '✗ Not configured'}")
        logger.info(f"  - OpenAI: {'✓ Available' if openai_available else '✗ Not configured'}")

    def is_available(self) -> bool:
        """Check if any AI provider is available."""
        provider = self.settings.ai_provider

        if provider == "gemini":
            return gemini_advisor.is_available()
        elif provider == "openai":
            return openai_advisor.is_available()
        else:  # auto
            return gemini_advisor.is_available() or openai_advisor.is_available()

    def get_active_provider(self) -> str:
        """Get the name of the active AI provider."""
        provider = self.settings.ai_provider

        if provider == "gemini" and gemini_advisor.is_available():
            return "gemini"
        elif provider == "openai" and openai_advisor.is_available():
            return "openai"
        elif provider == "auto":
            if gemini_advisor.is_available():
                return "gemini"
            elif openai_advisor.is_available():
                return "openai"
        return "fallback"

    async def generate_insights(
        self,
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse,
        transactions: List[TransactionData]
    ) -> AIInsightResponse:
        """Generate personalized insights using the configured AI provider."""
        provider = self.settings.ai_provider

        if provider == "gemini":
            return await self._try_gemini_insights(analysis, capacity, transactions)
        elif provider == "openai":
            return await self._try_openai_insights(analysis, capacity, transactions)
        else:  # auto - try Gemini first, then OpenAI
            result = await self._try_gemini_insights(analysis, capacity, transactions)
            if result is None:
                result = await self._try_openai_insights(analysis, capacity, transactions)
            return result or openai_advisor._generate_fallback_insights(analysis, capacity)

    async def _try_gemini_insights(
        self,
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse,
        transactions: List[TransactionData]
    ) -> AIInsightResponse | None:
        """Try to get insights from Gemini."""
        if not gemini_advisor.is_available():
            logger.info("Gemini not available, skipping...")
            return None
        try:
            logger.info("Using Gemini for insights...")
            return await gemini_advisor.generate_insights(analysis, capacity, transactions)
        except Exception as e:
            logger.error(f"Gemini failed: {e}")
            return None

    async def _try_openai_insights(
        self,
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse,
        transactions: List[TransactionData]
    ) -> AIInsightResponse | None:
        """Try to get insights from OpenAI."""
        if not openai_advisor.is_available():
            logger.info("OpenAI not available, skipping...")
            return None
        try:
            logger.info("Using OpenAI for insights...")
            return await openai_advisor.generate_insights(analysis, capacity, transactions)
        except Exception as e:
            logger.error(f"OpenAI failed: {e}")
            return None

    async def generate_goal_advice(
        self,
        goal,  # GoalRecommendationRequest
        capacity: SavingsCapacityResponse,
        analysis: SpendingAnalysisResponse
    ) -> dict:
        """Generate personalized advice for a specific goal using the configured provider."""
        provider = self.settings.ai_provider
        logger.info(f"Generating goal advice using provider: {provider}")

        if provider == "gemini":
            result = await self._try_gemini_goal_advice(goal, capacity, analysis)
            if result:
                return {"advice": result, "source": "gemini"}
            return {"advice": gemini_advisor._generate_fallback_goal_advice(goal, capacity), "source": "fallback"}
        elif provider == "openai":
            result = await self._try_openai_goal_advice(goal, capacity, analysis)
            if result:
                return {"advice": result, "source": "openai"}
            return {"advice": openai_advisor._generate_fallback_goal_advice(goal, capacity), "source": "fallback"}
        else:  # auto - try Gemini first, then OpenAI
            result = await self._try_gemini_goal_advice(goal, capacity, analysis)
            if result:
                return {"advice": result, "source": "gemini"}
            openai_result = await self._try_openai_goal_advice(goal, capacity, analysis)
            if openai_result:
                return {"advice": openai_result, "source": "openai"}
            return {"advice": openai_advisor._generate_fallback_goal_advice(goal, capacity), "source": "fallback"}

    async def _try_gemini_goal_advice(self, goal, capacity, analysis) -> str | None:
        """Try to get goal advice from Gemini."""
        if not gemini_advisor.is_available():
            logger.warning("Gemini not available for goal advice")
            return None
        try:
            logger.info("Calling Gemini for goal advice...")
            result = await gemini_advisor.generate_goal_advice(goal, capacity, analysis)
            logger.info(f"Gemini goal advice received: {result[:100] if result else 'None'}...")
            return result
        except Exception as e:
            logger.error(f"Gemini goal advice failed: {e}")
            return None

    async def _try_openai_goal_advice(self, goal, capacity, analysis) -> str | None:
        """Try to get goal advice from OpenAI."""
        if not openai_advisor.is_available():
            logger.warning("OpenAI not available for goal advice")
            return None
        try:
            logger.info("Calling OpenAI for goal advice...")
            result = await openai_advisor.generate_goal_advice(goal, capacity, analysis)
            logger.info(f"OpenAI goal advice received: {result[:100] if result else 'None'}...")
            return result
        except Exception as e:
            logger.error(f"OpenAI goal advice failed: {e}")
            return None

    async def generate_ai_goal_insights(
        self,
        goals: List[GoalData],
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse
    ) -> dict:
        """Generate AI-powered goal insights using the configured provider."""
        provider = self.settings.ai_provider

        if provider == "gemini":
            result = await self._try_gemini_goal_insights(goals, analysis, capacity)
            return result or gemini_advisor._generate_fallback_goal_insights(goals, capacity)
        elif provider == "openai":
            result = await self._try_openai_goal_insights(goals, analysis, capacity)
            return result or openai_advisor._generate_fallback_goal_insights(goals, capacity)
        else:  # auto - try Gemini first, then OpenAI
            result = await self._try_gemini_goal_insights(goals, analysis, capacity)
            if result is None or result.get("score_source") == "fallback":
                openai_result = await self._try_openai_goal_insights(goals, analysis, capacity)
                if openai_result and openai_result.get("score_source") != "fallback":
                    return openai_result
            return result or openai_advisor._generate_fallback_goal_insights(goals, capacity)

    async def _try_gemini_goal_insights(
        self,
        goals: List[GoalData],
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse
    ) -> dict | None:
        """Try to get goal insights from Gemini."""
        if not gemini_advisor.is_available():
            logger.info("Gemini not available for goal insights, skipping...")
            return None
        try:
            logger.info("Using Gemini for goal insights...")
            return await gemini_advisor.generate_ai_goal_insights(goals, analysis, capacity)
        except Exception as e:
            logger.error(f"Gemini goal insights failed: {e}")
            return None

    async def _try_openai_goal_insights(
        self,
        goals: List[GoalData],
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse
    ) -> dict | None:
        """Try to get goal insights from OpenAI."""
        if not openai_advisor.is_available():
            logger.info("OpenAI not available for goal insights, skipping...")
            return None
        try:
            logger.info("Using OpenAI for goal insights...")
            return await openai_advisor.generate_ai_goal_insights(goals, analysis, capacity)
        except Exception as e:
            logger.error(f"OpenAI goal insights failed: {e}")
            return None


# Singleton instance
ai_advisor = UnifiedAIAdvisor()

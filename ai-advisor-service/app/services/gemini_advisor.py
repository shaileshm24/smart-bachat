"""
Google Gemini-powered financial advisor for personalized recommendations.
Uses the new google-genai package (recommended by Google).
"""
import json
import logging
import re
from typing import List, Optional

from pydantic import BaseModel

from app.config import get_settings
from app.models.schemas import (
    TransactionData, SpendingAnalysisResponse, SavingsCapacityResponse,
    GoalData
)

logger = logging.getLogger(__name__)


def extract_json_from_response(text: str) -> dict:
    """Extract and parse JSON from AI response, handling common issues."""
    if not text:
        raise ValueError("Empty response")

    # Try direct parsing first
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    # Try to extract JSON from markdown code blocks
    json_match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', text)
    if json_match:
        try:
            return json.loads(json_match.group(1))
        except json.JSONDecodeError:
            pass

    # Try to find JSON object in the text
    json_match = re.search(r'\{[\s\S]*\}', text)
    if json_match:
        try:
            return json.loads(json_match.group(0))
        except json.JSONDecodeError:
            pass

    # Try to fix truncated JSON
    cleaned = text.strip()

    # Fix truncated strings by finding unclosed quotes and closing them
    # Count quotes to see if we have an unclosed string
    quote_count = cleaned.count('"') - cleaned.count('\\"')
    if quote_count % 2 == 1:
        # Odd number of quotes - find the last unclosed string and truncate it
        # Find the last quote that starts a string value
        last_quote_pos = cleaned.rfind('"')
        if last_quote_pos > 0:
            # Truncate at the last complete key-value pair
            # Look for the last complete structure before the truncation
            cleaned = cleaned[:last_quote_pos] + '..."'

    # Count braces and brackets
    open_braces = cleaned.count('{')
    close_braces = cleaned.count('}')
    open_brackets = cleaned.count('[')
    close_brackets = cleaned.count(']')

    # Add missing closing brackets and braces
    if open_brackets > close_brackets:
        cleaned += ']' * (open_brackets - close_brackets)
    if open_braces > close_braces:
        cleaned += '}' * (open_braces - close_braces)

    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        pass

    # Last resort: try to extract just the essential fields
    logger.warning(f"Attempting to extract partial JSON from truncated response")
    try:
        # Try to find overall_health_score at minimum
        score_match = re.search(r'"overall_health_score"\s*:\s*(\d+)', text)
        if score_match:
            return {
                "overall_health_score": int(score_match.group(1)),
                "goal_specific_insights": [],
                "motivational_message": "Keep saving! You're making progress.",
                "saving_tips": ["Set up automatic transfers", "Track your expenses"],
                "suggested_new_goals": [],
                "score_source": "partial"
            }
    except Exception:
        pass

    raise ValueError(f"Could not parse JSON from response: {text[:200]}...")


class AIRecommendation(BaseModel):
    """AI-generated recommendation."""
    title: str
    message: str
    priority: str  # HIGH, MEDIUM, LOW
    action_type: str  # REDUCE_SPENDING, INCREASE_SAVINGS, INVEST, etc.
    category: Optional[str] = None
    potential_savings: Optional[float] = None
    confidence: float = 0.8


class AIInsightResponse(BaseModel):
    """Response from AI advisor."""
    summary: str
    recommendations: List[AIRecommendation]
    personalized_tips: List[str]
    financial_health_score: int  # 0-100


class GeminiAdvisor:
    """Google Gemini-powered financial advisor using google-genai package."""

    def __init__(self):
        self.settings = get_settings()
        self.client = None
        self.model_name = None
        self._initialize_client()

    def _initialize_client(self):
        """Initialize Gemini client if API key is available."""
        if self.settings.gemini_api_key and self.settings.gemini_api_key != "your-gemini-api-key-here":
            try:
                from google import genai
                self.client = genai.Client(api_key=self.settings.gemini_api_key)
                self.model_name = self.settings.gemini_model
                logger.info(f"Gemini initialized with model: {self.model_name}")
            except Exception as e:
                logger.error(f"Failed to initialize Gemini: {e}")
                self.client = None

    def is_available(self) -> bool:
        """Check if Gemini is configured."""
        return self.client is not None

    async def generate_insights(
        self,
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse,
        transactions: List[TransactionData]
    ) -> AIInsightResponse:
        """Generate personalized insights using Gemini."""
        if not self.is_available():
            return self._generate_fallback_insights(analysis, capacity)

        spending_summary = self._prepare_spending_summary(analysis, transactions)

        prompt = f"""You are a friendly Indian financial advisor helping a user manage their money better.
Analyze this spending data and provide personalized, actionable advice in a warm, encouraging tone.

SPENDING SUMMARY:
- Total Income: ₹{analysis.total_income:,.0f}
- Total Expenses: ₹{analysis.total_expenses:,.0f}
- Net Savings: ₹{analysis.net_savings:,.0f}
- Savings Rate: {analysis.savings_rate:.1f}%
- Monthly Average Income: ₹{analysis.avg_monthly_income:,.0f}
- Monthly Average Expense: ₹{analysis.avg_monthly_expense:,.0f}

TOP SPENDING CATEGORIES:
{spending_summary}

SAVINGS CAPACITY:
- Safe Monthly Savings: ₹{capacity.safe_monthly_savings:,.0f}
- Current Savings Rate: {capacity.current_savings_rate:.1f}%
- Essential Expenses: ₹{capacity.avg_monthly_essential_expenses:,.0f}
- Discretionary Expenses: ₹{capacity.avg_monthly_discretionary_expenses:,.0f}

Provide your response as JSON with this exact structure:
{{
    "summary": "2-3 sentence overall assessment",
    "recommendations": [
        {{
            "title": "Short title",
            "message": "Detailed actionable advice (2-3 sentences)",
            "priority": "HIGH/MEDIUM/LOW",
            "action_type": "REDUCE_SPENDING/INCREASE_SAVINGS/INVEST/BUDGET/EMERGENCY_FUND",
            "category": "category if applicable or null",
            "potential_savings": number or null
        }}
    ],
    "personalized_tips": ["tip1", "tip2", "tip3"],
    "financial_health_score": 0-100
}}

Focus on:
1. Specific categories where spending can be reduced
2. Realistic savings targets based on their income
3. Indian context (mention UPI, SIPs, FDs, PPF, NPS where relevant)
4. Encouraging tone - celebrate what they're doing well"""

        try:
            from google.genai import types
            logger.info("Calling Gemini for financial insights...")

            response = await self.client.aio.models.generate_content(
                model=self.model_name,
                contents=prompt,
                config=types.GenerateContentConfig(
                    temperature=self.settings.gemini_temperature,
                    max_output_tokens=4096,
                    response_mime_type="application/json",
                )
            )
            result = extract_json_from_response(response.text)
            logger.info(f"Gemini response received. Keys: {list(result.keys())}")

            recommendations = [
                AIRecommendation(
                    title=r.get("title", "Recommendation"),
                    message=r.get("message", ""),
                    priority=r.get("priority", "MEDIUM"),
                    action_type=r.get("action_type", "BUDGET"),
                    category=r.get("category"),
                    potential_savings=r.get("potential_savings"),
                    confidence=0.85
                )
                for r in result.get("recommendations", [])
            ]

            return AIInsightResponse(
                summary=result.get("summary", ""),
                recommendations=recommendations,
                personalized_tips=result.get("personalized_tips", []),
                financial_health_score=result.get("financial_health_score", 50)
            )

        except Exception as e:
            logger.error(f"Gemini API error: {type(e).__name__}: {e}")
            return self._generate_fallback_insights(analysis, capacity)

    def _prepare_spending_summary(
        self, analysis: SpendingAnalysisResponse, transactions: List[TransactionData]
    ) -> str:
        """Prepare spending summary for the prompt."""
        lines = []
        for cat in analysis.category_breakdown[:8]:
            lines.append(f"- {cat.category}: ₹{cat.total_amount:,.0f} ({cat.percent_of_total:.1f}% of expenses)")
        return "\n".join(lines)

    def _generate_fallback_insights(
        self, analysis: SpendingAnalysisResponse, capacity: SavingsCapacityResponse
    ) -> AIInsightResponse:
        """Generate rule-based insights when Gemini is unavailable."""
        recommendations = []
        tips = []
        score = 50

        if analysis.savings_rate >= 30:
            score += 25
            tips.append("Excellent savings rate! Consider investing surplus in SIPs for long-term growth.")
        elif analysis.savings_rate >= 20:
            score += 15
            tips.append("Good savings habit! Try to increase by 5% for faster goal achievement.")
        elif analysis.savings_rate >= 10:
            score += 5
            recommendations.append(AIRecommendation(
                title="Boost Your Savings",
                message=f"Your savings rate is {analysis.savings_rate:.0f}%. Aim for 20% by reducing discretionary spending.",
                priority="HIGH",
                action_type="INCREASE_SAVINGS"
            ))
        else:
            recommendations.append(AIRecommendation(
                title="Critical: Low Savings",
                message="Your savings rate is below 10%. Review expenses and create a strict budget.",
                priority="HIGH",
                action_type="BUDGET"
            ))

        if not tips:
            tips = [
                "Set up automatic transfers on salary day",
                "Use the 50-30-20 rule: 50% needs, 30% wants, 20% savings",
                "Track every expense for a week to find hidden savings"
            ]

        return AIInsightResponse(
            summary=f"Your savings rate is {analysis.savings_rate:.1f}%. Focus on building consistent savings habits.",
            recommendations=recommendations,
            personalized_tips=tips,
            financial_health_score=min(100, max(0, score))
        )

    async def generate_goal_advice(
        self,
        goal,  # GoalRecommendationRequest
        capacity: SavingsCapacityResponse,
        analysis: SpendingAnalysisResponse
    ) -> str:
        """Generate personalized advice for a specific goal using Gemini."""
        from datetime import date

        if not self.is_available():
            return self._generate_fallback_goal_advice(goal, capacity)

        remaining = goal.target_amount - goal.current_amount
        months_to_deadline = 12
        if goal.deadline:
            months_to_deadline = max(1, (goal.deadline.year - date.today().year) * 12 +
                                     goal.deadline.month - date.today().month)

        # Try up to 2 times with progressively simpler prompts
        max_retries = 2

        for attempt in range(max_retries):
            try:
                from google.genai import types

                if attempt == 0:
                    # First attempt: normal prompt
                    prompt = f"""Give 2 sentences of financial advice for saving ₹{remaining:,.0f} for "{goal.goal_name}".
Monthly savings capacity: ₹{capacity.safe_monthly_savings:,.0f}. Required: ₹{remaining/months_to_deadline:,.0f}/month.
Be encouraging. Mention SIP or RD if helpful. Keep under 50 words total."""
                else:
                    # Retry with even simpler prompt
                    prompt = f"""Short advice for saving ₹{remaining:,.0f} for {goal.goal_name}. One sentence only."""

                logger.info(f"Calling Gemini for goal advice (attempt {attempt + 1})...")

                response = await self.client.aio.models.generate_content(
                    model=self.model_name,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        temperature=0.2,  # Lower temperature for more consistent output
                        max_output_tokens=256,  # Shorter limit to avoid truncation
                    )
                )
                advice = response.text.strip() if response.text else ""
                logger.info(f"Gemini goal advice response length: {len(advice)} chars")
                logger.info(f"Gemini goal advice: {advice}")

                # Check if response is complete (ends with sentence-ending punctuation)
                if advice and advice.endswith(('.', '!', '?', '।')):
                    return advice

                # Response is truncated - try to salvage it
                if advice:
                    last_period = max(advice.rfind('.'), advice.rfind('!'), advice.rfind('?'))
                    if last_period > 20:  # At least one meaningful sentence
                        logger.warning(f"Truncated response salvaged at position {last_period}")
                        return advice[:last_period + 1]
                    else:
                        logger.warning(f"Response truncated too early, retrying...")
                        continue  # Retry with simpler prompt

            except Exception as e:
                logger.error(f"Gemini goal advice error (attempt {attempt + 1}): {type(e).__name__}: {e}")

        # All retries failed, use fallback
        logger.warning("All Gemini attempts failed, using fallback advice")
        return self._generate_fallback_goal_advice(goal, capacity)

    def _generate_fallback_goal_advice(self, goal, capacity: SavingsCapacityResponse) -> str:
        """Generate rule-based goal advice."""
        remaining = goal.target_amount - goal.current_amount
        if remaining <= 0:
            return f"Congratulations! You've achieved your {goal.goal_name} goal! Consider setting a new target."

        if capacity.safe_monthly_savings > 0 and remaining <= capacity.safe_monthly_savings:
            return f"Great news! You can complete your {goal.goal_name} goal in just one month with your current savings capacity."

        if capacity.safe_monthly_savings > 0:
            months_needed = int(remaining / capacity.safe_monthly_savings) + 1
            return f"To achieve your {goal.goal_name} goal, save ₹{capacity.recommended_monthly_savings:,.0f} monthly. You'll reach your target in about {months_needed} months."
        else:
            suggested_monthly = remaining / 12
            return f"To achieve your {goal.goal_name} goal of ₹{goal.target_amount:,.0f}, consider saving ₹{suggested_monthly:,.0f} monthly. Upload your bank statements to get personalized recommendations."

    async def generate_ai_goal_insights(
        self,
        goals: List[GoalData],
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse
    ) -> dict:
        """Generate AI-powered goal insights and motivational messages."""
        if not self.is_available():
            return self._generate_fallback_goal_insights(goals, capacity)

        goals_summary = self._prepare_goals_summary(goals)
        spending_summary = self._prepare_spending_summary(analysis, [])

        # Limit to top 3 goals to keep response short
        active_goals = [g for g in goals if g.status == "ACTIVE"][:3]
        goals_count = len(active_goals)

        prompt = f"""Indian financial advisor. Analyze goals and give JSON response.

GOALS ({goals_count}):
{goals_summary}

FINANCES: Income ₹{analysis.avg_monthly_income:,.0f}/mo, Expenses ₹{analysis.avg_monthly_expense:,.0f}/mo, Savings {analysis.savings_rate:.1f}%

Return ONLY this JSON (keep insights SHORT - max 50 words each):
{{
    "overall_health_score": <0-100 based on savings rate and goal progress>,
    "goal_specific_insights": [
        {{"goal_name": "goal1", "insight": "Short advice", "action_items": ["action1"]}}
    ],
    "motivational_message": "One encouraging sentence",
    "saving_tips": ["tip1", "tip2"],
    "suggested_new_goals": []
}}

Score guide: 80+=Excellent, 60-79=Good, 40-59=Needs work, <40=Critical
Keep ALL text fields SHORT. Max 50 words per insight."""

        max_retries = 2
        last_error = None

        for attempt in range(max_retries):
            try:
                from google.genai import types
                logger.info(f"Calling Gemini for goal insights (attempt {attempt + 1}/{max_retries})...")

                response = await self.client.aio.models.generate_content(
                    model=self.model_name,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        temperature=self.settings.gemini_temperature,
                        max_output_tokens=4096,  # Increased to avoid truncation
                        response_mime_type="application/json",
                    )
                )

                # Use helper to extract and parse JSON
                result = extract_json_from_response(response.text)
                logger.info(f"Gemini goal insights received. Keys: {list(result.keys())}")
                logger.info(f"Gemini overall_health_score: {result.get('overall_health_score')}")
                return result

            except Exception as e:
                last_error = e
                logger.warning(f"Gemini attempt {attempt + 1} failed: {type(e).__name__}: {e}")
                if attempt < max_retries - 1:
                    continue

        logger.error(f"Gemini API error after {max_retries} attempts: {type(last_error).__name__}: {last_error}")
        return self._generate_fallback_goal_insights(goals, capacity)

    def _prepare_goals_summary(self, goals: List[GoalData]) -> str:
        """Prepare goals summary for the prompt."""
        if not goals:
            return "No active goals set."

        lines = []
        for g in goals:
            if g.status == "ACTIVE":
                deadline_str = f", deadline: {g.deadline}" if g.deadline else ""
                status = "On Track ✓" if g.is_on_track else "Needs Attention ⚠"
                lines.append(
                    f"- {g.name} ({g.goal_type}): ₹{g.current_amount:,.0f}/₹{g.target_amount:,.0f} "
                    f"({g.progress_percent:.0f}% complete){deadline_str} - {status}"
                )
        return "\n".join(lines) if lines else "No active goals."

    def _generate_fallback_goal_insights(
        self, goals: List[GoalData], capacity: SavingsCapacityResponse
    ) -> dict:
        """Generate rule-based goal insights when Gemini is unavailable."""
        logger.warning("Using fallback goal insights (Gemini unavailable or failed)")

        goal_insights = []
        active_goals = [g for g in goals if g.status == "ACTIVE"]

        for g in active_goals:
            if g.progress_percent >= 75:
                insight = f"You're almost there! Just ₹{g.remaining_amount:,.0f} more to reach your {g.name} goal."
                actions = ["Make one final push!", "Consider a bonus contribution"]
            elif g.is_on_track:
                insight = f"Great progress on {g.name}! Keep up the consistent savings."
                actions = ["Maintain your current pace", "Set up auto-debit for consistency"]
            else:
                monthly_needed = g.remaining_amount / max(1, (g.days_remaining or 365) / 30)
                insight = f"Your {g.name} goal needs attention. Try to save ₹{monthly_needed:,.0f}/month."
                actions = ["Review discretionary spending", "Consider extending deadline"]

            goal_insights.append({
                "goal_name": g.name,
                "insight": insight,
                "action_items": actions
            })

        fallback_health_score = self._calculate_fallback_health_score(active_goals, capacity)

        if capacity.current_savings_rate >= 20:
            motivation = "🌟 You're a savings superstar! Your discipline is building a secure future."
        elif capacity.current_savings_rate >= 10:
            motivation = "💪 Good progress! Every rupee saved brings you closer to your dreams."
        else:
            motivation = "🎯 Small steps lead to big achievements. Start with saving just ₹500 more this month!"

        return {
            "overall_health_score": fallback_health_score,
            "score_source": "fallback",
            "goal_specific_insights": goal_insights,
            "motivational_message": motivation,
            "saving_tips": [
                "Set up automatic transfers on salary day",
                "Use the 50-30-20 rule: 50% needs, 30% wants, 20% savings",
                "Track every expense for a week to find hidden savings"
            ],
            "suggested_new_goals": []
        }

    def _calculate_fallback_health_score(
        self, active_goals: List[GoalData], capacity: SavingsCapacityResponse
    ) -> float:
        """Calculate health score when Gemini is unavailable."""
        score = 50.0

        if active_goals:
            avg_progress = sum(g.progress_percent for g in active_goals) / len(active_goals)
            score += min(25, avg_progress * 0.25)

        if active_goals:
            on_track_ratio = sum(1 for g in active_goals if g.is_on_track) / len(active_goals)
            score += on_track_ratio * 20

        if capacity.current_savings_rate > 0:
            score += min(15, capacity.current_savings_rate * 0.5)

        return max(0, min(100, round(score, 1)))


# Singleton instance
gemini_advisor = GeminiAdvisor()

"""
OpenAI-powered financial advisor for personalized recommendations.
"""
import json
from typing import List, Optional
from datetime import date

from openai import AsyncOpenAI
from pydantic import BaseModel

from app.config import get_settings
from app.models.schemas import (
    TransactionData, SpendingAnalysisResponse, SavingsCapacityResponse,
    GoalRecommendationRequest
)


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


class OpenAIAdvisor:
    """OpenAI-powered financial advisor."""
    
    def __init__(self):
        self.settings = get_settings()
        self.client = None
        if self.settings.openai_api_key:
            self.client = AsyncOpenAI(api_key=self.settings.openai_api_key)
    
    def is_available(self) -> bool:
        """Check if OpenAI is configured."""
        return self.client is not None
    
    async def generate_insights(
        self,
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse,
        transactions: List[TransactionData]
    ) -> AIInsightResponse:
        """Generate personalized insights using OpenAI."""
        if not self.is_available():
            return self._generate_fallback_insights(analysis, capacity)
        
        # Prepare spending summary for the prompt
        spending_summary = self._prepare_spending_summary(analysis, transactions)
        
        prompt = f"""You are a friendly Indian financial advisor helping a user manage their money better.
Analyze this spending data and provide personalized, actionable advice in a warm, encouraging tone.

SPENDING SUMMARY (Last {len(analysis.monthly_trend)} months):
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

Provide your response as JSON with this structure:
{{
    "summary": "2-3 sentence overall assessment",
    "recommendations": [
        {{
            "title": "Short title",
            "message": "Detailed actionable advice (2-3 sentences)",
            "priority": "HIGH/MEDIUM/LOW",
            "action_type": "REDUCE_SPENDING/INCREASE_SAVINGS/INVEST/BUDGET/EMERGENCY_FUND",
            "category": "category if applicable",
            "potential_savings": amount if applicable
        }}
    ],
    "personalized_tips": ["tip1", "tip2", "tip3"],
    "financial_health_score": 0-100
}}

Focus on:
1. Specific categories where spending can be reduced
2. Realistic savings targets based on their income
3. Indian context (mention UPI, SIPs, FDs, etc. where relevant)
4. Encouraging tone - celebrate what they're doing well
"""

        try:
            # Model selection via config:
            # - "gpt-4o": 99% accuracy, ~$2.50/1M input tokens (recommended for production)
            # - "gpt-4o-mini": 95% accuracy, ~$0.15/1M input tokens (budget option)
            response = await self.client.chat.completions.create(
                model=self.settings.openai_model,
                messages=[
                    {"role": "system", "content": "You are an expert Indian financial advisor with deep knowledge of personal finance, Indian tax laws, and investment instruments like SIPs, FDs, PPF, NPS, and mutual funds. Provide accurate, actionable advice. Always respond with valid JSON."},
                    {"role": "user", "content": prompt}
                ],
                temperature=self.settings.openai_temperature,
                max_tokens=1200,
                response_format={"type": "json_object"}
            )
            
            result = json.loads(response.choices[0].message.content)
            
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
            # Fallback to rule-based insights on error
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
        """Generate rule-based insights when OpenAI is unavailable."""
        recommendations = []
        tips = []
        score = 50
        
        # Savings rate assessment
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

        # Check high spending categories
        for cat in analysis.potential_savings_categories[:2]:
            if cat.percent_of_total > 15:
                potential = cat.total_amount * 0.2
                recommendations.append(AIRecommendation(
                    title=f"Reduce {cat.category.replace('_', ' ').title()} Spending",
                    message=f"You spent ₹{cat.total_amount:,.0f} on {cat.category.lower()}. Reducing by 20% saves ₹{potential:,.0f}.",
                    priority="MEDIUM",
                    action_type="REDUCE_SPENDING",
                    category=cat.category,
                    potential_savings=potential
                ))

        # Emergency fund tip
        if capacity.avg_monthly_essential_expenses > 0:
            emergency_target = capacity.avg_monthly_essential_expenses * 6
            tips.append(f"Build an emergency fund of ₹{emergency_target:,.0f} (6 months of essentials).")

        # Investment tip
        if analysis.savings_rate >= 15:
            tips.append("Consider starting a SIP in index funds for long-term wealth creation.")

        summary = f"Your financial health score is {score}/100. "
        if score >= 70:
            summary += "You're doing well! Focus on optimizing and growing your wealth."
        elif score >= 50:
            summary += "There's room for improvement. Small changes can make a big difference."
        else:
            summary += "Your finances need attention. Let's work on building better habits."

        return AIInsightResponse(
            summary=summary,
            recommendations=recommendations[:5],
            personalized_tips=tips[:5],
            financial_health_score=min(100, max(0, score))
        )

    async def generate_goal_advice(
        self,
        goal: GoalRecommendationRequest,
        capacity: SavingsCapacityResponse,
        analysis: SpendingAnalysisResponse
    ) -> str:
        """Generate personalized advice for a specific goal."""
        if not self.is_available():
            return self._generate_fallback_goal_advice(goal, capacity)

        remaining = goal.target_amount - goal.current_amount
        months_to_deadline = 12
        if goal.deadline:
            months_to_deadline = max(1, (goal.deadline.year - date.today().year) * 12 +
                                     goal.deadline.month - date.today().month)

        prompt = f"""As a friendly Indian financial advisor, give brief personalized advice for this savings goal:

GOAL: {goal.goal_name}
Type: {goal.goal_type}
Target: ₹{goal.target_amount:,.0f}
Saved so far: ₹{goal.current_amount:,.0f}
Remaining: ₹{remaining:,.0f}
Deadline: {goal.deadline or 'Not set'}
Months remaining: {months_to_deadline}

USER'S FINANCIAL SITUATION:
- Monthly Income: ₹{analysis.avg_monthly_income:,.0f}
- Safe Monthly Savings Capacity: ₹{capacity.safe_monthly_savings:,.0f}
- Current Savings Rate: {capacity.current_savings_rate:.1f}%

Required monthly saving: ₹{remaining/months_to_deadline:,.0f}

Provide 2-3 sentences of encouraging, actionable advice specific to this goal.
Consider Indian context (mention relevant savings instruments if applicable).
"""

        try:
            response = await self.client.chat.completions.create(
                model=self.settings.openai_model,
                messages=[
                    {"role": "system", "content": "You are an expert Indian financial advisor. Provide accurate, personalized advice considering Indian financial instruments (SIPs, FDs, RDs, PPF). Be concise and encouraging."},
                    {"role": "user", "content": prompt}
                ],
                temperature=self.settings.openai_temperature,
                max_tokens=250
            )
            return response.choices[0].message.content.strip()
        except Exception:
            return self._generate_fallback_goal_advice(goal, capacity)

    def _generate_fallback_goal_advice(
        self, goal: GoalRecommendationRequest, capacity: SavingsCapacityResponse
    ) -> str:
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
            # No transaction data available
            suggested_monthly = remaining / 12  # Assume 1 year target
            return f"To achieve your {goal.goal_name} goal of ₹{goal.target_amount:,.0f}, consider saving ₹{suggested_monthly:,.0f} monthly. Upload your bank statements to get personalized recommendations."


# Singleton instance
openai_advisor = OpenAIAdvisor()


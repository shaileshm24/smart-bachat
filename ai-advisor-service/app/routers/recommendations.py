from fastapi import APIRouter, Depends
from datetime import date, timedelta
from typing import List, Optional

from app.security import UserPrincipal, require_profile
from app.models.schemas import (
    GoalRecommendationRequest, GoalRecommendationResponse,
    InsightsResponse, InsightResponse
)
from app.services.transaction_client import transaction_client
from app.services.spending_analyzer import spending_analyzer, DISCRETIONARY_CATEGORIES
from app.services.savings_recommender import savings_recommender
from app.services.openai_advisor import openai_advisor, AIInsightResponse
from app.services.forecast_service import forecast_service, ForecastResponse
from app.config import get_settings

router = APIRouter(prefix="/api/advisor", tags=["Recommendations"])


@router.post("/goal-recommendation", response_model=GoalRecommendationResponse)
async def get_goal_recommendation(
    request: GoalRecommendationRequest,
    user: UserPrincipal = Depends(require_profile)
):
    """
    Get AI-powered recommendation for a specific savings goal.
    
    Analyzes user's spending patterns and provides:
    - Suggested monthly saving amount
    - Whether the goal is achievable by deadline
    - Adjusted deadline if needed
    - Actionable tips
    """
    end_date = date.today()
    start_date = end_date - timedelta(days=180)
    
    # Fetch transactions for analysis
    transactions = await transaction_client.get_transactions(
        token=user.token,
        start_date=start_date,
        end_date=end_date
    )
    
    # Calculate savings capacity
    capacity = savings_recommender.calculate_savings_capacity(
        transactions=transactions,
        profile_id=str(user.profile_id)
    )
    
    # Generate goal-specific recommendation
    recommendation = savings_recommender.recommend_for_goal(request, capacity)
    
    return recommendation


@router.get("/insights", response_model=InsightsResponse)
async def get_insights(
    user: UserPrincipal = Depends(require_profile)
):
    """
    Get personalized financial insights based on spending patterns.
    
    Returns actionable insights about:
    - Unusual spending patterns
    - Savings opportunities
    - Budget recommendations
    """
    settings = get_settings()
    end_date = date.today()
    start_date = end_date - timedelta(days=90)  # Last 3 months
    
    # Fetch transactions
    transactions = await transaction_client.get_transactions(
        token=user.token,
        start_date=start_date,
        end_date=end_date
    )
    
    # Analyze spending
    analysis = spending_analyzer.analyze(
        transactions=transactions,
        profile_id=str(user.profile_id),
        start_date=start_date,
        end_date=end_date
    )
    
    insights: List[InsightResponse] = []
    
    # Insight 1: Savings rate
    if analysis.savings_rate < 10:
        insights.append(InsightResponse(
            insight_type="SAVINGS_RATE",
            title="Low Savings Rate",
            message=f"Your savings rate is {analysis.savings_rate:.1f}%. "
                   f"Aim for at least 20% to build a healthy financial cushion.",
            priority="HIGH",
            action_type="INCREASE_SAVINGS"
        ))
    elif analysis.savings_rate >= 30:
        insights.append(InsightResponse(
            insight_type="SAVINGS_RATE",
            title="Excellent Savings!",
            message=f"You're saving {analysis.savings_rate:.1f}% of your income. "
                   f"Consider investing the surplus for better returns.",
            priority="LOW",
            action_type="INVEST"
        ))
    
    # Insight 2: High spending categories
    for cat in analysis.potential_savings_categories[:2]:
        if cat.percent_of_income and cat.percent_of_income > settings.high_spending_threshold_percent:
            insights.append(InsightResponse(
                insight_type="HIGH_SPENDING",
                title=f"High {cat.category.replace('_', ' ').title()} Spending",
                message=f"You spent ₹{cat.total_amount:,.0f} on {cat.category.lower()} "
                       f"({cat.percent_of_income:.1f}% of income). "
                       f"Reducing by 20% could save ₹{cat.total_amount * 0.2:,.0f}.",
                category=cat.category,
                amount=cat.total_amount * 0.2,
                priority="MEDIUM",
                action_type="REDUCE_SPENDING"
            ))
    
    # Insight 3: Monthly trend
    if len(analysis.monthly_trend) >= 2:
        recent = analysis.monthly_trend[-1]
        previous = analysis.monthly_trend[-2]
        expense_change = ((recent.total_expense - previous.total_expense) / previous.total_expense * 100
                         if previous.total_expense > 0 else 0)
        
        if expense_change > 20:
            insights.append(InsightResponse(
                insight_type="EXPENSE_TREND",
                title="Spending Increased",
                message=f"Your expenses increased by {expense_change:.0f}% compared to last month. "
                       f"Review recent transactions to identify the cause.",
                amount=recent.total_expense - previous.total_expense,
                priority="MEDIUM",
                action_type="REVIEW_SPENDING"
            ))
    
    # Generate summary
    if not insights:
        summary = "Your finances look healthy! Keep up the good work."
    elif any(i.priority == "HIGH" for i in insights):
        summary = "There are some areas that need attention. Review the insights below."
    else:
        summary = "A few optimization opportunities found. Small changes can make a big difference!"
    
    return InsightsResponse(
        profile_id=user.profile_id,
        generated_at=date.today(),
        insights=insights,
        summary=summary
    )


@router.get("/ai-insights", response_model=AIInsightResponse)
async def get_ai_insights(
    user: UserPrincipal = Depends(require_profile)
):
    """
    Get AI-powered personalized financial insights using OpenAI.

    Provides:
    - Comprehensive financial health assessment
    - Personalized recommendations based on spending patterns
    - Actionable tips tailored to Indian context
    - Financial health score (0-100)

    Falls back to rule-based insights if OpenAI is unavailable.
    """
    end_date = date.today()
    start_date = end_date - timedelta(days=180)  # 6 months

    # Fetch transactions
    transactions = await transaction_client.get_transactions(
        token=user.token,
        start_date=start_date,
        end_date=end_date
    )

    # Analyze spending
    analysis = spending_analyzer.analyze(
        transactions=transactions,
        profile_id=str(user.profile_id),
        start_date=start_date,
        end_date=end_date
    )

    # Calculate savings capacity
    capacity = savings_recommender.calculate_savings_capacity(
        transactions=transactions,
        profile_id=str(user.profile_id)
    )

    # Generate AI insights
    insights = await openai_advisor.generate_insights(analysis, capacity, transactions)

    return insights


@router.post("/ai-goal-advice")
async def get_ai_goal_advice(
    request: GoalRecommendationRequest,
    user: UserPrincipal = Depends(require_profile)
):
    """
    Get AI-powered personalized advice for a specific savings goal.

    Uses OpenAI to generate contextual, encouraging advice
    based on the user's financial situation and goal details.
    """
    end_date = date.today()
    start_date = end_date - timedelta(days=180)

    # Fetch transactions
    transactions = await transaction_client.get_transactions(
        token=user.token,
        start_date=start_date,
        end_date=end_date
    )

    # Analyze spending
    analysis = spending_analyzer.analyze(
        transactions=transactions,
        profile_id=str(user.profile_id),
        start_date=start_date,
        end_date=end_date
    )

    # Calculate savings capacity
    capacity = savings_recommender.calculate_savings_capacity(
        transactions=transactions,
        profile_id=str(user.profile_id)
    )

    # Generate AI advice
    advice = await openai_advisor.generate_goal_advice(request, capacity, analysis)

    # Also get the standard recommendation
    recommendation = savings_recommender.recommend_for_goal(request, capacity)

    return {
        "goal_id": request.goal_id,
        "goal_name": request.goal_name,
        "ai_advice": advice,
        "recommendation": recommendation,
        "ai_available": openai_advisor.is_available()
    }


@router.get("/forecast", response_model=ForecastResponse)
async def get_forecast(
    user: UserPrincipal = Depends(require_profile)
):
    """
    Get AI-powered financial forecast for the current month.

    Uses statistical methods and trend analysis to predict:
    - Projected income for current month
    - Projected expenses for current month
    - Projected savings
    - Trend direction (UP, DOWN, STABLE)
    - Confidence score
    - Actionable insights

    The forecast uses:
    1. Weighted moving average of past 6 months (recent months weighted more)
    2. Day-of-month projection for current month data
    3. Trend analysis for adjustment
    """
    end_date = date.today()
    start_date = end_date - timedelta(days=180)  # 6 months of data

    # Fetch transactions
    transactions = await transaction_client.get_transactions(
        token=user.token,
        start_date=start_date,
        end_date=end_date
    )

    # Generate forecast
    forecast = forecast_service.generate_forecast(
        transactions=transactions,
        profile_id=str(user.profile_id)
    )

    return forecast

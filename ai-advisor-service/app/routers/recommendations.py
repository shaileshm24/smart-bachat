from fastapi import APIRouter, Depends
from datetime import date, timedelta
from typing import List, Optional
import logging

from app.security import UserPrincipal, require_profile

logger = logging.getLogger(__name__)
from app.models.schemas import (
    GoalRecommendationRequest, GoalRecommendationResponse,
    InsightsResponse, InsightResponse, GoalInsightsResponse
)
from app.services.transaction_client import transaction_client
from app.services.spending_analyzer import spending_analyzer, DISCRETIONARY_CATEGORIES
from app.services.savings_recommender import savings_recommender
from app.services.ai_advisor import ai_advisor, AIInsightResponse
from app.services.forecast_service import forecast_service, ForecastResponse
from app.services.goal_client import goal_client
from app.services.goal_insights_service import goal_insights_service
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
    insights = await ai_advisor.generate_insights(analysis, capacity, transactions)

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
    logger.info(f"Generating AI advice for goal: {request.goal_name}")
    advice_result = await ai_advisor.generate_goal_advice(request, capacity, analysis)
    logger.info(f"AI advice source: {advice_result.get('source', 'unknown')}")

    # Also get the standard recommendation for comparison
    recommendation = savings_recommender.recommend_for_goal(request, capacity)

    return {
        "goal_id": request.goal_id,
        "goal_name": request.goal_name,
        "ai_advice": advice_result.get("advice", ""),
        "advice_source": advice_result.get("source", "unknown"),
        "recommendation": recommendation,
        "ai_available": ai_advisor.is_available()
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


@router.get("/goal-insights", response_model=GoalInsightsResponse)
async def get_goal_insights(
    user: UserPrincipal = Depends(require_profile)
):
    """
    Get comprehensive goal insights based on spending behavior.

    Provides:
    - Insights for each active goal (progress, warnings, suggestions)
    - New goal suggestions based on spending patterns
    - Motivational messages to encourage saving
    - Overall goal health score

    Analyzes user's transactions and goals to provide personalized recommendations.
    """
    end_date = date.today()
    start_date = end_date - timedelta(days=180)  # 6 months

    # Fetch transactions and goals in parallel
    transactions = await transaction_client.get_transactions(
        token=user.token,
        start_date=start_date,
        end_date=end_date
    )

    goals = await goal_client.get_goals(
        token=user.token,
        status="ACTIVE"
    )

    # Also get completed goals for context
    all_goals = await goal_client.get_goals(token=user.token)

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

    # Generate goal insights
    insights = goal_insights_service.generate_insights(
        profile_id=user.profile_id,
        goals=all_goals,
        transactions=transactions,
        analysis=analysis,
        capacity=capacity
    )

    return insights


@router.get("/ai-goal-insights")
async def get_ai_goal_insights(
    user: UserPrincipal = Depends(require_profile)
):
    """
    Get AI-powered goal insights with personalized motivational messages.

    Uses OpenAI to generate:
    - Personalized insights for each goal
    - Motivational messages to encourage saving
    - Smart saving tips based on spending patterns
    - New goal suggestions based on behavior

    Falls back to rule-based insights if OpenAI is unavailable.
    """
    import time

    logger.info("=" * 60)
    logger.info("AI GOAL INSIGHTS ENDPOINT CALLED")
    logger.info(f"User profile_id: {user.profile_id}")
    start_time = time.time()

    end_date = date.today()
    start_date = end_date - timedelta(days=180)
    logger.info(f"Date range: {start_date} to {end_date}")

    try:
        # Fetch transactions and goals
        logger.info("Fetching transactions...")
        transactions = await transaction_client.get_transactions(
            token=user.token,
            start_date=start_date,
            end_date=end_date
        )
        logger.info(f"Fetched {len(transactions)} transactions in {time.time() - start_time:.2f}s")

        logger.info("Fetching goals...")
        goals = await goal_client.get_goals(token=user.token)
        logger.info(f"Fetched {len(goals)} goals in {time.time() - start_time:.2f}s")
    except Exception as e:
        logger.error(f"Error fetching data: {type(e).__name__}: {e}")
        raise

    # Analyze spending
    logger.info("Analyzing spending...")
    analysis = spending_analyzer.analyze(
        transactions=transactions,
        profile_id=str(user.profile_id),
        start_date=start_date,
        end_date=end_date
    )
    logger.info(f"Spending analysis complete in {time.time() - start_time:.2f}s")

    # Calculate savings capacity
    logger.info("Calculating savings capacity...")
    capacity = savings_recommender.calculate_savings_capacity(
        transactions=transactions,
        profile_id=str(user.profile_id)
    )
    logger.info(f"Savings capacity calculated in {time.time() - start_time:.2f}s")

    # Generate AI-powered insights
    active_provider = ai_advisor.get_active_provider()
    logger.info(f"Calling AI for insights (provider: {active_provider})...")
    try:
        ai_insights = await ai_advisor.generate_ai_goal_insights(
            goals=goals,
            analysis=analysis,
            capacity=capacity
        )
        logger.info(f"AI response received in {time.time() - start_time:.2f}s")
        logger.info(f"AI insights keys: {list(ai_insights.keys()) if ai_insights else 'None'}")
    except Exception as e:
        logger.error(f"AI error: {type(e).__name__}: {e}")
        raise

    # Build response
    active_goals = [g for g in goals if g.status == "ACTIVE"]
    completed_goals = [g for g in goals if g.status == "COMPLETED"]

    # Calculate overall health score
    # Use AI's score if available, otherwise calculate locally
    ai_health_score = ai_insights.get("overall_health_score")

    # Calculate local health score as fallback
    def calculate_local_health_score() -> float:
        score = 50.0  # Base score

        # Factor 1: Goals progress (0-25 points)
        if active_goals:
            avg_progress = sum(g.progress_percent for g in active_goals) / len(active_goals)
            score += min(25, avg_progress * 0.25)

        # Factor 2: Goals on track (0-20 points)
        if active_goals:
            on_track_ratio = sum(1 for g in active_goals if g.is_on_track) / len(active_goals)
            score += on_track_ratio * 20

        # Factor 3: Savings rate (0-20 points)
        if analysis.savings_rate > 0:
            score += min(20, analysis.savings_rate * 0.5)

        # Factor 4: Completed goals bonus (0-10 points)
        if completed_goals:
            score += min(10, len(completed_goals) * 2)

        # Factor 5: Discretionary spending penalty (-15 to 0 points)
        if capacity.avg_monthly_income > 0:
            discretionary_ratio = capacity.avg_monthly_discretionary_expenses / capacity.avg_monthly_income
            if discretionary_ratio > 0.3:
                score -= min(15, (discretionary_ratio - 0.3) * 50)

        return max(0, min(100, score))

    calculated_score = calculate_local_health_score()

    # Check if response came from fallback (AI failed)
    is_fallback = ai_insights.get("score_source") == "fallback"

    # Use AI score only if it's from actual AI (not fallback) and valid (0-100)
    if not is_fallback and ai_health_score is not None and isinstance(ai_health_score, (int, float)) and 0 <= ai_health_score <= 100:
        overall_health_score = ai_health_score
        score_source = active_provider  # "gemini" or "openai"
    elif is_fallback and ai_health_score is not None:
        # Fallback score from ai_advisor
        overall_health_score = ai_health_score
        score_source = "fallback"
    else:
        # Local calculation
        overall_health_score = calculated_score
        score_source = "calculated"

    logger.info(f"Health Score: {overall_health_score:.1f} (source: {score_source}, AI: {ai_health_score if not is_fallback else 'N/A'}, Calculated: {calculated_score:.1f})")
    logger.info(f"Total time: {time.time() - start_time:.2f}s")
    logger.info("=" * 60)

    return {
        "profile_id": str(user.profile_id),
        "generated_at": date.today().isoformat(),
        "goals_summary": {
            "total_goals": len(goals),
            "active_goals": len(active_goals),
            "completed_goals": len(completed_goals),
            "total_target": sum(g.target_amount for g in active_goals),
            "total_saved": sum(g.current_amount for g in active_goals),
            "overall_progress": (
                sum(g.current_amount for g in active_goals) /
                sum(g.target_amount for g in active_goals) * 100
                if active_goals else 0
            )
        },
        "goal_insights": ai_insights.get("goal_specific_insights", []),
        "motivational_message": ai_insights.get("motivational_message", ""),
        "saving_tips": ai_insights.get("saving_tips", []),
        "suggested_new_goals": ai_insights.get("suggested_new_goals", []),
        "overall_health_score": round(overall_health_score, 1),
        "score_source": score_source,  # Include source in response for transparency
        "ai_available": ai_advisor.is_available() and not is_fallback
    }

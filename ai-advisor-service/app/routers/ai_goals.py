"""
AI-Powered Goal Advisory Endpoints.
All endpoints use OpenAI for intelligent analysis and recommendations.
"""
import logging
from fastapi import APIRouter, Depends, HTTPException
from datetime import date, timedelta
from typing import Optional
from uuid import UUID
from pydantic import BaseModel

from app.security import UserPrincipal, require_profile
from app.services.transaction_client import transaction_client
from app.services.goal_client import goal_client
from app.services.ai_goal_advisor import (
    ai_goal_advisor,
    AIGoalAchievementPlan,
    AIGoalSuggestion,
    AITransactionImpact,
    AISpendingWarning
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/ai-goals", tags=["AI Goal Advisor"])


class TransactionAnalysisRequest(BaseModel):
    """Request for analyzing a transaction's impact on goals."""
    amount: float
    category: Optional[str] = None
    description: Optional[str] = None


@router.get("/advice/{goal_id}", response_model=AIGoalAchievementPlan)
async def get_goal_achievement_plan(
    goal_id: UUID,
    user: UserPrincipal = Depends(require_profile)
):
    """
    🤖 AI Goal Achievement Advisor
    
    Get an AI-generated plan to achieve a specific goal.
    
    AI analyzes:
    - Your spending patterns
    - Current savings rate
    - Goal timeline
    
    Returns:
    - Achievement probability
    - Recommended monthly savings
    - Specific spending cuts with amounts
    - Step-by-step action plan
    - Risk factors
    - Motivational insights
    """
    if not ai_goal_advisor.is_available():
        raise HTTPException(status_code=503, detail="AI service not configured")

    end_date = date.today()
    start_date = end_date - timedelta(days=180)

    # Fetch data
    transactions = await transaction_client.get_transactions(
        token=user.token, start_date=start_date, end_date=end_date
    )
    all_goals = await goal_client.get_goals(token=user.token)
    
    # Find the specific goal
    goal = next((g for g in all_goals if g.id == goal_id), None)
    if not goal:
        raise HTTPException(status_code=404, detail="Goal not found")

    # Get AI analysis
    return await ai_goal_advisor.analyze_goal_achievement(
        goal=goal,
        transactions=transactions,
        all_goals=all_goals
    )


@router.get("/suggestions", response_model=list[AIGoalSuggestion])
async def get_ai_goal_suggestions(
    user: UserPrincipal = Depends(require_profile)
):
    """
    🤖 AI Goal Suggestions
    
    AI analyzes your spending behavior and suggests personalized goals.
    
    AI considers:
    - Your spending patterns by category
    - Current savings capacity
    - Missing essential goals (emergency fund, etc.)
    - Lifestyle patterns from transactions
    
    Returns goals you should consider with:
    - Why this goal makes sense for YOU
    - How to achieve it
    - Suggested target and timeline
    """
    logger.info("=" * 60)
    logger.info("AI GOAL SUGGESTIONS ENDPOINT CALLED")
    logger.info(f"User profile_id: {user.profile_id}")

    if not ai_goal_advisor.is_available():
        logger.error("AI service not configured - OpenAI client is None")
        raise HTTPException(status_code=503, detail="AI service not configured")

    logger.info("AI service is available, fetching data...")

    end_date = date.today()
    start_date = end_date - timedelta(days=180)
    logger.info(f"Date range: {start_date} to {end_date}")

    try:
        logger.info("Fetching transactions from bachat-core-service...")
        transactions = await transaction_client.get_transactions(
            token=user.token, start_date=start_date, end_date=end_date
        )
        logger.info(f"Fetched {len(transactions)} transactions")

        logger.info("Fetching existing goals from bachat-core-service...")
        existing_goals = await goal_client.get_goals(token=user.token)
        logger.info(f"Fetched {len(existing_goals)} existing goals")
    except Exception as e:
        logger.error(f"Error fetching data: {type(e).__name__}: {e}")
        raise HTTPException(status_code=500, detail=f"Error fetching data: {str(e)}")

    if not transactions:
        logger.warning("No transactions found - cannot suggest goals")
        raise HTTPException(
            status_code=400,
            detail="No transaction history found. AI needs transaction data to suggest goals."
        )

    try:
        logger.info("Calling AI to generate goal suggestions...")
        suggestions = await ai_goal_advisor.suggest_goals(
            transactions=transactions,
            existing_goals=existing_goals
        )
        logger.info(f"AI returned {len(suggestions)} goal suggestions")
        logger.info("=" * 60)
        return suggestions
    except Exception as e:
        logger.error(f"Error from AI service: {type(e).__name__}: {e}")
        logger.info("=" * 60)
        raise HTTPException(status_code=500, detail=f"AI service error: {str(e)}")


@router.post("/transaction-impact", response_model=AITransactionImpact)
async def analyze_transaction_impact(
    request: TransactionAnalysisRequest,
    user: UserPrincipal = Depends(require_profile)
):
    """
    🤖 AI Transaction Impact Analysis
    
    Before making a purchase, check how it affects your goals!
    
    AI analyzes:
    - Is this transaction essential?
    - Which goals will be affected?
    - How many days will this delay your goals?
    
    Returns:
    - AI verdict on the transaction
    - Impact level (HIGH/MEDIUM/LOW)
    - Alternative suggestions to save money
    - Savings opportunity
    """
    if not ai_goal_advisor.is_available():
        raise HTTPException(status_code=503, detail="AI service not configured")

    end_date = date.today()
    start_date = end_date - timedelta(days=90)

    transactions = await transaction_client.get_transactions(
        token=user.token, start_date=start_date, end_date=end_date
    )
    goals = await goal_client.get_goals(token=user.token, status="ACTIVE")

    return await ai_goal_advisor.analyze_transaction_impact(
        transaction_amount=request.amount,
        transaction_category=request.category,
        transaction_description=request.description,
        goals=goals,
        recent_transactions=transactions
    )


@router.get("/spending-warnings", response_model=list[AISpendingWarning])
async def get_spending_warnings(
    user: UserPrincipal = Depends(require_profile)
):
    """
    🤖 AI Spending Warnings

    AI identifies unnecessary spending patterns affecting your goals.

    Detects:
    - UNNECESSARY: Things you don't need
    - EXCESSIVE: Overspending in categories
    - IMPULSE: Frequent small purchases adding up
    - RECURRING_WASTE: Unused subscriptions

    Returns warnings with:
    - Severity level
    - Affected goals
    - Potential monthly savings
    - Specific recommendations
    """
    if not ai_goal_advisor.is_available():
        raise HTTPException(status_code=503, detail="AI service not configured")

    end_date = date.today()
    start_date = end_date - timedelta(days=180)

    transactions = await transaction_client.get_transactions(
        token=user.token, start_date=start_date, end_date=end_date
    )
    goals = await goal_client.get_goals(token=user.token, status="ACTIVE")

    return await ai_goal_advisor.get_spending_warnings(
        transactions=transactions,
        goals=goals
    )


@router.get("/comprehensive-advice")
async def get_comprehensive_advice(
    user: UserPrincipal = Depends(require_profile)
):
    """
    🤖 AI Comprehensive Goal Advice

    Get a complete AI-powered financial advisory report.

    Includes:
    - Financial health score (0-100)
    - Overall assessment
    - Individual goal insights with status
    - Spending analysis
    - Prioritized recommendations
    - Motivational message
    - Next steps to take

    This is your AI financial advisor in one endpoint!
    """
    if not ai_goal_advisor.is_available():
        raise HTTPException(status_code=503, detail="AI service not configured")

    end_date = date.today()
    start_date = end_date - timedelta(days=180)

    transactions = await transaction_client.get_transactions(
        token=user.token, start_date=start_date, end_date=end_date
    )
    goals = await goal_client.get_goals(token=user.token)

    advice = await ai_goal_advisor.get_comprehensive_goal_advice(
        goals=goals,
        transactions=transactions
    )

    return {
        "profile_id": str(user.profile_id),
        "generated_at": date.today().isoformat(),
        "ai_powered": True,
        **advice
    }


@router.post("/should-i-buy")
async def should_i_buy(
    request: TransactionAnalysisRequest,
    user: UserPrincipal = Depends(require_profile)
):
    """
    🤖 AI "Should I Buy?" Advisor

    Thinking of making a purchase? Ask AI first!

    Send the amount and category, and AI will tell you:
    - Should you buy it? (YES/NO/MAYBE)
    - How it affects your goals
    - What you could do instead
    - The real cost in terms of goal delays

    Perfect for avoiding impulse purchases!
    """
    if not ai_goal_advisor.is_available():
        raise HTTPException(status_code=503, detail="AI service not configured")

    end_date = date.today()
    start_date = end_date - timedelta(days=90)

    transactions = await transaction_client.get_transactions(
        token=user.token, start_date=start_date, end_date=end_date
    )
    goals = await goal_client.get_goals(token=user.token, status="ACTIVE")

    impact = await ai_goal_advisor.analyze_transaction_impact(
        transaction_amount=request.amount,
        transaction_category=request.category,
        transaction_description=request.description,
        goals=goals,
        recent_transactions=transactions
    )

    # Determine recommendation
    if impact.is_essential:
        recommendation = "YES"
        reason = "This appears to be an essential expense."
    elif impact.impact_level == "HIGH":
        recommendation = "NO"
        reason = "This will significantly impact your goals."
    elif impact.impact_level == "MEDIUM":
        recommendation = "MAYBE"
        reason = "Consider if you really need this right now."
    else:
        recommendation = "YES"
        reason = "This has minimal impact on your goals."

    return {
        "amount": request.amount,
        "category": request.category,
        "recommendation": recommendation,
        "reason": reason,
        "is_essential": impact.is_essential,
        "impact_level": impact.impact_level,
        "ai_verdict": impact.ai_verdict,
        "affected_goals": impact.affected_goals,
        "alternative": impact.alternative_suggestion,
        "potential_savings": impact.savings_opportunity
    }


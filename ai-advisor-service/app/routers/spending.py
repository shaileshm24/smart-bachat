from fastapi import APIRouter, Depends, Query
from datetime import date, timedelta
from typing import Optional

from app.security import UserPrincipal, require_profile
from app.models.schemas import SpendingAnalysisResponse, SavingsCapacityResponse
from app.services.transaction_client import transaction_client
from app.services.spending_analyzer import spending_analyzer
from app.services.savings_recommender import savings_recommender
from app.config import get_settings

router = APIRouter(prefix="/api/advisor", tags=["Spending Analysis"])


@router.get("/spending-analysis", response_model=SpendingAnalysisResponse)
async def get_spending_analysis(
    user: UserPrincipal = Depends(require_profile),
    months: int = Query(default=6, ge=1, le=24, description="Number of months to analyze")
):
    """
    Analyze spending patterns for the authenticated user.
    
    Returns detailed breakdown by category, monthly trends, and potential savings areas.
    """
    settings = get_settings()
    
    end_date = date.today()
    start_date = end_date - timedelta(days=months * 30)
    
    # Fetch transactions from pdf-parser-service
    transactions = await transaction_client.get_transactions(
        token=user.token,
        start_date=start_date,
        end_date=end_date
    )
    
    # Perform analysis
    analysis = spending_analyzer.analyze(
        transactions=transactions,
        profile_id=str(user.profile_id),
        start_date=start_date,
        end_date=end_date
    )
    
    return analysis


@router.get("/savings-capacity", response_model=SavingsCapacityResponse)
async def get_savings_capacity(
    user: UserPrincipal = Depends(require_profile)
):
    """
    Calculate how much the user can safely save each month.
    
    Analyzes income and expense patterns to determine:
    - Safe monthly savings (without impacting essentials)
    - Aggressive savings potential
    - Recommended savings amount
    """
    end_date = date.today()
    start_date = end_date - timedelta(days=180)  # 6 months
    
    # Fetch transactions
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
    
    return capacity


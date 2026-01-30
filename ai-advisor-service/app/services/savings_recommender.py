from typing import List, Optional
from datetime import date, timedelta
from uuid import UUID

from app.models.schemas import (
    TransactionData, SavingsCapacityResponse, GoalRecommendationRequest,
    GoalRecommendationResponse, InsightResponse, InsightsResponse,
    RecommendationType
)
from app.services.spending_analyzer import (
    spending_analyzer, ESSENTIAL_CATEGORIES, DISCRETIONARY_CATEGORIES
)
from app.config import get_settings


class SavingsRecommender:
    """Generates savings recommendations based on spending analysis."""
    
    def __init__(self):
        self.settings = get_settings()
    
    def calculate_savings_capacity(
        self,
        transactions: List[TransactionData],
        profile_id: str
    ) -> SavingsCapacityResponse:
        """
        Calculate how much the user can safely save each month.
        """
        # Analyze last 3 months for more accurate current picture
        recent_txns = [t for t in transactions if t.txn_date >= date.today() - timedelta(days=90)]
        
        if len(recent_txns) < self.settings.min_transactions_for_analysis:
            recent_txns = transactions  # Fall back to all transactions
        
        months = max(1, len(set(t.txn_date.strftime("%Y-%m") for t in recent_txns)))
        
        # Calculate income and expenses
        income = sum(t.amount for t in recent_txns if t.direction == "CREDIT")
        expenses = sum(t.amount for t in recent_txns if t.direction == "DEBIT")
        
        avg_monthly_income = income / months
        avg_monthly_expense = expenses / months
        
        # Categorize expenses
        essential_expense = sum(
            t.amount for t in recent_txns
            if t.direction == "DEBIT" and t.category in ESSENTIAL_CATEGORIES
        ) / months
        
        discretionary_expense = sum(
            t.amount for t in recent_txns
            if t.direction == "DEBIT" and t.category in DISCRETIONARY_CATEGORIES
        ) / months
        
        other_expense = avg_monthly_expense - essential_expense - discretionary_expense
        
        # Calculate savings capacity
        current_savings = avg_monthly_income - avg_monthly_expense
        current_savings_rate = (current_savings / avg_monthly_income * 100) if avg_monthly_income > 0 else 0
        
        # Safe savings: income - essentials - 50% of discretionary (buffer)
        safe_savings = avg_monthly_income - essential_expense - (discretionary_expense * 0.5) - other_expense
        safe_savings = max(0, safe_savings)
        
        # Aggressive savings: income - essentials - 20% of discretionary
        aggressive_savings = avg_monthly_income - essential_expense - (discretionary_expense * 0.2) - (other_expense * 0.5)
        aggressive_savings = max(0, aggressive_savings)
        
        # Recommended: somewhere in between based on current habits
        if current_savings_rate < 10:
            recommended = safe_savings * 0.7  # Start conservative
        elif current_savings_rate < 20:
            recommended = safe_savings * 0.85
        else:
            recommended = safe_savings  # Already good saver
        
        # Confidence based on data quality
        confidence = min(0.95, 0.5 + (len(recent_txns) / 100) * 0.45)
        
        explanation = self._generate_capacity_explanation(
            avg_monthly_income, essential_expense, discretionary_expense,
            safe_savings, current_savings_rate
        )
        
        return SavingsCapacityResponse(
            profile_id=profile_id,
            avg_monthly_income=round(avg_monthly_income, 2),
            avg_monthly_essential_expenses=round(essential_expense, 2),
            avg_monthly_discretionary_expenses=round(discretionary_expense, 2),
            current_savings_rate=round(current_savings_rate, 2),
            safe_monthly_savings=round(safe_savings, 2),
            aggressive_monthly_savings=round(aggressive_savings, 2),
            recommended_monthly_savings=round(recommended, 2),
            confidence_score=round(confidence, 2),
            explanation=explanation
        )
    
    def recommend_for_goal(
        self,
        request: GoalRecommendationRequest,
        savings_capacity: SavingsCapacityResponse
    ) -> GoalRecommendationResponse:
        """Generate recommendation for a specific goal."""
        remaining = request.target_amount - request.current_amount
        
        if remaining <= 0:
            return GoalRecommendationResponse(
                goal_id=request.goal_id,
                recommendation_type=RecommendationType.MONTHLY_SAVING,
                message=f"Congratulations! You've achieved your {request.goal_name} goal!",
                suggested_monthly_saving=0,
                is_achievable=True,
                confidence_score=1.0,
                tips=["Consider setting a new goal to keep the momentum!"]
            )
        
        # Calculate required monthly saving
        if request.deadline:
            months_remaining = max(1, (request.deadline.year - date.today().year) * 12 + 
                                   request.deadline.month - date.today().month)
            required_monthly = remaining / months_remaining
        else:
            # Default to 12 months if no deadline
            months_remaining = 12
            required_monthly = remaining / 12
        
        is_achievable = required_monthly <= savings_capacity.safe_monthly_savings
        
        # Generate appropriate recommendation
        if is_achievable:
            message = f"You can easily save ₹{required_monthly:,.0f} per month for your {request.goal_name}."
            tips = [
                f"Set up an automatic transfer of ₹{required_monthly:,.0f} on salary day",
                "Track your progress weekly to stay motivated"
            ]
            rec_type = RecommendationType.MONTHLY_SAVING
            adjusted_deadline = None
        else:
            # Calculate realistic deadline (handle zero savings case)
            if savings_capacity.recommended_monthly_savings > 0:
                realistic_months = int(remaining / savings_capacity.recommended_monthly_savings) + 1
            else:
                realistic_months = 24  # Default to 2 years if no savings data
            adjusted_deadline = date.today() + timedelta(days=realistic_months * 30)

            gap = required_monthly - savings_capacity.safe_monthly_savings
            message = (f"To meet your {request.goal_name} goal on time, you'd need to save "
                      f"₹{required_monthly:,.0f}/month. Consider increasing savings by ₹{gap:,.0f} "
                      f"or extending deadline to {adjusted_deadline.strftime('%B %Y')}.")
            tips = [
                "Review discretionary spending for potential cuts",
                "Consider a side income to boost savings",
                f"Realistic monthly saving: ₹{savings_capacity.recommended_monthly_savings:,.0f}"
            ]
            rec_type = RecommendationType.GOAL_ADJUSTMENT
        
        return GoalRecommendationResponse(
            goal_id=request.goal_id,
            recommendation_type=rec_type,
            message=message,
            suggested_monthly_saving=round(min(required_monthly, savings_capacity.safe_monthly_savings), 2),
            is_achievable=is_achievable,
            adjusted_deadline=adjusted_deadline,
            confidence_score=savings_capacity.confidence_score,
            tips=tips
        )
    
    def _generate_capacity_explanation(
        self, income: float, essential: float, discretionary: float,
        safe_savings: float, current_rate: float
    ) -> str:
        """Generate human-readable explanation."""
        if current_rate >= 20:
            return (f"Great job! You're already saving {current_rate:.0f}% of your income. "
                   f"You can comfortably save up to ₹{safe_savings:,.0f} per month.")
        elif current_rate >= 10:
            return (f"You're saving {current_rate:.0f}% of your income. "
                   f"With some adjustments, you could save ₹{safe_savings:,.0f} per month.")
        elif current_rate > 0:
            return (f"Your current savings rate is {current_rate:.0f}%. "
                   f"By optimizing discretionary spending, you could save ₹{safe_savings:,.0f} per month.")
        elif safe_savings > 0:
            return (f"You're currently spending more than you earn (savings rate: {current_rate:.0f}%). "
                   f"By reducing discretionary spending, you could potentially save ₹{safe_savings:,.0f} per month.")
        else:
            # Negative savings rate AND no safe savings capacity
            potential_savings = discretionary * 0.5  # What they could save by cutting discretionary
            return (f"Your expenses currently exceed your income (savings rate: {current_rate:.0f}%). "
                   f"Consider reviewing your spending - cutting discretionary expenses by 50% could free up "
                   f"₹{potential_savings:,.0f} per month. Focus on reducing non-essential expenses first.")


# Singleton instance
savings_recommender = SavingsRecommender()


"""
Forecast Service - AI-powered financial forecasting.

Uses statistical methods and trend analysis to predict:
- Projected income for current month
- Projected expenses for current month
- Projected savings
- Trend direction and confidence
"""
from typing import List, Dict, Optional
from datetime import date, timedelta
from collections import defaultdict
import statistics
from enum import Enum

from pydantic import BaseModel
from app.models.schemas import TransactionData, MonthlySpending
from app.config import get_settings


class TrendDirection(str, Enum):
    UP = "UP"
    DOWN = "DOWN"
    STABLE = "STABLE"


class ForecastResponse(BaseModel):
    """Response for forecast endpoint."""
    projected_income: float
    projected_expense: float
    projected_savings: float
    trend: TrendDirection
    change_percent: float
    avg_monthly_income: float
    avg_monthly_expense: float
    savings_rate: float
    confidence_score: float
    forecast_method: str
    insights: List[str]


class ForecastService:
    """AI-powered financial forecasting service."""
    
    def __init__(self):
        self.settings = get_settings()
    
    def generate_forecast(
        self,
        transactions: List[TransactionData],
        profile_id: str
    ) -> ForecastResponse:
        """
        Generate AI-powered forecast for the current month.
        
        Uses multiple methods:
        1. Weighted moving average of past months
        2. Day-of-month projection for current month
        3. Trend analysis for adjustment
        """
        today = date.today()
        
        # Get monthly data for the past 6 months
        monthly_data = self._get_monthly_data(transactions)
        
        # Get current month partial data
        current_month_key = today.strftime("%Y-%m")
        current_month_data = monthly_data.get(current_month_key, {"income": 0, "expense": 0})
        
        # Get historical months (excluding current)
        historical_months = {k: v for k, v in monthly_data.items() if k != current_month_key}
        
        if len(historical_months) < 2:
            # Not enough data - use simple projection
            return self._simple_projection(current_month_data, today)
        
        # Calculate weighted moving average (recent months weighted more)
        avg_income, avg_expense = self._weighted_moving_average(historical_months)
        
        # Calculate trend
        trend_direction, trend_factor = self._calculate_trend(historical_months)
        
        # Project current month
        days_in_month = self._days_in_month(today)
        days_elapsed = today.day
        
        if days_elapsed > 0 and current_month_data["income"] > 0:
            # Use current month data with projection
            projection_factor = days_in_month / days_elapsed
            projected_income = current_month_data["income"] * projection_factor
            projected_expense = current_month_data["expense"] * projection_factor
            
            # Blend with historical average (70% current projection, 30% historical)
            projected_income = projected_income * 0.7 + avg_income * 0.3
            projected_expense = projected_expense * 0.7 + avg_expense * 0.3
            confidence = min(0.9, 0.5 + (days_elapsed / days_in_month) * 0.4)
            method = "BLENDED_PROJECTION"
        else:
            # Use historical average with trend adjustment
            projected_income = avg_income * (1 + trend_factor * 0.5)
            projected_expense = avg_expense * (1 + trend_factor * 0.5)
            confidence = 0.6
            method = "HISTORICAL_TREND"
        
        projected_savings = projected_income - projected_expense
        savings_rate = (projected_savings / projected_income * 100) if projected_income > 0 else 0
        
        # Calculate change percent from average
        change_percent = 0
        if avg_expense > 0:
            change_percent = ((projected_expense - avg_expense) / avg_expense) * 100
        
        # Generate insights
        insights = self._generate_insights(
            projected_income, projected_expense, avg_income, avg_expense,
            trend_direction, savings_rate
        )
        
        return ForecastResponse(
            projected_income=round(projected_income, 2),
            projected_expense=round(projected_expense, 2),
            projected_savings=round(projected_savings, 2),
            trend=trend_direction,
            change_percent=round(change_percent, 1),
            avg_monthly_income=round(avg_income, 2),
            avg_monthly_expense=round(avg_expense, 2),
            savings_rate=round(savings_rate, 1),
            confidence_score=round(confidence, 2),
            forecast_method=method,
            insights=insights
        )
    
    def _get_monthly_data(self, transactions: List[TransactionData]) -> Dict[str, Dict[str, float]]:
        """Group transactions by month."""
        monthly: Dict[str, Dict[str, float]] = defaultdict(lambda: {"income": 0, "expense": 0})
        
        for txn in transactions:
            month_key = txn.txn_date.strftime("%Y-%m")
            if txn.direction == "CREDIT":
                monthly[month_key]["income"] += txn.amount
            else:
                monthly[month_key]["expense"] += txn.amount
        
        return dict(monthly)
    
    def _weighted_moving_average(self, monthly_data: Dict[str, Dict[str, float]]) -> tuple:
        """Calculate weighted moving average with recent months weighted more."""
        sorted_months = sorted(monthly_data.keys(), reverse=True)
        
        weights = [0.35, 0.25, 0.20, 0.10, 0.05, 0.05]  # Most recent first
        total_weight = 0
        weighted_income = 0
        weighted_expense = 0
        
        for i, month in enumerate(sorted_months[:6]):
            weight = weights[i] if i < len(weights) else 0.05
            weighted_income += monthly_data[month]["income"] * weight
            weighted_expense += monthly_data[month]["expense"] * weight
            total_weight += weight
        
        if total_weight > 0:
            return weighted_income / total_weight, weighted_expense / total_weight
        return 0, 0

    def _calculate_trend(self, monthly_data: Dict[str, Dict[str, float]]) -> tuple:
        """Calculate trend direction and factor from historical data."""
        sorted_months = sorted(monthly_data.keys())

        if len(sorted_months) < 2:
            return TrendDirection.STABLE, 0

        # Compare recent 2 months vs older 2 months
        recent_months = sorted_months[-2:]
        older_months = sorted_months[-4:-2] if len(sorted_months) >= 4 else sorted_months[:2]

        recent_expense = sum(monthly_data[m]["expense"] for m in recent_months) / len(recent_months)
        older_expense = sum(monthly_data[m]["expense"] for m in older_months) / len(older_months)

        if older_expense == 0:
            return TrendDirection.STABLE, 0

        change = (recent_expense - older_expense) / older_expense

        if change > 0.1:
            return TrendDirection.UP, change
        elif change < -0.1:
            return TrendDirection.DOWN, change
        return TrendDirection.STABLE, change

    def _days_in_month(self, d: date) -> int:
        """Get number of days in the month."""
        if d.month == 12:
            next_month = date(d.year + 1, 1, 1)
        else:
            next_month = date(d.year, d.month + 1, 1)
        return (next_month - date(d.year, d.month, 1)).days

    def _simple_projection(self, current_data: Dict[str, float], today: date) -> ForecastResponse:
        """Simple projection when not enough historical data."""
        days_in_month = self._days_in_month(today)
        days_elapsed = max(1, today.day)
        factor = days_in_month / days_elapsed

        projected_income = current_data["income"] * factor
        projected_expense = current_data["expense"] * factor
        projected_savings = projected_income - projected_expense
        savings_rate = (projected_savings / projected_income * 100) if projected_income > 0 else 0

        return ForecastResponse(
            projected_income=round(projected_income, 2),
            projected_expense=round(projected_expense, 2),
            projected_savings=round(projected_savings, 2),
            trend=TrendDirection.STABLE,
            change_percent=0,
            avg_monthly_income=round(projected_income, 2),
            avg_monthly_expense=round(projected_expense, 2),
            savings_rate=round(savings_rate, 1),
            confidence_score=0.4,
            forecast_method="SIMPLE_PROJECTION",
            insights=["Not enough historical data for accurate forecast. Showing simple projection."]
        )

    def _generate_insights(
        self,
        projected_income: float,
        projected_expense: float,
        avg_income: float,
        avg_expense: float,
        trend: TrendDirection,
        savings_rate: float
    ) -> List[str]:
        """Generate forecast insights."""
        insights = []

        # Expense trend insight
        if trend == TrendDirection.UP:
            expense_change = ((projected_expense - avg_expense) / avg_expense * 100) if avg_expense > 0 else 0
            insights.append(f"Expenses trending up by {abs(expense_change):.1f}% compared to average.")
        elif trend == TrendDirection.DOWN:
            expense_change = ((avg_expense - projected_expense) / avg_expense * 100) if avg_expense > 0 else 0
            insights.append(f"Great! Expenses trending down by {abs(expense_change):.1f}%.")

        # Savings rate insight
        if savings_rate < 0:
            insights.append("⚠️ Projected to spend more than income this month.")
        elif savings_rate < 10:
            insights.append("Savings rate is low. Consider reducing discretionary spending.")
        elif savings_rate >= 30:
            insights.append("Excellent savings rate! You're on track for your goals.")

        # Income vs expense insight
        if projected_income > avg_income * 1.1:
            insights.append("Income looking higher than usual this month.")
        elif projected_income < avg_income * 0.9:
            insights.append("Income may be lower than usual. Plan expenses accordingly.")

        return insights


# Singleton instance
forecast_service = ForecastService()


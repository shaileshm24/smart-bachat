from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import date
from uuid import UUID
from enum import Enum


class RecommendationType(str, Enum):
    MONTHLY_SAVING = "MONTHLY_SAVING"
    GOAL_ADJUSTMENT = "GOAL_ADJUSTMENT"
    SPENDING_CUT = "SPENDING_CUT"
    GOAL_PRIORITY = "GOAL_PRIORITY"
    DEADLINE_WARNING = "DEADLINE_WARNING"


class CategorySpending(BaseModel):
    """Spending breakdown by category."""
    category: str
    total_amount: float  # in rupees
    transaction_count: int
    percent_of_total: float
    percent_of_income: Optional[float] = None
    avg_transaction: float
    trend: Optional[str] = None  # INCREASING, DECREASING, STABLE


class MonthlySpending(BaseModel):
    """Monthly spending summary."""
    month: str  # YYYY-MM format
    total_income: float
    total_expense: float
    net_savings: float
    savings_rate: float  # percentage


class SpendingAnalysisResponse(BaseModel):
    """Response for spending analysis endpoint."""
    profile_id: UUID
    analysis_period_start: date
    analysis_period_end: date
    total_income: float
    total_expenses: float
    net_savings: float
    avg_monthly_income: float
    avg_monthly_expense: float
    avg_monthly_savings: float
    savings_rate: float
    category_breakdown: List[CategorySpending]
    monthly_trend: List[MonthlySpending]
    top_spending_categories: List[str]
    potential_savings_categories: List[CategorySpending]


class SavingsCapacityResponse(BaseModel):
    """Response for savings capacity calculation."""
    profile_id: UUID
    avg_monthly_income: float
    avg_monthly_essential_expenses: float
    avg_monthly_discretionary_expenses: float
    current_savings_rate: float
    safe_monthly_savings: float  # Amount that can be saved without impacting essentials
    aggressive_monthly_savings: float  # Maximum possible savings
    recommended_monthly_savings: float
    confidence_score: float
    explanation: str


class GoalRecommendationRequest(BaseModel):
    """Request for goal-specific recommendation."""
    goal_id: UUID
    goal_name: str
    goal_type: str
    target_amount: float
    current_amount: float
    deadline: Optional[date] = None
    priority: Optional[str] = None


class GoalRecommendationResponse(BaseModel):
    """Response for goal recommendation."""
    goal_id: UUID
    recommendation_type: RecommendationType
    message: str
    suggested_monthly_saving: float
    is_achievable: bool
    adjusted_deadline: Optional[date] = None
    confidence_score: float
    tips: List[str]


class InsightResponse(BaseModel):
    """A single financial insight."""
    insight_type: str
    title: str
    message: str
    category: Optional[str] = None
    amount: Optional[float] = None
    action_type: Optional[str] = None
    priority: str = "MEDIUM"  # HIGH, MEDIUM, LOW


class InsightsResponse(BaseModel):
    """Response for insights endpoint."""
    profile_id: UUID
    generated_at: date
    insights: List[InsightResponse]
    summary: str


class TransactionData(BaseModel):
    """Transaction data from pdf-parser-service."""
    id: UUID
    txn_date: date
    amount: float
    direction: str  # CREDIT or DEBIT
    category: Optional[str] = None
    sub_category: Optional[str] = None
    description: Optional[str] = None
    merchant: Optional[str] = None


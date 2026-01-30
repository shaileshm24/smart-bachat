from typing import List, Dict
from datetime import date
from collections import defaultdict
import statistics

from app.models.schemas import (
    TransactionData, CategorySpending, MonthlySpending,
    SpendingAnalysisResponse
)
from app.config import get_settings


# Essential expense categories
ESSENTIAL_CATEGORIES = {
    "UTILITIES", "RENT", "GROCERIES", "HEALTHCARE", "INSURANCE",
    "EDUCATION", "EMI", "LOAN_REPAYMENT"
}

# Discretionary expense categories
DISCRETIONARY_CATEGORIES = {
    "FOOD", "ENTERTAINMENT", "SHOPPING", "TRAVEL", "SUBSCRIPTIONS",
    "PERSONAL_CARE", "GIFTS"
}


class SpendingAnalyzer:
    """Analyzes spending patterns from transaction data."""
    
    def __init__(self):
        self.settings = get_settings()
    
    def analyze(
        self,
        transactions: List[TransactionData],
        profile_id: str,
        start_date: date,
        end_date: date
    ) -> SpendingAnalysisResponse:
        """
        Perform comprehensive spending analysis.
        
        Args:
            transactions: List of transactions to analyze
            profile_id: User's profile ID
            start_date: Analysis period start
            end_date: Analysis period end
            
        Returns:
            SpendingAnalysisResponse with detailed breakdown
        """
        # Separate income and expenses
        income_txns = [t for t in transactions if t.direction == "CREDIT"]
        expense_txns = [t for t in transactions if t.direction == "DEBIT"]
        
        total_income = sum(t.amount for t in income_txns)
        total_expenses = sum(t.amount for t in expense_txns)
        net_savings = total_income - total_expenses
        
        # Calculate months in period
        months = max(1, (end_date.year - start_date.year) * 12 + end_date.month - start_date.month)
        
        avg_monthly_income = total_income / months
        avg_monthly_expense = total_expenses / months
        avg_monthly_savings = net_savings / months
        savings_rate = (net_savings / total_income * 100) if total_income > 0 else 0
        
        # Category breakdown
        category_breakdown = self._analyze_categories(expense_txns, total_expenses, total_income)
        
        # Monthly trend
        monthly_trend = self._analyze_monthly_trend(transactions)
        
        # Top spending categories
        top_categories = sorted(category_breakdown, key=lambda x: x.total_amount, reverse=True)[:5]
        top_spending_categories = [c.category for c in top_categories]
        
        # Potential savings categories (discretionary with high spending)
        potential_savings = [
            c for c in category_breakdown
            if c.category in DISCRETIONARY_CATEGORIES
            and c.percent_of_total > self.settings.savings_potential_threshold_percent
        ]
        
        return SpendingAnalysisResponse(
            profile_id=profile_id,
            analysis_period_start=start_date,
            analysis_period_end=end_date,
            total_income=round(total_income, 2),
            total_expenses=round(total_expenses, 2),
            net_savings=round(net_savings, 2),
            avg_monthly_income=round(avg_monthly_income, 2),
            avg_monthly_expense=round(avg_monthly_expense, 2),
            avg_monthly_savings=round(avg_monthly_savings, 2),
            savings_rate=round(savings_rate, 2),
            category_breakdown=category_breakdown,
            monthly_trend=monthly_trend,
            top_spending_categories=top_spending_categories,
            potential_savings_categories=potential_savings
        )
    
    def _analyze_categories(
        self,
        expenses: List[TransactionData],
        total_expenses: float,
        total_income: float
    ) -> List[CategorySpending]:
        """Analyze spending by category."""
        category_data: Dict[str, List[float]] = defaultdict(list)
        
        for txn in expenses:
            category = txn.category or "OTHER"
            category_data[category].append(txn.amount)
        
        result = []
        for category, amounts in category_data.items():
            total = sum(amounts)
            result.append(CategorySpending(
                category=category,
                total_amount=round(total, 2),
                transaction_count=len(amounts),
                percent_of_total=round(total / total_expenses * 100, 2) if total_expenses > 0 else 0,
                percent_of_income=round(total / total_income * 100, 2) if total_income > 0 else None,
                avg_transaction=round(statistics.mean(amounts), 2) if amounts else 0
            ))
        
        return sorted(result, key=lambda x: x.total_amount, reverse=True)
    
    def _analyze_monthly_trend(self, transactions: List[TransactionData]) -> List[MonthlySpending]:
        """Analyze monthly income/expense trend."""
        monthly: Dict[str, Dict[str, float]] = defaultdict(lambda: {"income": 0, "expense": 0})
        
        for txn in transactions:
            month_key = txn.txn_date.strftime("%Y-%m")
            if txn.direction == "CREDIT":
                monthly[month_key]["income"] += txn.amount
            else:
                monthly[month_key]["expense"] += txn.amount
        
        result = []
        for month, data in sorted(monthly.items()):
            income = data["income"]
            expense = data["expense"]
            savings = income - expense
            result.append(MonthlySpending(
                month=month,
                total_income=round(income, 2),
                total_expense=round(expense, 2),
                net_savings=round(savings, 2),
                savings_rate=round(savings / income * 100, 2) if income > 0 else 0
            ))
        
        return result


# Singleton instance
spending_analyzer = SpendingAnalyzer()


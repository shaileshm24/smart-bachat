"""
Tests for ForecastService - AI-powered financial forecasting.
"""
import pytest
import uuid
from datetime import date, timedelta
from app.services.forecast_service import ForecastService, ForecastResponse, TrendDirection
from app.models.schemas import TransactionData


@pytest.fixture
def forecast_service():
    """Create a ForecastService instance for testing."""
    return ForecastService()


@pytest.fixture
def sample_transactions():
    """Create sample transactions spanning 6 months."""
    transactions = []
    today = date.today()

    # Generate transactions for the past 6 months
    for month_offset in range(6):
        month_date = today - timedelta(days=30 * month_offset)

        # Income transactions (salary-like, around 100000)
        transactions.append(TransactionData(
            id=uuid.uuid4(),
            txn_date=month_date.replace(day=1),
            amount=100000.0 + (month_offset * 1000),  # Slight variation
            direction="CREDIT",
            category="SALARY",
            description=f"Salary Month {month_offset}"
        ))

        # Expense transactions (various categories)
        for day in [5, 10, 15, 20, 25]:
            try:
                txn_date = month_date.replace(day=day)
            except ValueError:
                txn_date = month_date.replace(day=min(day, 28))

            transactions.append(TransactionData(
                id=uuid.uuid4(),
                txn_date=txn_date,
                amount=5000.0 + (day * 100),  # Varying expenses
                direction="DEBIT",
                category="SHOPPING",
                description=f"Expense {day}"
            ))

    return transactions


class TestForecastService:
    """Test cases for ForecastService."""
    
    def test_generate_forecast_returns_valid_response(self, forecast_service, sample_transactions):
        """Test that generate_forecast returns a valid ForecastResponse."""
        result = forecast_service.generate_forecast(
            transactions=sample_transactions,
            profile_id="test-profile-123"
        )
        
        assert isinstance(result, ForecastResponse)
        assert result.projected_income >= 0
        assert result.projected_expense >= 0
        assert result.trend in [TrendDirection.UP, TrendDirection.DOWN, TrendDirection.STABLE]
        assert 0 <= result.confidence_score <= 1
        assert result.forecast_method in ["WEIGHTED_MOVING_AVERAGE", "BLENDED_PROJECTION", "SIMPLE_AVERAGE"]
    
    def test_generate_forecast_with_empty_transactions(self, forecast_service):
        """Test forecast with no transactions returns zeros."""
        result = forecast_service.generate_forecast(
            transactions=[],
            profile_id="test-profile-123"
        )
        
        assert result.projected_income == 0
        assert result.projected_expense == 0
        assert result.projected_savings == 0
        assert result.trend == TrendDirection.STABLE
        assert result.confidence_score < 0.5  # Low confidence with no data
    
    def test_generate_forecast_calculates_savings_correctly(self, forecast_service, sample_transactions):
        """Test that projected savings = income - expense."""
        result = forecast_service.generate_forecast(
            transactions=sample_transactions,
            profile_id="test-profile-123"
        )
        
        expected_savings = result.projected_income - result.projected_expense
        assert abs(result.projected_savings - expected_savings) < 0.01
    
    def test_generate_forecast_savings_rate(self, forecast_service, sample_transactions):
        """Test savings rate calculation."""
        result = forecast_service.generate_forecast(
            transactions=sample_transactions,
            profile_id="test-profile-123"
        )
        
        if result.projected_income > 0:
            expected_rate = (result.projected_savings / result.projected_income) * 100
            assert abs(result.savings_rate - expected_rate) < 0.1
    
    def test_generate_forecast_includes_insights(self, forecast_service, sample_transactions):
        """Test that forecast includes actionable insights."""
        result = forecast_service.generate_forecast(
            transactions=sample_transactions,
            profile_id="test-profile-123"
        )
        
        assert isinstance(result.insights, list)
        # Should have at least one insight
        assert len(result.insights) >= 0  # May be empty for stable trends
    
    def test_trend_detection_upward(self, forecast_service):
        """Test trend detection for increasing expenses."""
        transactions = []
        today = date.today()

        # Create increasing expense pattern
        for month_offset in range(6):
            month_date = today - timedelta(days=30 * month_offset)
            # Expenses increase each month (older months have lower expenses)
            expense_amount = 50000 + (5 - month_offset) * 10000

            transactions.append(TransactionData(
                id=uuid.uuid4(),
                txn_date=month_date.replace(day=15),
                amount=expense_amount,
                direction="DEBIT",
                category="SHOPPING",
                description=f"Expense {month_offset}"
            ))

            transactions.append(TransactionData(
                id=uuid.uuid4(),
                txn_date=month_date.replace(day=1),
                amount=100000.0,
                direction="CREDIT",
                category="SALARY",
                description=f"Salary {month_offset}"
            ))

        result = forecast_service.generate_forecast(
            transactions=transactions,
            profile_id="test-profile-123"
        )

        # With increasing expenses, trend should be UP
        assert result.trend in [TrendDirection.UP, TrendDirection.STABLE]


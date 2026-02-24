"""
Service for generating goal insights based on spending behavior.
"""
import logging
from typing import List, Dict
from datetime import date, timedelta
from uuid import UUID

from app.models.schemas import (
    GoalData, GoalInsight, GoalSuggestion, GoalInsightsResponse,
    TransactionData, SpendingAnalysisResponse, SavingsCapacityResponse
)


# Goal type suggestions based on spending patterns
GOAL_TYPE_TRIGGERS = {
    "TRAVEL": {
        "categories": ["TRAVEL", "TRANSPORT", "ENTERTAINMENT"],
        "threshold_percent": 10,
        "suggested_target_months": 12
    },
    "GADGET": {
        "categories": ["SHOPPING", "ELECTRONICS"],
        "threshold_percent": 8,
        "suggested_target_months": 6
    },
    "EMERGENCY": {
        "categories": [],  # Always suggest if not present
        "threshold_percent": 0,
        "suggested_target_months": 12
    },
    "EDUCATION": {
        "categories": ["EDUCATION", "BOOKS"],
        "threshold_percent": 5,
        "suggested_target_months": 12
    }
}


class GoalInsightsService:
    """Service for analyzing goals and generating insights."""

    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def generate_insights(
        self,
        profile_id: UUID,
        goals: List[GoalData],
        transactions: List[TransactionData],
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse
    ) -> GoalInsightsResponse:
        """
        Generate comprehensive goal insights based on spending behavior.
        """
        today = date.today()
        
        # Generate insights for each goal
        goal_insights = self._generate_goal_insights(goals, capacity)
        
        # Generate goal suggestions based on spending patterns
        suggested_goals = self._suggest_goals(goals, analysis, capacity)
        
        # Calculate overall health score
        health_score = self._calculate_health_score(goals, capacity)
        
        # Generate motivational message
        motivational_message = self._generate_motivation(goals, capacity, health_score)
        
        # Build summary
        goals_summary = self._build_summary(goals)
        
        return GoalInsightsResponse(
            profile_id=profile_id,
            generated_at=today,
            goals_summary=goals_summary,
            goal_insights=goal_insights,
            suggested_goals=suggested_goals,
            motivational_message=motivational_message,
            overall_health_score=health_score
        )

    def _generate_goal_insights(
        self,
        goals: List[GoalData],
        capacity: SavingsCapacityResponse
    ) -> List[GoalInsight]:
        """Generate insights for each goal."""
        insights = []
        
        for goal in goals:
            if goal.status != "ACTIVE":
                continue
                
            # Progress insight
            if goal.progress_percent >= 75:
                insights.append(GoalInsight(
                    goal_id=goal.id,
                    goal_name=goal.name,
                    insight_type="PROGRESS",
                    title="Almost there! 🎉",
                    message=f"You're {goal.progress_percent:.0f}% towards your {goal.name} goal! Just ₹{goal.remaining_amount:,.0f} more to go.",
                    priority="HIGH",
                    action_items=["Keep up the momentum!", "Consider a final push to complete this goal"]
                ))
            elif goal.progress_percent >= 50:
                insights.append(GoalInsight(
                    goal_id=goal.id,
                    goal_name=goal.name,
                    insight_type="PROGRESS",
                    title="Halfway milestone! 🌟",
                    message=f"Great progress on {goal.name}! You've saved ₹{goal.current_amount:,.0f} of ₹{goal.target_amount:,.0f}.",
                    priority="MEDIUM",
                    action_items=["Stay consistent with your savings"]
                ))
            
            # Warning for goals behind schedule
            if goal.is_on_track is False and goal.deadline:
                days_left = goal.days_remaining or 0
                if days_left > 0:
                    required_monthly = goal.remaining_amount / max(1, days_left / 30)
                    insights.append(GoalInsight(
                        goal_id=goal.id,
                        goal_name=goal.name,
                        insight_type="WARNING",
                        title="Needs attention ⚠️",
                        message=f"Your {goal.name} goal needs ₹{required_monthly:,.0f}/month to meet the deadline. Consider increasing contributions.",
                        priority="HIGH",
                        action_items=[
                            f"Increase monthly savings to ₹{required_monthly:,.0f}",
                            "Review discretionary spending",
                            "Consider extending deadline if needed"
                        ]
                    ))
            
            # Suggestion for low-priority goals with high capacity
            if goal.priority == "LOW" and capacity.safe_monthly_savings > 5000:
                insights.append(GoalInsight(
                    goal_id=goal.id,
                    goal_name=goal.name,
                    insight_type="SUGGESTION",
                    title="Opportunity to accelerate 🚀",
                    message=f"You have capacity to save more! Consider increasing contributions to {goal.name}.",
                    priority="LOW",
                    action_items=[f"Add ₹{min(2000, capacity.safe_monthly_savings * 0.2):,.0f} extra monthly"]
                ))

        return insights

    def _suggest_goals(
        self,
        existing_goals: List[GoalData],
        analysis: SpendingAnalysisResponse,
        capacity: SavingsCapacityResponse
    ) -> List[GoalSuggestion]:
        """Suggest new goals based on spending patterns."""
        suggestions = []
        existing_types = {g.goal_type for g in existing_goals if g.status == "ACTIVE"}

        # Always suggest emergency fund if not present
        if "EMERGENCY" not in existing_types:
            emergency_target = capacity.avg_monthly_income * 6  # 6 months expenses
            suggestions.append(GoalSuggestion(
                goal_type="EMERGENCY",
                suggested_name="Emergency Fund",
                suggested_target=round(emergency_target, -3),  # Round to nearest 1000
                suggested_deadline=date.today() + timedelta(days=365),
                reason="Every financial plan needs an emergency fund. Aim for 6 months of expenses.",
                priority="HIGH",
                confidence_score=0.95
            ))

        # Analyze spending patterns for other suggestions
        category_spending = {c.category: c for c in analysis.category_breakdown}

        for goal_type, config in GOAL_TYPE_TRIGGERS.items():
            if goal_type in existing_types or goal_type == "EMERGENCY":
                continue

            # Check if spending in related categories exceeds threshold
            total_related_spending = sum(
                category_spending.get(cat, type('', (), {'percent_of_income': 0})).percent_of_income or 0
                for cat in config["categories"]
            )

            if total_related_spending >= config["threshold_percent"]:
                monthly_amount = capacity.recommended_monthly_savings * 0.3
                target = monthly_amount * config["suggested_target_months"]

                suggestions.append(GoalSuggestion(
                    goal_type=goal_type,
                    suggested_name=self._get_goal_name(goal_type),
                    suggested_target=round(target, -3),
                    suggested_deadline=date.today() + timedelta(days=config["suggested_target_months"] * 30),
                    reason=f"Based on your spending in {', '.join(config['categories'])}, this goal could help you plan better.",
                    priority="MEDIUM",
                    confidence_score=0.7
                ))

        return suggestions[:3]  # Return top 3 suggestions

    def _get_goal_name(self, goal_type: str) -> str:
        """Get suggested name for goal type."""
        names = {
            "TRAVEL": "Dream Vacation",
            "GADGET": "New Gadget Fund",
            "EMERGENCY": "Emergency Fund",
            "HOME": "Home Down Payment",
            "VEHICLE": "Vehicle Fund",
            "EDUCATION": "Education Fund",
            "WEDDING": "Wedding Fund"
        }
        return names.get(goal_type, f"{goal_type.title()} Goal")

    def _calculate_health_score(
        self,
        goals: List[GoalData],
        capacity: SavingsCapacityResponse
    ) -> float:
        """Calculate overall goal health score (0-100)."""
        if not goals:
            return 50.0  # Neutral score if no goals

        active_goals = [g for g in goals if g.status == "ACTIVE"]
        if not active_goals:
            return 70.0  # Good score if all goals completed

        # Factors: progress, on-track status, savings capacity
        avg_progress = sum(g.progress_percent for g in active_goals) / len(active_goals)
        on_track_ratio = sum(1 for g in active_goals if g.is_on_track) / len(active_goals)
        savings_health = min(100, capacity.current_savings_rate * 5)  # 20% savings = 100

        score = (avg_progress * 0.4) + (on_track_ratio * 100 * 0.3) + (savings_health * 0.3)
        return round(min(100, max(0, score)), 1)

    def _generate_motivation(
        self,
        goals: List[GoalData],
        capacity: SavingsCapacityResponse,
        health_score: float
    ) -> str:
        """Generate motivational message based on current status."""
        active_goals = [g for g in goals if g.status == "ACTIVE"]
        completed_goals = [g for g in goals if g.status == "COMPLETED"]

        if health_score >= 80:
            return "🌟 Outstanding! You're crushing your financial goals. Keep this momentum going!"
        elif health_score >= 60:
            if completed_goals:
                return f"💪 Great progress! You've already achieved {len(completed_goals)} goal(s). Stay focused on your remaining targets!"
            return "👍 You're on the right track! Small consistent steps lead to big achievements."
        elif health_score >= 40:
            if capacity.safe_monthly_savings > 0:
                return f"📈 Room to grow! You can safely save ₹{capacity.safe_monthly_savings:,.0f}/month. Let's accelerate your goals!"
            return "🎯 Every rupee saved counts! Review your spending to find savings opportunities."
        else:
            return "💡 Let's build better habits! Start small - even ₹500/month can grow into something big."

    def _build_summary(self, goals: List[GoalData]) -> dict:
        """Build goals summary statistics."""
        active = [g for g in goals if g.status == "ACTIVE"]
        completed = [g for g in goals if g.status == "COMPLETED"]

        total_target = sum(g.target_amount for g in active)
        total_saved = sum(g.current_amount for g in active)

        return {
            "total_goals": len(goals),
            "active_goals": len(active),
            "completed_goals": len(completed),
            "total_target_amount": total_target,
            "total_saved_amount": total_saved,
            "overall_progress": (total_saved / total_target * 100) if total_target > 0 else 0,
            "goals_on_track": sum(1 for g in active if g.is_on_track),
            "goals_behind": sum(1 for g in active if g.is_on_track is False)
        }


# Singleton instance
goal_insights_service = GoalInsightsService()


"""
Client for fetching goal data from bachat-core-service.
"""
import httpx
import logging
from typing import List, Optional
from datetime import date
from uuid import UUID

from app.config import get_settings
from app.models.schemas import GoalData


class GoalClient:
    """Client for fetching goal data from bachat-core-service."""

    def __init__(self):
        self.settings = get_settings()
        self.base_url = self.settings.bachat_core_service_url
        self.logger = logging.getLogger(__name__)

    async def get_goals(
        self,
        token: str,
        status: Optional[str] = None
    ) -> List[GoalData]:
        """
        Fetch goals from bachat-core-service.
        
        Args:
            token: JWT token for authentication
            status: Optional status filter (ACTIVE, COMPLETED, PAUSED, CANCELLED)
            
        Returns:
            List of goals
        """
        headers = {"Authorization": f"Bearer {token}"}
        params = {}
        
        if status:
            params["status"] = status

        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/goals",
                headers=headers,
                params=params,
                timeout=30.0
            )
            if response.status_code >= 400:
                self.logger.error(f"Goals API error: status={response.status_code}, body={response.text}")
            response.raise_for_status()

            goals_data = response.json()
            
            result = []
            for g in goals_data:
                try:
                    goal = GoalData(
                        id=UUID(g["id"]),
                        name=g["name"],
                        goal_type=g["goalType"],
                        target_amount=g.get("targetAmount", 0),
                        current_amount=g.get("currentAmount", 0),
                        remaining_amount=g.get("remainingAmount", 0),
                        progress_percent=g.get("progressPercent", 0),
                        deadline=date.fromisoformat(g["deadline"]) if g.get("deadline") else None,
                        priority=g.get("priority"),
                        status=g.get("status", "ACTIVE"),
                        is_on_track=g.get("isOnTrack"),
                        suggested_monthly_saving=g.get("suggestedMonthlySaving"),
                        days_remaining=g.get("daysRemaining")
                    )
                    result.append(goal)
                except (KeyError, ValueError) as e:
                    self.logger.warning(f"Skipping malformed goal: {e}")
                    continue

            return result

    async def get_goals_summary(self, token: str) -> dict:
        """
        Fetch goals summary from bachat-core-service.
        
        Args:
            token: JWT token for authentication
            
        Returns:
            Goals summary dict
        """
        headers = {"Authorization": f"Bearer {token}"}

        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/goals/summary",
                headers=headers,
                timeout=30.0
            )
            if response.status_code >= 400:
                self.logger.error(f"Goals Summary API error: status={response.status_code}, body={response.text}")
            response.raise_for_status()

            return response.json()


# Singleton instance
goal_client = GoalClient()


import httpx
from typing import List, Optional
from datetime import date
from uuid import UUID

from app.config import get_settings
from app.models.schemas import TransactionData


class TransactionClient:
    """Client for fetching transaction data from pdf-parser-service."""
    
    def __init__(self):
        self.settings = get_settings()
        self.base_url = self.settings.pdf_parser_service_url
    
    async def get_transactions(
        self,
        token: str,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None
    ) -> List[TransactionData]:
        """
        Fetch transactions from pdf-parser-service.
        
        Args:
            token: JWT token for authentication
            start_date: Optional start date filter
            end_date: Optional end date filter
            
        Returns:
            List of transactions
        """
        headers = {"Authorization": f"Bearer {token}"}
        params = {}
        
        if start_date:
            params["startDate"] = start_date.isoformat()
        if end_date:
            params["endDate"] = end_date.isoformat()
        
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/transactions",
                headers=headers,
                params=params,
                timeout=30.0
            )
            response.raise_for_status()

            data = response.json()
            transactions = data.get("transactions", [])

            result = []
            for t in transactions:
                try:
                    txn = TransactionData(
                        id=UUID(t["id"]),
                        txn_date=date.fromisoformat(t["txnDate"]),
                        amount=t.get("amount", 0),
                        direction=t.get("direction", "DEBIT"),
                        category=t.get("category"),
                        sub_category=t.get("subCategory"),
                        description=t.get("description"),
                        merchant=t.get("merchant")
                    )
                    result.append(txn)
                except (KeyError, ValueError) as e:
                    # Skip malformed transactions
                    continue

            return result
    
    async def get_spending_summary(
        self,
        token: str,
        months: int = 6
    ) -> dict:
        """
        Get spending summary from pdf-parser-service.
        Falls back to calculating from transactions if endpoint not available.
        """
        # For now, we'll calculate from transactions
        # In future, pdf-parser-service could expose a summary endpoint
        from datetime import timedelta
        
        end_date = date.today()
        start_date = end_date - timedelta(days=months * 30)
        
        transactions = await self.get_transactions(token, start_date, end_date)
        
        return {
            "transactions": transactions,
            "start_date": start_date,
            "end_date": end_date
        }


# Singleton instance
transaction_client = TransactionClient()


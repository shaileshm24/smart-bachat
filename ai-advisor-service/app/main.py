import logging
import sys
import time
import uuid
from typing import Callable

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.config import get_settings
from app.routers import spending, recommendations, ai_goals
from app.security import ApiException, ApiErrorResponse

# Configure logging to show all logs in console
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

# Set specific loggers to INFO level
logging.getLogger("app.services.ai_goal_advisor").setLevel(logging.INFO)
logging.getLogger("app.routers.ai_goals").setLevel(logging.INFO)

logger = logging.getLogger("api")

settings = get_settings()


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Middleware to log all API requests and responses with unique identifiers."""

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        # Generate unique request ID
        request_id = str(uuid.uuid4())[:8]

        # Get request details
        method = request.method
        path = request.url.path
        query = str(request.query_params) if request.query_params else ""
        client_ip = request.client.host if request.client else "unknown"

        # Log request
        logger.info(f"[{request_id}] ➡️  REQUEST: {method} {path} {query} | Client: {client_ip}")

        # Track timing
        start_time = time.time()

        try:
            # Process request
            response = await call_next(request)

            # Calculate duration
            duration = time.time() - start_time

            # Log response
            status_code = response.status_code
            status_emoji = "✅" if status_code < 400 else "❌"

            logger.info(
                f"[{request_id}] {status_emoji} RESPONSE: {method} {path} | "
                f"Status: {status_code} | Duration: {duration:.2f}s"
            )

            # Add request ID to response headers
            response.headers["X-Request-ID"] = request_id

            return response

        except Exception as e:
            duration = time.time() - start_time
            logger.error(
                f"[{request_id}] ❌ ERROR: {method} {path} | "
                f"Exception: {type(e).__name__}: {str(e)} | Duration: {duration:.2f}s"
            )
            raise


app = FastAPI(
    title=settings.app_name,
    description="AI-powered financial advisor for SmartBachat - provides spending analysis and savings recommendations",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# Add request logging middleware (must be added before CORS)
app.add_middleware(RequestLoggingMiddleware)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(ApiException)
async def api_exception_handler(request: Request, exc: ApiException):
    """Handle ApiException and return structured JSON response."""
    return JSONResponse(
        status_code=exc.status_code,
        content=exc.error_response
    )


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    """Handle unexpected exceptions with structured error response."""
    error_response = ApiErrorResponse.internal_error(path=str(request.url.path))
    return JSONResponse(
        status_code=500,
        content=error_response
    )

# Include routers
app.include_router(spending.router)
app.include_router(recommendations.router)
app.include_router(ai_goals.router)


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "ai-advisor-service"}


@app.get("/")
async def root():
    """Root endpoint with API information."""
    return {
        "service": settings.app_name,
        "version": "1.0.0",
        "endpoints": {
            "spending_analysis": "/api/advisor/spending-analysis",
            "savings_capacity": "/api/advisor/savings-capacity",
            "goal_recommendation": "/api/advisor/goal-recommendation",
            "insights": "/api/advisor/insights",
            "ai_goal_advice": "/api/ai-goals/advice/{goal_id}",
            "ai_goal_suggestions": "/api/ai-goals/suggestions",
            "ai_transaction_impact": "/api/ai-goals/transaction-impact",
            "ai_spending_warnings": "/api/ai-goals/spending-warnings",
            "ai_comprehensive_advice": "/api/ai-goals/comprehensive-advice",
            "ai_should_i_buy": "/api/ai-goals/should-i-buy",
            "docs": "/docs"
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8089, reload=True)


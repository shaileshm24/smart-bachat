from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import spending, recommendations

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    description="AI-powered financial advisor for SmartBachat - provides spending analysis and savings recommendations",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(spending.router)
app.include_router(recommendations.router)


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
            "docs": "/docs"
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8082, reload=True)


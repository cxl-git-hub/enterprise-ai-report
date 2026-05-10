"""Main API router that aggregates all route modules."""

from fastapi import APIRouter

from app.api.auth import router as auth_router
from app.api.nl2sql import router as nl2sql_router
from app.api.analysis import router as analysis_router
from app.api.report import router as report_router
from app.api.traces import router as traces_router
from app.api.health import router as health_router

api_router = APIRouter(prefix="/api")

api_router.include_router(auth_router, prefix="/auth", tags=["Authentication"])
api_router.include_router(nl2sql_router, prefix="/ai", tags=["NL2SQL"])
api_router.include_router(analysis_router, prefix="/ai", tags=["Analysis"])
api_router.include_router(report_router, prefix="/ai", tags=["Report"])
api_router.include_router(traces_router, prefix="/ai", tags=["Traces"])
api_router.include_router(health_router, tags=["Health"])

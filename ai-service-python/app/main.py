"""FastAPI application entry point."""

import logging
import os
from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.router import api_router
from app.core.config import settings
from app.schemas.common import ApiResponse


# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.JSONRenderer(),
    ],
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    wrapper_class=structlog.stdlib.BoundLogger,
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager for startup/shutdown events."""
    logger.info("Starting AI Service", env=settings.APP_ENV)

    # Initialize MinIO bucket
    try:
        from app.core.minio_client import ensure_bucket_exists
        ensure_bucket_exists()
        logger.info("MinIO bucket ensured")
    except Exception as e:
        logger.warning("MinIO initialization skipped", error=str(e))

    yield

    logger.info("Shutting down AI Service")

    # Close Redis connection
    try:
        from app.core.redis import redis_client
        await redis_client.close()
    except Exception:
        pass

    # Dispose database engine
    try:
        from app.core.database import engine
        await engine.dispose()
    except Exception:
        pass


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    app = FastAPI(
        title="AI Service - Enterprise Reporting Platform",
        description="AI-powered natural language to SQL, data analysis, and report generation service",
        version="1.0.0",
        lifespan=lifespan,
        docs_url="/docs" if settings.DEBUG else None,
        redoc_url="/redoc" if settings.DEBUG else None,
    )

    # CORS middleware
    allowed_origins = os.getenv("CORS_ORIGINS", "http://localhost:3000,http://localhost:8080").split(",")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=allowed_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Include API routes
    app.include_router(api_router)

    # Global exception handler
    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        logger.error(
            "Unhandled exception",
            path=request.url.path,
            method=request.method,
            error=str(exc),
            exc_info=True,
        )
        return JSONResponse(
            status_code=500,
            content=ApiResponse(
                success=False,
                message="Internal server error",
                error_code="INTERNAL_ERROR",
            ).model_dump(),
        )

    # Request logging middleware
    @app.middleware("http")
    async def log_requests(request: Request, call_next):
        logger.info(
            "Request started",
            method=request.method,
            path=request.url.path,
        )
        response = await call_next(request)
        logger.info(
            "Request completed",
            method=request.method,
            path=request.url.path,
            status_code=response.status_code,
        )
        return response

    return app


app = create_app()

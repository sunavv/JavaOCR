"""FastAPI Main Application Entry Point."""

from contextlib import asynccontextmanager
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router as ocr_router
from app.core.config import settings

# Configure logging
logging.basicConfig(
    level=logging.INFO if not settings.app_debug else logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("ocr-module")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown lifespan events."""
    logger.info("Starting %s in %s mode...", settings.app_name, settings.app_env)
    logger.info("Configured OCR Engine: %s", settings.ocr_engine)
    logger.info("Preprocessing Enabled: %s (Deskew: %s, CLAHE: %s)", settings.enable_preprocessing, settings.deskew_enabled, settings.clahe_enabled)
    yield
    logger.info("Shutting down %s...", settings.app_name)


app = FastAPI(
    title=settings.app_name,
    description="Standalone and reusable Python OCR Module powered by PP-OCRv6 / PaddleOCR 3.x",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# Enable CORS for cross-origin clients (JavaFX, Electron, Web)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register API routes
app.include_router(ocr_router)


@app.get("/")
async def root():
    """Root endpoint redirecting to health check or documentation info."""
    return {
        "service": settings.app_name,
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/api/v1/health",
        "ocr_endpoint": "/api/v1/ocr",
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.app_debug,
    )

"""Application configuration module using Pydantic Settings."""

from typing import List
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application configuration loaded from environment variables and .env."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # General
    app_name: str = Field(default="Reusable-OCR-Service", description="API application name")
    app_env: str = Field(default="development", description="Environment: development, production, test")
    app_debug: bool = Field(default=True, description="Debug mode")
    port: int = Field(default=8000, description="Port to listen on")
    host: str = Field(default="0.0.0.0", description="Host binding")

    # OCR Engine
    ocr_engine: str = Field(default="paddle", description="Selected OCR engine: paddle, mock")
    paddle_use_angle_cls: bool = Field(default=True, description="Enable text orientation angle classification")
    paddle_lang: str = Field(default="en", description="Default OCR language")
    paddle_use_gpu: bool = Field(default=False, description="Use GPU acceleration if available")
    paddle_det_db_thresh: float = Field(default=0.3, description="DB detection threshold")
    paddle_det_db_box_thresh: float = Field(default=0.5, description="DB box detection threshold")

    # Image Preprocessing
    enable_preprocessing: bool = Field(default=True, description="Enable OpenCV image preprocessing pipeline")
    deskew_enabled: bool = Field(default=True, description="Enable automatic document deskewing")
    clahe_enabled: bool = Field(default=True, description="Enable Contrast Limited Adaptive Histogram Equalization")
    denoise_enabled: bool = Field(default=False, description="Enable Gaussian / bilateral denoising")

    # Document Validation
    max_image_size_mb: int = Field(default=20, description="Max allowed file size in MB")
    allowed_extensions_str: str = Field(default="jpg,jpeg,png,webp,bmp,tiff,tif", alias="ALLOWED_EXTENSIONS")

    @property
    def allowed_extensions(self) -> List[str]:
        """Return list of allowed file extensions."""
        return [ext.strip().lower() for ext in self.allowed_extensions_str.split(",") if ext.strip()]


# Global singleton settings
settings = Settings()

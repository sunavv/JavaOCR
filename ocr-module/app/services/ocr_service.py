"""OCR Service Layer.

Orchestrates document validation, preprocessing, OCR engine execution,
text normalization, and response assembly. Isolated from direct web framework logic.
"""

import logging
import mimetypes
import os
import time
from typing import Optional

from app.core.config import settings
from app.models.response import DocumentMetadata, OCRResponse, PreprocessingInfo, TextLine
from app.ocr.engine import OCREngine
from app.ocr.factory import get_ocr_engine
from app.ocr.preprocessing import ImagePreprocessor
from app.utils.text import clean_line_text, normalize_text

logger = logging.getLogger(__name__)


class OCRService:
    """Service handling high-level OCR workflows."""

    def __init__(
        self,
        engine: Optional[OCREngine] = None,
        preprocessor: Optional[ImagePreprocessor] = None,
    ):
        self.engine = engine or get_ocr_engine()
        self.preprocessor = preprocessor or ImagePreprocessor(
            enable_deskew=settings.deskew_enabled,
            enable_clahe=settings.clahe_enabled,
            enable_denoise=settings.denoise_enabled,
        )

    def validate_document(self, filename: str, content: bytes) -> None:
        """Validate uploaded file before OCR processing.

        Raises:
            ValueError: If file fails validation criteria.
        """
        if not content or len(content) == 0:
            raise ValueError("Document file is empty (0 bytes).")

        max_bytes = settings.max_image_size_mb * 1024 * 1024
        if len(content) > max_bytes:
            raise ValueError(f"Document exceeds maximum allowable size of {settings.max_image_size_mb}MB.")

        # Validate extension
        _, ext = os.path.splitext(filename)
        clean_ext = ext.lstrip(".").lower()
        if clean_ext not in settings.allowed_extensions:
            raise ValueError(
                f"File format '{ext}' is not supported. Supported extensions: {', '.join(settings.allowed_extensions)}"
            )

    def process_document(self, filename: str, content: bytes) -> OCRResponse:
        """Execute the end-to-end OCR pipeline on an uploaded document.

        Args:
            filename: Name of the uploaded file.
            content: Raw binary payload of the document.

        Returns:
            OCRResponse: Structured response model.
        """
        start_time = time.perf_counter()

        # 1. Validation
        self.validate_document(filename, content)

        # 2. Image Decoding
        image = self.preprocessor.decode_image_bytes(content)
        h, w = image.shape[:2]
        channels = image.shape[2] if len(image.shape) == 3 else 1

        mime_type, _ = mimetypes.guess_type(filename)
        doc_meta = DocumentMetadata(
            filename=filename,
            type=mime_type or "application/octet-stream",
            width=w,
            height=h,
            channels=channels,
            size_bytes=len(content),
        )

        # 3. Preprocessing
        prep_info: Optional[PreprocessingInfo] = None
        if settings.enable_preprocessing:
            prep_res = self.preprocessor.process(image)
            image_to_ocr = prep_res.processed_image
            prep_info = PreprocessingInfo(
                applied=True,
                deskew_angle=prep_res.deskew_angle,
                clahe_applied=prep_res.clahe_applied,
                denoise_applied=prep_res.denoise_applied,
            )
        else:
            image_to_ocr = image
            prep_info = PreprocessingInfo(applied=False)

        # 4. OCR Extraction
        engine_result = self.engine.extract_text(image_to_ocr)

        # 5. Text Normalization and Structuring
        structured_lines: list[TextLine] = []
        for line in engine_result.lines:
            cleaned = clean_line_text(line.text)
            if cleaned:
                structured_lines.append(
                    TextLine(
                        text=cleaned,
                        confidence=line.confidence,
                        bbox=line.bbox,
                        polygon=line.polygon,
                    )
                )

        normalized_full_text = normalize_text(
            "\n".join(l.text for l in structured_lines) if structured_lines else engine_result.raw_text
        )

        elapsed_ms = round((time.perf_counter() - start_time) * 1000.0, 2)

        return OCRResponse(
            success=True,
            processing_time_ms=elapsed_ms,
            document=doc_meta,
            text=normalized_full_text,
            lines=structured_lines,
            preprocessing=prep_info,
            engine=engine_result.engine_name,
            error=None,
        )

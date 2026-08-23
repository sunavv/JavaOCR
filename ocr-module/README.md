# Python OCR Microservice (PP-OCRv6 / PaddleOCR 3.x)

Standalone, reusable Python OCR module designed to provide high-performance document text extraction and spatial structure recovery over a REST API.

---

## Features

- **PaddleOCR 3.x with PP-OCRv6 pipeline**: State-of-the-art text detection and recognition.
- **OpenCV Preprocessing**:
  - Automatic document deskewing via contour analysis & `minAreaRect`
  - CLAHE (Contrast Limited Adaptive Histogram Equalization)
  - Color space normalization and noise reduction
- **Modular Engine Interface**: Isolated behind the `OCREngine` abstract base class, allowing seamless engine swaps (e.g. EasyOCR, Tesseract, Apple Vision) with zero REST API changes.
- **Strict Separation of Concerns**: Zero business logic, validation rules, or kiosk-specific logic in Python.
- **FastAPI REST Service**: Exposes `POST /api/v1/ocr` and `GET /api/v1/health` with detailed spatial metadata (polygons, bounding boxes, confidence, timings).

---

## Project Structure

```
ocr-module/
├── app/
│   ├── api/
│   │   └── routes.py            # FastAPI endpoints
│   ├── core/
│   │   └── config.py            # Pydantic Settings
│   ├── ocr/
│   │   ├── engine.py            # Abstract OCREngine interface
│   │   ├── paddle_engine.py     # PaddleOCR 3.x / PP-OCRv6 implementation
│   │   ├── mock_engine.py       # Deterministic mock engine for testing/CI
│   │   ├── factory.py           # Engine factory
│   │   └── preprocessing.py     # OpenCV deskewing & CLAHE pipeline
│   ├── models/
│   │   └── response.py          # Structured Pydantic response models
│   ├── services/
│   │   └── ocr_service.py       # Service orchestration layer
│   ├── utils/
│   │   ├── text.py              # Text cleaning & NFKC normalization
│   │   └── dataset_generator.py # Synthetic benchmark dataset generator
│   └── main.py                  # FastAPI entry point
├── tests/                       # Pytest test suite (16 tests)
│   ├── conftest.py
│   ├── test_api.py
│   ├── test_engine.py
│   ├── test_text_normalization.py
│   └── data/                    # Generated test documents
├── requirements.txt
└── .env.example
```

---

## Installation & Execution

```bash
# 1. Activate virtual environment
source ../.venv/bin/activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Copy environment configuration
cp .env.example .env

# 4. Start the service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## Running Automated Tests

```bash
export PYTHONPATH=$(pwd):$PYTHONPATH
pytest tests/ -v
```

# Reusable Python OCR Module (PP-OCRv6 / PaddleOCR 3.x) & JavaFX Verification Prototype

A modular, standalone Python OCR microservice powered by **PaddleOCR 3.x with the PP-OCRv6 pipeline**, accompanied by a decoupled **JavaFX prototype client** for document identity verification.

---

## Architecture Overview

```
                          ┌───────────────────────────┐
                          │   JavaFX Client / Web     │
                          │ (Application Validation)  │
                          └─────────────┬─────────────┘
                                        │ HTTP POST multipart/form-data
                                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ Python OCR Microservice (FastAPI)                                       │
│                                                                         │
│  [POST /api/v1/ocr] ───► [Validation] ───► [OpenCV Preprocessing]      │
│                                                   │ (Deskew, CLAHE)    │
│                                                   ▼                     │
│  [Normalized JSON Response] ◄─── [Text Normalizer] ◄─── [OCREngine]     │
│                                                     (PaddleOCR PP-OCRv6)│
└─────────────────────────────────────────────────────────────────────────┘
```

### Strict Separation of Responsibilities

- **Python OCR Service**: Strictly extracts text, line bounding boxes `[x1, y1, x2, y2]`, polygon geometry, confidence scores, and preprocessing telemetry. **Contains zero kiosk-specific or verification business logic.**
- **JavaFX Client**: Handles desktop UI, image selection, bounding box overlays, and client-side name normalization / matching against extracted text, computing status badges (`VERIFIED`, `NOT VERIFIED`, `OCR FAILED / RESCAN`).

---

## Directory Structure

```
.
├── ocr-module/                     # Standalone Python OCR Microservice
│   ├── app/
│   │   ├── api/
│   │   │   └── routes.py           # FastAPI endpoints (/api/v1/ocr, /api/v1/health)
│   │   ├── core/
│   │   │   └── config.py           # Pydantic Settings (.env configuration)
│   │   ├── ocr/
│   │   │   ├── engine.py           # Abstract OCREngine interface & data models
│   │   │   ├── paddle_engine.py    # PaddleOCR 3.x / PP-OCRv6 engine implementation
│   │   │   ├── mock_engine.py      # Deterministic Mock engine for testing/CI
│   │   │   ├── factory.py          # Dynamic engine factory
│   │   │   └── preprocessing.py    # OpenCV deskewing, CLAHE, denoising
│   │   ├── models/
│   │   │   └── response.py         # Standardized Pydantic response models
│   │   ├── services/
│   │   │   └── ocr_service.py      # Business workflow orchestration
│   │   ├── utils/
│   │   │   ├── text.py             # Text cleaning & NFKC normalization
│   │   │   └── dataset_generator.py # Synthetic benchmark dataset generator
│   │   └── main.py                 # FastAPI application & CORS setup
│   ├── tests/                      # Automated test suite (16 tests)
│   │   ├── test_api.py             # FastAPI REST endpoint integration tests
│   │   ├── test_engine.py          # OCR engine & OpenCV preprocessing tests
│   │   └── test_text_normalization.py
│   ├── requirements.txt
│   ├── .env.example
│   └── README.md
│
└── javafx-client/                  # JavaFX Prototype Client
    ├── pom.xml                     # Maven build file (JavaFX 21, Jackson, JUnit 5)
    ├── README.md
    └── src/
        ├── main/
        │   ├── java/com/ocr/client/
        │   │   ├── App.java
        │   │   ├── controller/MainController.java
        │   │   ├── model/ (OcrResponse, TextLine, DocumentMeta, VerificationStatus)
        │   │   └── service/ (OcrApiClient, VerificationService)
        │   └── resources/com/ocr/client/
        │       ├── main_view.fxml
        │       ├── styles.css
        │       └── test-samples/   # Bundled test images
        └── test/java/com/ocr/client/service/
            └── VerificationServiceTest.java
```

---

## Quick Start Guide

### 1. Start the Python OCR Microservice

```bash
# 1. Navigate to the project root (adjust path to wherever you cloned it)
cd FinalModule

# 2. Activate virtual environment
source .venv/bin/activate

# 3. Start the FastAPI service
cd ocr-module
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The service will be available at:
- **API Base URL**: `http://127.0.0.1:8000`
- **Interactive Swagger Docs**: `http://127.0.0.1:8000/docs`
- **Health Check**: `http://127.0.0.1:8000/api/v1/health`

### 2. Run the JavaFX Verification Prototype Client

In a separate terminal:

```bash
cd FinalModule/javafx-client

# Run the JavaFX Application (connects to http://127.0.0.1:8000 by default)
# No separate build step needed — mvn javafx:run compiles automatically before launching.
mvn javafx:run

# If you hit stale-class or classpath errors, clean first:
mvn clean javafx:run
```

---

## Running on a Different Server

The OCR service and JavaFX client are fully decoupled via HTTP. Here is how to
connect them when the service runs on a separate machine or port.

### 1. Expose the OCR Service on a Remote Host

By default the service binds to `0.0.0.0` (all interfaces), so any port-accessible
machine on the same network can reach it. Just pass the host's IP or a custom port:

```bash
# Example: bind to all interfaces on port 9000
uvicorn app.main:app --host 0.0.0.0 --port 9000
```

You can also set `HOST` and `PORT` in `ocr-module/.env` (copy from `.env.example`):

```env
HOST=0.0.0.0
PORT=9000
```

### 2. Point the JavaFX Client at the Remote Server

The `OcrApiClient` accepts any base URL in its constructor. Pass the remote
address when instantiating it in `MainController`:

```java
// Default (localhost)
OcrApiClient client = new OcrApiClient();

// Remote server
OcrApiClient client = new OcrApiClient("http://192.168.1.50:9000");
```

Alternatively, set the URL via a system property so you do not need to recompile:

```java
// In OcrApiClient constructor — read from system property with fallback
String baseUrl = System.getProperty("ocr.server.url", "http://127.0.0.1:8000");
```

Then launch the client with:

```bash
mvn javafx:run -Docr.server.url=http://192.168.1.50:9000
```

### 3. Verify Connectivity

Before launching the full client, confirm the service is reachable:

```bash
curl http://<server-ip>:<port>/api/v1/health
# Expected: {"status": "healthy", ...}
```

---

## Testing & Verification

### Run Python Automated Tests (Pytest)
```bash
source .venv/bin/activate
export PYTHONPATH=$(pwd)/ocr-module:$PYTHONPATH
pytest ocr-module/tests -v
```

### Run JavaFX Unit Tests (JUnit 5)
```bash
cd javafx-client
mvn test
```

---

## API Specification

### `POST /api/v1/ocr`
Uploads a document image and returns recognized text with bounding boxes, confidence, and preprocessing telemetry.

**Request:** `multipart/form-data` with `file` parameter (PNG, JPEG, WebP, BMP, TIFF).

**Example Response:**
```json
{
  "success": true,
  "processing_time_ms": 145.2,
  "document": {
    "filename": "document.png",
    "type": "image/png",
    "width": 1000,
    "height": 650,
    "channels": 3,
    "size_bytes": 82992
  },
  "text": "OFFICIAL IDENTIFICATION DOCUMENT\nName: Sunav Sharma\nDocument No: ABC123456\nDate of Birth: 15/08/1990\nNationality: Nepalese",
  "lines": [
    {
      "text": "Name: Sunav Sharma",
      "confidence": 0.9998,
      "bbox": [64, 124, 180, 149],
      "polygon": [[64, 124], [180, 124], [180, 149], [64, 149]]
    },
    {
      "text": "Document No: ABC123456",
      "confidence": 0.9987,
      "bbox": [62, 183, 204, 207],
      "polygon": [[62, 183], [204, 183], [204, 207], [62, 207]]
    }
  ],
  "preprocessing": {
    "applied": true,
    "deskew_angle": -5.5,
    "clahe_applied": true,
    "denoise_applied": false
  },
  "engine": "paddleocr_ppocrv6",
  "error": null
}
```

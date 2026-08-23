# Python OCR Module + JavaFX Client

A standalone **Python OCR microservice** powered by [PaddleOCR 3.x (PP-OCRv6)](https://github.com/PaddlePaddle/PaddleOCR), paired with a **JavaFX desktop client** that consumes the REST API.

---

## Features

- **PP-OCRv6 pipeline** — state-of-the-art OCR accuracy via PaddleOCR 3.x
- **OpenCV preprocessing** — automatic deskewing, CLAHE contrast enhancement, and optional denoising
- **FastAPI REST API** — `multipart/form-data` upload, JSON response with text, bounding boxes, polygons, and confidence scores
- **Pluggable engine** — swap between `paddle` (real OCR) and `mock` (deterministic, no GPU needed) via `.env`
- **JavaFX desktop client** — image picker, live bounding-box overlay, and text extraction results
- **Decoupled architecture** — the service and client communicate over HTTP; run them on the same machine or different hosts

---

## Architecture

```
  ┌──────────────────────────┐
  │     JavaFX Client        │
  │  (Desktop Application)   │
  └────────────┬─────────────┘
               │ HTTP POST multipart/form-data
               ▼
┌──────────────────────────────────────────────────────┐
│ Python OCR Microservice (FastAPI)                    │
│                                                      │
│  POST /api/v1/ocr                                    │
│    → Validation                                      │
│    → OpenCV Preprocessing  (Deskew, CLAHE)           │
│    → PaddleOCR PP-OCRv6 Engine                       │
│    → Text Normalizer (NFKC)                          │
│    → Normalized JSON Response                        │
└──────────────────────────────────────────────────────┘
```

---

## Repository Structure

```
.
├── ocr-module/                     # Python OCR Microservice
│   ├── app/
│   │   ├── api/routes.py           # FastAPI endpoints
│   │   ├── core/config.py          # Pydantic Settings (.env)
│   │   ├── ocr/
│   │   │   ├── engine.py           # Abstract OCREngine + data models
│   │   │   ├── paddle_engine.py    # PaddleOCR 3.x / PP-OCRv6
│   │   │   ├── mock_engine.py      # Deterministic mock (no GPU)
│   │   │   ├── factory.py          # Engine factory
│   │   │   └── preprocessing.py   # OpenCV pipeline
│   │   ├── models/response.py      # Pydantic response schemas
│   │   ├── services/ocr_service.py # Workflow orchestration
│   │   ├── utils/text.py           # NFKC text normalization
│   │   └── main.py                 # FastAPI app + CORS
│   ├── requirements.txt
│   └── .env.example
│
└── javafx-client/                  # JavaFX Desktop Client
    ├── pom.xml                     # Maven (JavaFX 21, Jackson, JUnit 5)
    └── src/main/java/com/ocr/client/
        ├── App.java
        ├── controller/MainController.java
        ├── model/                  # OcrResponse, TextLine, DocumentMeta
        └── service/                # OcrApiClient, VerificationService
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Python | 3.9+ |
| Java | 21+ |
| Maven | 3.8+ |
| pip | latest |

---

## Quick Start

### 1. Start the OCR Service

```bash
# Clone and enter the repo
git clone https://github.com/sunavv/JavaOCR.git
cd JavaOCR

# Create and activate a virtual environment
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate

# Install dependencies
pip install -r ocr-module/requirements.txt

# Copy env config (edit as needed)
cp ocr-module/.env.example ocr-module/.env

# Start the service
cd ocr-module
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The service will be available at:

| Endpoint | URL |
|---|---|
| API base | `http://127.0.0.1:8000` |
| Swagger UI | `http://127.0.0.1:8000/docs` |
| Health check | `http://127.0.0.1:8000/api/v1/health` |

### 2. Run the JavaFX Client

In a separate terminal:

```bash
cd JavaOCR/javafx-client

# Compile and launch (no separate build step needed)
mvn javafx:run

# Clean build if you hit classpath issues
mvn clean javafx:run
```

By default the client connects to `http://127.0.0.1:8000`. To point it at a remote server:

```bash
mvn javafx:run -Docr.server.url=http://192.168.1.50:8000
```

---

## Configuration

All service settings are controlled via `ocr-module/.env` (copy from `.env.example`):

```env
OCR_ENGINE=paddle          # paddle | mock
PORT=8000
HOST=0.0.0.0
ENABLE_PREPROCESSING=true
DESKEW_ENABLED=true
CLAHE_ENABLED=true
PADDLE_USE_GPU=false
```

---

## API Reference

### `POST /api/v1/ocr`

Upload an image and receive extracted text with bounding boxes.

**Request:** `multipart/form-data` — field `file` (PNG, JPEG, WebP, BMP, TIFF, max 20 MB)

**Response:**
```json
{
  "success": true,
  "processing_time_ms": 145.2,
  "document": { "filename": "doc.png", "width": 1000, "height": 650 },
  "text": "Name: Jane Doe\nDocument No: XYZ789",
  "lines": [
    {
      "text": "Name: Jane Doe",
      "confidence": 0.9998,
      "bbox": [64, 124, 180, 149],
      "polygon": [[64,124],[180,124],[180,149],[64,149]]
    }
  ],
  "preprocessing": {
    "applied": true,
    "deskew_angle": -1.5,
    "clahe_applied": true,
    "denoise_applied": false
  },
  "engine": "paddleocr_ppocrv6",
  "error": null
}
```

### `GET /api/v1/health`

Returns service status and loaded engine.

---

## Running Tests

```bash
# Python (pytest)
source .venv/bin/activate
export PYTHONPATH=$(pwd)/ocr-module:$PYTHONPATH
pytest ocr-module/tests -v

# Java (JUnit 5)
cd javafx-client
mvn test
```

---

## Tech Stack

- **OCR Engine** — PaddleOCR 3.x, PP-OCRv6
- **Image preprocessing** — OpenCV (deskew, CLAHE, denoise)
- **Service framework** — FastAPI + Uvicorn
- **Configuration** — Pydantic Settings
- **Desktop client** — JavaFX 21, FXML
- **HTTP** — Java `HttpClient` (JDK 11+)
- **JSON** — Jackson Databind
- **Testing** — pytest, JUnit 5

---

## License

MIT

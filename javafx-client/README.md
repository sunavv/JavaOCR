# JavaFX OCR Verification Prototype Client

Desktop prototype client built with JavaFX 21 to test and demonstrate integration with the independent Python OCR service.

---

## Features

- **Document Selection & Preview**: Load image documents (PNG, JPEG, WebP, etc.) or select from the pre-bundled test sample suite.
- **Visual Bounding Box Canvas**: Overlays spatial bounding boxes and polygons on the loaded document, highlighting the selected text lines.
- **Client-Side Identity Verification**:
  - Resilient name normalization (stripping prefixes like Mr./Dr., casing, punctuation).
  - Exact & Levenshtein fuzzy matching directly in JavaFX.
  - Verification Status Banner (`VERIFIED`, `NOT VERIFIED`, `OCR FAILED / RESCAN`).
- **Telemetry & Performance View**: Shows confidence levels, backend server processing time, active OCR engine, and OpenCV preprocessing parameters.

---

## Requirements

- JDK 21+
- Apache Maven 3.8+
- Running Python OCR Service on `http://127.0.0.1:8000`

---

## Build & Run

```bash
# 1. Run Unit Tests (JUnit 5)
mvn test

# 2. Launch the Application
mvn javafx:run
```

"""Text normalization and cleaning utilities for the OCR module."""

import re
import unicodedata
from typing import List


def normalize_text(text: str) -> str:
    """Perform standard normalization on recognized OCR text.

    - Decomposes and normalizes Unicode characters (NFKC)
    - Replaces non-standard whitespace and tabs with single spaces
    - Strips leading and trailing whitespace from lines
    - Preserves logical paragraph breaks
    """
    if not text:
        return ""

    # Normalize unicode to NFKC (combines characters, converts full-width to half-width)
    normalized = unicodedata.normalize("NFKC", text)

    # Normalize carriage returns
    normalized = normalized.replace("\r\n", "\n").replace("\r", "\n")

    # Split lines, normalize each line's whitespace, and re-join
    lines = normalized.split("\n")
    cleaned_lines: List[str] = []
    for line in lines:
        # Collapse multiple internal whitespaces to a single space
        cleaned_line = re.sub(r"[ \t\f\v]+", " ", line).strip()
        cleaned_lines.append(cleaned_line)

    # Collapse consecutive blank lines to at most one empty line
    result = "\n".join(cleaned_lines)
    result = re.sub(r"\n{3,}", "\n\n", result)
    return result.strip()


def clean_line_text(line_text: str) -> str:
    """Clean a single recognized text line snippet."""
    if not line_text:
        return ""
    normalized = unicodedata.normalize("NFKC", line_text)
    # Strip non-printable control characters except standard space
    cleaned = "".join(ch for ch in normalized if unicodedata.category(ch)[0] != "C" or ch == " ")
    return re.sub(r"\s+", " ", cleaned).strip()

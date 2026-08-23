package com.ocr.client.model;

public enum VerificationStatus {
    IDLE("Ready for Document", "#64748b"),
    VERIFIED("VERIFIED", "#10b981"),
    NOT_VERIFIED("NOT VERIFIED", "#f59e0b"),
    OCR_FAILED_RESCAN("OCR FAILED / RESCAN", "#ef4444");

    private final String label;
    private final String colorHex;

    VerificationStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }
}

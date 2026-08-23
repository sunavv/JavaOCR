package com.ocr.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrResponse {
    private boolean success;

    @JsonProperty("processing_time_ms")
    private double processingTimeMs;

    private DocumentMeta document;
    private String text = "";
    private List<TextLine> lines = new ArrayList<>();
    private String engine = "";
    private String error;

    public OcrResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public double getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(double processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public DocumentMeta getDocument() {
        return document;
    }

    public void setDocument(DocumentMeta document) {
        this.document = document;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<TextLine> getLines() {
        return lines != null ? lines : new ArrayList<>();
    }

    public void setLines(List<TextLine> lines) {
        this.lines = lines;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

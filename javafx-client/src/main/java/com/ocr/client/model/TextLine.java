package com.ocr.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextLine {
    private String text;
    private double confidence;
    private List<Integer> bbox;
    private List<List<Integer>> polygon;

    public TextLine() {
    }

    public TextLine(String text, double confidence, List<Integer> bbox) {
        this.text = text;
        this.confidence = confidence;
        this.bbox = bbox;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<Integer> getBbox() {
        return bbox;
    }

    public void setBbox(List<Integer> bbox) {
        this.bbox = bbox;
    }

    public List<List<Integer>> getPolygon() {
        return polygon;
    }

    public void setPolygon(List<List<Integer>> polygon) {
        this.polygon = polygon;
    }

    @Override
    public String toString() {
        return String.format("%s (%.1f%%)", text, confidence * 100);
    }
}

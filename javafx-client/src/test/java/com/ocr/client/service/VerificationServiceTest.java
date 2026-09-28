package com.ocr.client.service;

import com.ocr.client.model.OcrResponse;
import com.ocr.client.model.TextLine;
import com.ocr.client.model.VerificationStatus;
import com.ocr.client.service.VerificationService.VerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationServiceTest {

    private VerificationService service;

    @BeforeEach
    public void setup() {
        service = new VerificationService();
    }

    @Test
    public void testNormalizeName() {
        assertEquals("sunav sharma", service.normalize("Mr. Sunav Sharma"));
        assertEquals("sunav sharma", service.normalize("  Dr.  Sunav   Sharma! "));
        assertEquals("sunav sharma", service.normalize("SUNAV SHARMA"));
    }

    @Test
    public void testLevenshteinAndSimilarity() {
        assertEquals(0, service.levenshteinDistance("sunav", "sunav"));
        assertEquals(1, service.levenshteinDistance("sunav", "sunaw"));
        assertTrue(service.similarity("sunav sharma", "sunaw sharma") > 0.90);
    }

    @Test
    public void testVerifyNameExactMatch() {
        OcrResponse response = new OcrResponse();
        response.setSuccess(true);
        response.setText("OFFICIAL DOCUMENT\nName: Sunav Sharma\nDocument No: ABC123456");

        List<TextLine> lines = new ArrayList<>();
        lines.add(new TextLine("OFFICIAL DOCUMENT", 0.99, List.of(0, 0, 100, 20)));
        lines.add(new TextLine("Name: Sunav Sharma", 0.98, List.of(0, 30, 100, 50)));
        lines.add(new TextLine("Document No: ABC123456", 0.95, List.of(0, 60, 100, 80)));
        response.setLines(lines);

        VerificationResult result = service.verifyName(response, "Sunav Sharma");
        assertEquals(VerificationStatus.VERIFIED, result.getStatus());
        assertEquals(1.0, result.getMatchScore());
    }

    @Test
    public void testVerifyNameFuzzyMatch() {
        OcrResponse response = new OcrResponse();
        response.setSuccess(true);
        response.setText("Name: Sunaw Sharma");

        List<TextLine> lines = new ArrayList<>();
        lines.add(new TextLine("Name: Sunaw Sharma", 0.92, List.of(0, 0, 100, 20)));
        response.setLines(lines);

        // One typo from OCR ('w' instead of 'v')
        VerificationResult result = service.verifyName(response, "Sunav Sharma");
        assertEquals(VerificationStatus.VERIFIED, result.getStatus());
        assertTrue(result.getMatchScore() >= 0.80);
    }

    @Test
    public void testVerifyNameNotFound() {
        OcrResponse response = new OcrResponse();
        response.setSuccess(true);
        response.setText("Name: John Doe\nDocument No: XYZ999");

        List<TextLine> lines = new ArrayList<>();
        lines.add(new TextLine("Name: John Doe", 0.98, List.of(0, 0, 100, 20)));
        response.setLines(lines);

        VerificationResult result = service.verifyName(response, "Sunav Sharma");
        assertEquals(VerificationStatus.NOT_VERIFIED, result.getStatus());
    }

    @Test
    public void testVerifyOcrFailedBlank() {
        OcrResponse response = new OcrResponse();
        response.setSuccess(true);
        response.setText("");
        response.setLines(new ArrayList<>());

        VerificationResult result = service.verifyName(response, "Sunav Sharma");
        assertEquals(VerificationStatus.OCR_FAILED_RESCAN, result.getStatus());
    }
}

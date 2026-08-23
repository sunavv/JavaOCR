package com.ocr.client.service;

import com.ocr.client.model.OcrResponse;
import com.ocr.client.model.TextLine;
import com.ocr.client.model.VerificationStatus;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Client-side verification and name comparison service.
 * All application-specific validation and matching logic is isolated here.
 */
public class VerificationService {

    private static final Pattern TITLE_PREFIXES = Pattern.compile("^(mr|mrs|ms|dr|prof)\\.?\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    public static class VerificationResult {
        private final VerificationStatus status;
        private final String message;
        private final double matchScore;
        private final String matchedLine;

        public VerificationResult(VerificationStatus status, String message, double matchScore, String matchedLine) {
            this.status = status;
            this.message = message;
            this.matchScore = matchScore;
            this.matchedLine = matchedLine;
        }

        public VerificationStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public double getMatchScore() {
            return matchScore;
        }

        public String getMatchedLine() {
            return matchedLine;
        }
    }

    /**
     * Normalize a name string for resilient comparison.
     */
    public String normalize(String input) {
        if (input == null) {
            return "";
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        s = TITLE_PREFIXES.matcher(s).replaceFirst("");
        s = SPECIAL_CHARS.matcher(s).replaceAll(" ");
        s = MULTIPLE_SPACES.matcher(s).replaceAll(" ");
        return s.trim();
    }

    /**
     * Compute Levenshtein distance between two strings.
     */
    public int levenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    /**
     * Compute similarity ratio between 0.0 and 1.0.
     */
    public double similarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int dist = levenshteinDistance(s1, s2);
        return 1.0 - ((double) dist / (double) maxLen);
    }

    /**
     * Verify whether the expected name matches the OCR response document.
     */
    public VerificationResult verifyName(OcrResponse response, String expectedName) {
        if (response == null || !response.isSuccess()) {
            return new VerificationResult(
                    VerificationStatus.OCR_FAILED_RESCAN,
                    "OCR response was unsuccessful or empty. Please rescan.",
                    0.0,
                    null
            );
        }

        if (response.getLines().isEmpty() && (response.getText() == null || response.getText().trim().isEmpty())) {
            return new VerificationResult(
                    VerificationStatus.OCR_FAILED_RESCAN,
                    "No text could be extracted from document. Image might be blank or blurry.",
                    0.0,
                    null
            );
        }

        String normExpected = normalize(expectedName);
        if (normExpected.isEmpty()) {
            return new VerificationResult(
                    VerificationStatus.NOT_VERIFIED,
                    "Please enter an expected name to verify.",
                    0.0,
                    null
            );
        }

        double highestScore = 0.0;
        String bestMatchedLine = null;

        // 1. Check direct line matches
        for (TextLine line : response.getLines()) {
            String normLine = normalize(line.getText());
            if (normLine.isEmpty()) {
                continue;
            }

            // Exact substring check
            if (normLine.contains(normExpected)) {
                return new VerificationResult(
                        VerificationStatus.VERIFIED,
                        String.format("Exact match found in line: \"%s\" (Confidence: %.1f%%)",
                                line.getText(), line.getConfidence() * 100),
                        1.0,
                        line.getText()
                );
            }

            // Fuzzy similarity check
            double score = similarity(normExpected, normLine);
            if (score > highestScore) {
                highestScore = score;
                bestMatchedLine = line.getText();
            }

            // Token-based matching (e.g. line is "Name Sunav Sharma")
            String[] tokens = normLine.split(" ");
            for (int i = 0; i < tokens.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < tokens.length; j++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(tokens[j]);
                    double tokenScore = similarity(normExpected, sb.toString());
                    if (tokenScore > highestScore) {
                        highestScore = tokenScore;
                        bestMatchedLine = line.getText();
                    }
                }
            }
        }

        // 2. Also check against normalized full text
        String normFull = normalize(response.getText());
        if (normFull.contains(normExpected)) {
            return new VerificationResult(
                    VerificationStatus.VERIFIED,
                    "Match confirmed in document text.",
                    1.0,
                    bestMatchedLine
            );
        }

        // Fuzzy match threshold (80% similarity or above)
        if (highestScore >= 0.80) {
            return new VerificationResult(
                    VerificationStatus.VERIFIED,
                    String.format("Fuzzy match confirmed (%.1f%% similarity) with: \"%s\"",
                            highestScore * 100, bestMatchedLine),
                    highestScore,
                    bestMatchedLine
            );
        }

        return new VerificationResult(
                VerificationStatus.NOT_VERIFIED,
                String.format("Expected name \"%s\" was not found in document. Best similarity: %.1f%%",
                        expectedName, highestScore * 100),
                highestScore,
                bestMatchedLine
        );
    }
}

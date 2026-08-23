package com.ocr.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocr.client.model.OcrResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP Client for connecting to the Python FastAPI OCR Module.
 */
public class OcrApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OcrApiClient() {
        this("http://103.143.211.26:8000");
    }

    public CompletableFuture<Boolean> checkHealth() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }

    public OcrApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<OcrResponse> uploadDocumentAsync(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadDocument(file);
            } catch (Exception e) {
                OcrResponse errResp = new OcrResponse();
                errResp.setSuccess(false);
                errResp.setError("Network/API Error: " + e.getMessage());
                return errResp;
            }
        });
    }

    public OcrResponse uploadDocument(File file) throws IOException, InterruptedException {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }

        String boundary = "Boundary-" + UUID.randomUUID().toString();
        byte[] multipartBody = createMultipartBody(file, boundary);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/ocr"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.body() == null || response.body().trim().isEmpty()) {
            OcrResponse err = new OcrResponse();
            err.setSuccess(false);
            err.setError("Empty response from OCR API (Status " + response.statusCode() + ")");
            return err;
        }

        return objectMapper.readValue(response.body(), OcrResponse.class);
    }

    private byte[] createMultipartBody(File file, String boundary) throws IOException {
        String filename = file.getName();
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        byte[] fileBytes = Files.readAllBytes(file.toPath());

        StringBuilder headerSb = new StringBuilder();
        headerSb.append("--").append(boundary).append("\r\n");
        headerSb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        headerSb.append("Content-Type: ").append(contentType).append("\r\n\r\n");

        byte[] headerBytes = headerSb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

        return body;
    }
}

package com.ocr.client.controller;

import com.ocr.client.model.OcrResponse;
import com.ocr.client.model.TextLine;
import com.ocr.client.model.VerificationStatus;
import com.ocr.client.service.OcrApiClient;
import com.ocr.client.service.VerificationService;
import com.ocr.client.service.VerificationService.VerificationResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class MainController {

    @FXML
    private TextField serverUrlField;
    @FXML
    private Button checkConnectionButton;
    @FXML
    private Label connectionStatusLabel;

    @FXML
    private Button chooseFileButton;
    @FXML
    private ComboBox<String> sampleSelector;
    @FXML
    private Button runOcrButton;
    @FXML
    private Label selectedFileLabel;

    @FXML
    private StackPane imageContainer;
    @FXML
    private Canvas documentCanvas;
    @FXML
    private Label imageDimsLabel;
    @FXML
    private Label fileSizeLabel;
    @FXML
    private CheckBox showBoxesCheckbox;

    @FXML
    private TextField   expectedNameField;
    @FXML
    private HBox statusBadgeBox;
    @FXML
    private Label statusBadgeText;
    @FXML
    private Label verificationMessageLabel;

    @FXML
    private ListView<String> linesListView;
    @FXML
    private TextArea fullTextArea;

    @FXML
    private Label engineLabel;
    @FXML
    private Label processingTimeLabel;
    @FXML
    private Label lineCountLabel;
    @FXML
    private Label deskewAngleLabel;
    @FXML
    private Label claheStatusLabel;

    private OcrApiClient apiClient;
    private final VerificationService verificationService = new VerificationService();

    private File currentSelectedFile;
    private Image currentLoadedImage;
    private OcrResponse lastOcrResponse;

    @FXML
    public void initialize() {
        apiClient = new OcrApiClient(serverUrlField.getText());
        handleCheckConnection();

        // Populate sample document dropdown
        sampleSelector.setItems(FXCollections.observableArrayList(
                "clear_document.png",
                "rotated_document.png",
                "low_resolution_document.jpg",
                "low_lighting_document.jpg",
                "ocr_fail_blank.png"));

        // Default name to verify
        expectedNameField.setText("Sunav Sharma");

        // Cell factory for list view to display lines nicely
        linesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            redrawCanvas();
        });
    }

    @FXML
    public void handleCheckConnection() {
        connectionStatusLabel.setText("Checking...");
        connectionStatusLabel.setStyle("-fx-text-fill: #94a3b8;");
        apiClient = new OcrApiClient(serverUrlField.getText().trim());

        apiClient.checkHealth().thenAccept(isHealthy -> Platform.runLater(() -> {
            if (isHealthy) {
                connectionStatusLabel.setText("● Connected");
                connectionStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                connectionStatusLabel.setText("● Disconnected");
                connectionStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
        }));
    }

    @FXML
    public void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Document for OCR");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp",
                        "*.tiff"));

        File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
        if (file != null) {
            loadFile(file);
        }
    }

    @FXML
    public void handleSampleSelected() {
        String selectedSample = sampleSelector.getSelectionModel().getSelectedItem();
        if (selectedSample == null || selectedSample.isEmpty()) {
            return;
        }

        // Check relative paths to test dataset
        File sampleFile = new File("../ocr-module/tests/data/" + selectedSample);
        if (!sampleFile.exists()) {
            sampleFile = new File("src/main/resources/test-samples/" + selectedSample);
        }

        if (sampleFile.exists()) {
            loadFile(sampleFile);
        } else {
            selectedFileLabel.setText("Sample not found: " + sampleFile.getPath());
        }
    }

    private void loadFile(File file) {
        currentSelectedFile = file;
        selectedFileLabel.setText(file.getName());
        fileSizeLabel.setText(String.format("Size: %.1f KB", file.length() / 1024.0));

        try (FileInputStream fis = new FileInputStream(file)) {
            currentLoadedImage = new Image(fis);
            imageDimsLabel.setText(String.format("Resolution: %.0f x %.0f px", currentLoadedImage.getWidth(),
                    currentLoadedImage.getHeight()));
            runOcrButton.setDisable(false);
            lastOcrResponse = null;
            clearResults();
            redrawCanvas();
        } catch (Exception e) {
            selectedFileLabel.setText("Failed to load: " + e.getMessage());
        }
    }

    @FXML
    public void handleRunOcr() {
        if (currentSelectedFile == null || !currentSelectedFile.exists()) {
            return;
        }

        runOcrButton.setDisable(true);
        runOcrButton.setText("⏳ Processing...");
        setStatusBadge(VerificationStatus.IDLE, "PROCESSING OCR...");

        apiClient.uploadDocumentAsync(currentSelectedFile).thenAccept(response -> Platform.runLater(() -> {
            runOcrButton.setDisable(false);
            runOcrButton.setText("⚡ Run OCR");
            lastOcrResponse = response;
            displayOcrResults(response);
            handleVerifyName();
        }));
    }

    private void displayOcrResults(OcrResponse response) {
        if (response == null || !response.isSuccess()) {
            String err = response != null && response.getError() != null ? response.getError() : "Unknown OCR error";
            fullTextArea.setText("Error during OCR extraction: " + err);
            linesListView.getItems().clear();
            setStatusBadge(VerificationStatus.OCR_FAILED_RESCAN, "OCR FAILED / RESCAN");
            verificationMessageLabel.setText("OCR Server Error: " + err);
            return;
        }

        // Populate lines
        linesListView.getItems().clear();
        for (TextLine line : response.getLines()) {
            linesListView.getItems().add(String.format("[%04.1f%%] %s", line.getConfidence() * 100, line.getText()));
        }

        // Full text
        fullTextArea.setText(response.getText());

        // Performance metadata
        engineLabel.setText(response.getEngine() != null ? response.getEngine() : "-");
        processingTimeLabel.setText(String.format("%.1f ms", response.getProcessingTimeMs()));
        lineCountLabel.setText(String.valueOf(response.getLines().size()));

        redrawCanvas();
    }

    @FXML
    public void handleVerifyName() {
        String expected = expectedNameField.getText();
        VerificationResult result = verificationService.verifyName(lastOcrResponse, expected);
        setStatusBadge(result.getStatus(), result.getStatus().getLabel());
        verificationMessageLabel.setText(result.getMessage());
    }

    @FXML
    public void handleNameKeyReleased() {
        if (lastOcrResponse != null) {
            handleVerifyName();
        }
    }

    @FXML
    public void handleToggleBoxes() {
        redrawCanvas();
    }

    private void setStatusBadge(VerificationStatus status, String text) {
        statusBadgeBox.getStyleClass().removeAll("badge-idle", "badge-verified", "badge-not-verified", "badge-failed");
        statusBadgeText.setText(text);

        switch (status) {
            case VERIFIED -> statusBadgeBox.getStyleClass().add("badge-verified");
            case NOT_VERIFIED -> statusBadgeBox.getStyleClass().add("badge-not-verified");
            case OCR_FAILED_RESCAN -> statusBadgeBox.getStyleClass().add("badge-failed");
            default -> statusBadgeBox.getStyleClass().add("badge-idle");
        }
    }

    private void clearResults() {
        linesListView.getItems().clear();
        fullTextArea.clear();
        engineLabel.setText("-");
        processingTimeLabel.setText("-");
        lineCountLabel.setText("-");
        setStatusBadge(VerificationStatus.IDLE, "READY FOR DOCUMENT");
        verificationMessageLabel.setText("Click 'Run OCR' to extract text and verify identity.");
    }

    private void redrawCanvas() {
        GraphicsContext gc = documentCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, documentCanvas.getWidth(), documentCanvas.getHeight());

        if (currentLoadedImage == null) {
            gc.setFill(Color.web("#1e293b"));
            gc.fillRect(0, 0, documentCanvas.getWidth(), documentCanvas.getHeight());
            gc.setFill(Color.web("#64748b"));
            gc.fillText("No document loaded", documentCanvas.getWidth() / 2 - 60, documentCanvas.getHeight() / 2);
            return;
        }

        // Draw image scaled to fit canvas maintaining aspect ratio
        double canvasW = documentCanvas.getWidth();
        double canvasH = documentCanvas.getHeight();
        double imgW = currentLoadedImage.getWidth();
        double imgH = currentLoadedImage.getHeight();

        double scale = Math.min(canvasW / imgW, canvasH / imgH);
        double drawW = imgW * scale;
        double drawH = imgH * scale;
        double offsetX = (canvasW - drawW) / 2.0;
        double offsetY = (canvasH - drawH) / 2.0;

        gc.drawImage(currentLoadedImage, offsetX, offsetY, drawW, drawH);

        // Draw bounding boxes if enabled
        if (showBoxesCheckbox.isSelected() && lastOcrResponse != null && lastOcrResponse.getLines() != null) {
            int selectedIdx = linesListView.getSelectionModel().getSelectedIndex();

            for (int i = 0; i < lastOcrResponse.getLines().size(); i++) {
                TextLine line = lastOcrResponse.getLines().get(i);
                List<Integer> bbox = line.getBbox();
                if (bbox == null || bbox.size() < 4)
                    continue;

                double x1 = offsetX + (bbox.get(0) * scale);
                double y1 = offsetY + (bbox.get(1) * scale);
                double x2 = offsetX + (bbox.get(2) * scale);
                double y2 = offsetY + (bbox.get(3) * scale);
                double boxW = x2 - x1;
                double boxH = y2 - y1;

                if (i == selectedIdx) {
                    // Highlight selected line
                    gc.setStroke(Color.web("#38bdf8"));
                    gc.setLineWidth(3);
                    gc.setFill(Color.web("#38bdf8", 0.3));
                    gc.fillRect(x1, y1, boxW, boxH);
                    gc.strokeRect(x1, y1, boxW, boxH);
                } else {
                    gc.setStroke(Color.web("#10b981"));
                    gc.setLineWidth(1.5);
                    gc.setFill(Color.web("#10b981", 0.15));
                    gc.fillRect(x1, y1, boxW, boxH);
                    gc.strokeRect(x1, y1, boxW, boxH);
                }
            }
        }
    }
}

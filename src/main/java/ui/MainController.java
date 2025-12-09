package ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import capture.CaptureService;
import service.AudioCaptureService;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private ImageView captureView;

    @FXML
    private ComboBox<String> deviceSelector;

    @FXML
    private StackPane captureContainer;

    private CaptureService captureService = new CaptureService();
    private AudioCaptureService audioService = new AudioCaptureService();

    @FXML
    private Button zoomInButton;

    @FXML
    private Button zoomOutButton;

    @FXML
    private Slider zoomSlider;

    // ズームの状態
    private double currentScale = 1.0;
    private final double MIN_SCALE = 0.2;
    private final double MAX_SCALE = 3.0;
    private final double STEP = 0.1;
    private Stage stage;

    public void initialize() {
        // デバイス検出を別スレッドで実行してUIをブロックしない
        new Thread(() -> {
            try {
                var deviceList = captureService.getDeviceList();
                
                Platform.runLater(() -> {
                    // Debug: list available audio devices
                    try {
                        var audioDevices = audioService.getAudioDeviceList();
                        System.out.println("Audio devices: " + audioDevices);
                    } catch (Exception ignored) {}
                    // 🔹① デバイス一覧を ComboBox に反映
                    if (deviceList != null && !deviceList.isEmpty()) {
                        deviceSelector.getItems().addAll(deviceList);
                        deviceSelector.getSelectionModel().select(0);
                        
                        // 初期起動（選択されたデバイスで開始）
                        int deviceIndex = deviceSelector.getSelectionModel().getSelectedIndex();
                        startCapture(deviceIndex);
                    } else {
                        // デバイスがない場合のメッセージ
                        deviceSelector.setPromptText("カメラが見つかりません");
                        System.out.println("利用可能なカメラがありません");
                    }

                    // 🔹④ コンボボックス変更時にカメラ切替
                    deviceSelector.setOnAction(e -> {
                        int index = deviceSelector.getSelectionModel().getSelectedIndex();
                        if (index >= 0) {
                            startCapture(index);
                        }
                    });

                    // --- ズームコントロール初期化 ---
                    setupZoomControls();
                });
            } catch (Exception e) {
                System.err.println("デバイス検出エラー: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Called by launcher to provide Stage so controller can observe size/fullscreen changes.
     */
    public void setStage(Stage stage) {
        this.stage = stage;

        // Bind ImageView fit size to container so it resizes with window
        if (captureView != null && captureContainer != null) {
            captureView.fitWidthProperty().bind(captureContainer.widthProperty());
            captureView.fitHeightProperty().bind(captureContainer.heightProperty());
        }

        // Listen for changes to show debug info or perform additional layout updates
        stage.widthProperty().addListener((obs, oldV, newV) -> {
            // Could perform actions here when width changes
        });

        stage.heightProperty().addListener((obs, oldV, newV) -> {
            // Could perform actions here when height changes
        });

        stage.fullScreenProperty().addListener((obs, oldV, newV) -> {
            // Fullscreen toggled - UI will resize via bindings
        });
    }

    private void setupZoomControls() {
        // slider 初期値・範囲
        if (zoomSlider != null) {
            zoomSlider.setMin(MIN_SCALE);
            zoomSlider.setMax(MAX_SCALE);
            zoomSlider.setValue(currentScale);

            zoomSlider.valueProperty().addListener((obs, oldV, newV) -> {
                applyScale(newV.doubleValue());
            });
        }

        if (zoomInButton != null) {
            zoomInButton.setOnAction(e -> {
                double next = clamp(currentScale + STEP, MIN_SCALE, MAX_SCALE);
                if (zoomSlider != null) zoomSlider.setValue(next);
                applyScale(next);
            });
        }

        if (zoomOutButton != null) {
            zoomOutButton.setOnAction(e -> {
                double next = clamp(currentScale - STEP, MIN_SCALE, MAX_SCALE);
                if (zoomSlider != null) zoomSlider.setValue(next);
                applyScale(next);
            });
        }

        // マウスホイールでズーム（Ctrl + スクロール）
        if (captureView != null) {
            captureView.setOnScroll(ev -> {
                if (ev.isControlDown()) {
                    double delta = ev.getDeltaY() > 0 ? STEP : -STEP;
                    double next = clamp(currentScale + delta, MIN_SCALE, MAX_SCALE);
                    if (zoomSlider != null) zoomSlider.setValue(next);
                    applyScale(next);
                    ev.consume();
                }
            });
        }
    }

    private void applyScale(double scale) {
        currentScale = scale;
        if (captureView != null) {
            captureView.setScaleX(currentScale);
            captureView.setScaleY(currentScale);
        }
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void startCapture(int index) {
        captureService.stopCapture();
        audioService.stopCapture();
        captureService.startCapture(index, image -> {
            captureView.setImage(image);
        });

        // Try to start audio capture from the same index (may need adjustment per environment)
        try {
            audioService.startCapture(index);
        } catch (Exception e) {
            System.err.println("Audio start error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        captureService.stopCapture();
        audioService.stopCapture();
    }
}

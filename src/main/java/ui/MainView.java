package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class MainView {

    private BorderPane root = new BorderPane();
    private ImageView captureView = new ImageView();
    private Label analysisLabel = new Label("解析結果を表示するエリア");
    private Label statusBar = new Label("ステータス: 準備完了");

    public MainView() {

        // メニュー
        MenuBar menuBar = new MenuBar(
                new Menu("ファイル"),
                new Menu("設定"),
                new Menu("ヘルプ")
        );
        root.setTop(menuBar);

        // 左側（キャプチャ）
        captureView.setFitWidth(1280);
        captureView.setPreserveRatio(true);

        VBox leftPane = new VBox(captureView);
        leftPane.setPadding(new Insets(10));
        leftPane.setStyle("-fx-border-color: gray");
        root.setLeft(leftPane);

        // 右側（解析）
        VBox rightPane = new VBox(analysisLabel, new Button("画像切り替え"));
        rightPane.setPrefWidth(300);
        root.setRight(rightPane);

        // ステータスバー
        statusBar.setStyle("-fx-background-color: #EEE; -fx-padding: 5;");
        root.setBottom(statusBar);
    }

    public BorderPane getRoot() {
        return root;
    }

    public ImageView getCaptureView() {
        return captureView;
    }

    public Label getStatusBar() {
        return statusBar;
    }
}

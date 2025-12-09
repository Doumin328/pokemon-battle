

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import service.SampleCaptureTask;

public class SampleApp extends Application {

    @Override
    public void start(Stage stage) {
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(1280);

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 1280, 720);

        stage.setTitle("JavaCV Capture Test");
        stage.setScene(scene);
        stage.show();

        // JavaCV キャプチャ開始
        new Thread(new SampleCaptureTask(imageView)).start();
    }

    public static void main(String[] args) {
        launch();
    }
}

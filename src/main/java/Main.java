import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // FXMLファイルを読み込む
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/MainView.fxml"));
        BorderPane root = loader.load();
        
        // シーン作成
        Scene scene = new Scene(root, 1920, 1080);
        
        // ステージ設定
        primaryStage.setTitle("Pokemon Battle Project");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // JavaFXアプリ起動
    }
}

package landing;


import data.Data;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/landing/landing.fxml"));
        primaryStage.setMaximized(true);

        primaryStage.setTitle("Chintu");
        Scene mScene = new Scene(root);
        primaryStage.setScene(mScene);
        primaryStage.show();
        mScene.getStylesheets().add(getClass().getResource("/landing/style.css").toExternalForm());
    }


    @Override
    public void stop() throws Exception {
        Data.getInstance().stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

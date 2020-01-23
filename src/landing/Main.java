package landing;


import arduino.Communication;
import data.Data;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/landing/landing.fxml"));
        primaryStage.setMaximized(true);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Chintu");
        Scene mScene = new Scene(root);
        primaryStage.setScene(mScene);
        primaryStage.show();
        mScene.getStylesheets().add(getClass().getResource("/landing/style.css").toExternalForm());

        mScene.setOnKeyPressed(keyEvent -> {
            boolean isManual = !Data.getInstance().isAuto.getValue();

            switch (keyEvent.getCode()) {
                case W:
                    if (isManual) {
                        Data.getInstance().statusSubject.onNext(Data.FORWARD);
                    }
                    break;
                case S:
                    if (isManual) {
                        Data.getInstance().statusSubject.onNext(Data.BACKWARD);

                    }
                    break;
                case A:
                    if (isManual) {
                        Data.getInstance().statusSubject.onNext(Data.LEFT);

                    }
                    break;
                case D:
                    if (isManual) {
                        Data.getInstance().statusSubject.onNext(Data.RIGHT);

                    }
                    break;
            }
        });
        mScene.setOnKeyReleased(keyEvent -> {
            boolean isAuto = Data.getInstance().isAuto.getValue();
            switch (keyEvent.getCode()) {
                case W:
                case S:
                case A:
                case D:
                    if (!isAuto) {
                        Data.getInstance().statusSubject.onNext(Data.MANUAL);
                    }
                    break;
                case M:
                    if (isAuto) {

                        Data.getInstance().statusSubject.onNext(Data.MANUAL);
                        Data.getInstance().isAuto.onNext(false);
                    } else {
                        Data.getInstance().isAuto.onNext(true);

                        Data.getInstance().statusSubject.onNext(Data.MANUAL);
                    }

                    break;
            }
        });
    }


    @Override
    public void stop() throws Exception {
        Data.getInstance().stop();
        Communication.getInstance().OnDestroy();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

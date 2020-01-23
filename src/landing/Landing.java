package landing;


import arduino.Communication;
import com.fazecast.jSerialComm.SerialPort;
import data.Data;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;


public class Landing{
    private Data db = Data.getInstance();

    @FXML private Pane overLay;

    @FXML private Pane JISLogo;

    @FXML private Label flameValue;
    @FXML private Label connectionFail;

    @FXML private StackedAreaChart<?, ?> graphValueGas;

    @FXML
    private StackedAreaChart<?, ?> graphValueFlame;
    @FXML
    private StackedAreaChart<?, ?> graphValueHum;
    @FXML
    private StackedAreaChart<?, ?> graphValueTemp;

    @FXML
    private Label gasValue;
    @FXML
    private Label curStatus;

    @FXML
    private ToggleButton lightsButton;

    @FXML
    private Label tempValue;

    @FXML
    private ToggleButton carButton;

    @FXML
    private Label humilityValue;

    @FXML VBox portList;

    @FXML
    Button portRefresh;

    @FXML
    private AnchorPane parent;
    private Disposable statusSubscription;

    @FXML
    protected void initialize() {
        overLay.setVisible(true);
        connectionFail.setVisible(false);
        connectionFail.setMinHeight(0.0d);

        portRefresh.setOnAction((e)->{
            portList.getChildren().remove(0, portList.getChildren().size());
            Communication.getInstance().updatePortList();
            setPortList();
        });
        // TODO: unSubscribe
        statusSubscription = Data.getInstance().statusSubject.subscribe((s) -> curStatus.setText(s));

        setPortList();

    }

    private void setPortList(){


        for(SerialPort port: db.getPorts()){
            Button btn = new Button(port.getDescriptivePortName());
            btn.setOnMouseClicked((MouseEvent mouseEvent) -> {
                if(Communication.getInstance().connect( btn.getText())){
                    overLay.setVisible(false);
                    initializeViews();
                }else {
                    connectionFail.setVisible(true);
                    connectionFail.setMinHeight(60);
                }
            });
            portList.getChildren().add(btn);
        }
    }

    private void initializeViews() {


        chart();
        try {
            Image image = new Image("/jisulogo.png");
            ImageView imageView = new ImageView(image);
            JISLogo.getChildren().add(imageView);

        } catch (Exception e) {
            e.printStackTrace();
        }


        lightsButton.setOnAction(event -> {
            if(lightsButton.isSelected()){
                lightsButton.setText("ON");
                Communication.getInstance().send(Communication.MsgCode.LIGHT_ON);
            }else {
                lightsButton.setText("OFF");
                Communication.getInstance().send(Communication.MsgCode.LIGHT_OFF);

            }
        });

        carButton.setOnAction(event -> {
            if(carButton.isSelected()){
                carButton.setText("ON");
                Communication.getInstance().send(Communication.MsgCode.CAR_ON);

            }else {
                carButton.setText("OFF");
                Communication.getInstance().send(Communication.MsgCode.CAR_OFF);

            }
        });
        Communication.NanoData nanoData = db.getNanoData();

        nanoData.Temperature.subscribe(d-> Platform.runLater(()-> tempValue.setText(    d + "\u00B0C")));
        nanoData.Humidity.subscribe(   d-> Platform.runLater(()-> humilityValue.setText(d + "%")));
        nanoData.Gas_value.subscribe(  d-> Platform.runLater(()-> {
            if(d> db.gasthres) {
                gasValue.setStyle("-fx-background-color: #dd6666; -fx-padding: 5pt;");
                gasValue.setText("Found");

            }else {
                gasValue.setStyle("-fx-background-color: transparent;");
                gasValue.setText("Not found");

            }

        }));
        nanoData.Flame_value.subscribe(d-> Platform.runLater(()->{
            if(  d < db.flamethres ){
                flameValue.setStyle("-fx-background-color: #dd6666; -fx-padding: 5pt;");
                flameValue.setText("Found" );
            }else {
                flameValue.setStyle("-fx-background-color: transparent;");

                flameValue.setText( "Not found");
            }
        }));

    }

    private void chart(){

        Communication.NanoData nanoData = db.getNanoData();

        setGraph(graphValueHum, nanoData.Humidity);
        setGraph(graphValueFlame, nanoData.Flame_value);
        setGraph(graphValueGas, nanoData.Gas_value);
        setGraph(graphValueTemp, nanoData.Temperature);

    }

    private void setGraph(StackedAreaChart<?, ?> graph, Observable<Double> value) {

        XYChart.Series series = new XYChart.Series();
        graph.setLegendVisible(false);
        graph.getXAxis().setLabel("Time");
        graph.getYAxis().setLabel("Value");
        graph.setAnimated(false);
        value.subscribe(d-> {
            Platform.runLater(()-> {
                if (series.getData().size() >= 25)
                    series.getData().remove(series.getData().size() - 25);
                //noinspection unchecked
                series.getData().add(new XYChart.Data(db.getTime(), d));
            });
        });
        //noinspection unchecked
        graph.getData().add(series);
    }
}
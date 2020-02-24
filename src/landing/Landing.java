package landing;


import arduino.Communication;
import com.fazecast.jSerialComm.SerialPort;
import data.Data;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.*;
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
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;


public class Landing{
    private Data db = Data.getInstance();

    @FXML private Pane overLay;

    @FXML private Pane JISLogo;

    @FXML private Label flameValue;
    @FXML private AnchorPane draw3d;
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



        renderCarDrawing();


        portRefresh.setOnAction((e)->{
            portList.getChildren().remove(0, portList.getChildren().size());
            Communication.getInstance().updatePortList();
            setPortList();
        });
        // TODO: unSubscribe
        statusSubscription = Data.getInstance().statusSubject.subscribe((s) -> curStatus.setText(s));


        setPortList();

    }

    private void renderCarDrawing() {
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setSpecularColor(Color.ORANGE);
        redMaterial.setDiffuseColor(Color.RED);

        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.BLUE);
        blueMaterial.setSpecularColor(Color.LIGHTBLUE);

        final PhongMaterial greyMaterial = new PhongMaterial();
        greyMaterial.setDiffuseColor(Color.DARKGREY);
        greyMaterial.setSpecularColor(Color.GREY);

        final Box red = new Box(200, 200, 20);
        red.setMaterial(redMaterial);

        final Sphere blue = new Sphere(200);
        blue.setMaterial(blueMaterial);

        final Cylinder grey = new Cylinder(5, 100);
        grey.setMaterial(greyMaterial);

        PerspectiveCamera camera = new PerspectiveCamera();
        Group root = new Group();

        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(50);
        light.setTranslateY(-300);
        light.setTranslateZ(-400);
        PointLight light2 = new PointLight(Color.color(0.6, 0.3, 0.4));
        light2.setTranslateX(400);
        light2.setTranslateY(0);
        light2.setTranslateZ(-400);

        AmbientLight ambientLight = new AmbientLight(Color.color(0.2, 0.2, 0.2));
//        red.setRotationAxis(new Point3D(20, 11, 5).normalize());
        red.setTranslateX(180);
        red.setTranslateY(180);
        root.getChildren().addAll(ambientLight, light, light2, red);

        SubScene subScene = new SubScene(root, draw3d.getWidth(), draw3d.getHeight(), true, SceneAntialiasing.BALANCED);

        draw3d.layoutBoundsProperty().addListener(change -> {
            subScene.setWidth(draw3d.getWidth());
            subScene.setHeight(draw3d.getHeight());
            red.setTranslateX(draw3d.getWidth() / 2);
            red.setTranslateY(draw3d.getHeight() / 2);
            subScene.setFill(Color.TRANSPARENT);
            subScene.setCamera(camera);
        });
        Rotate xRotate;
        Rotate yRotate;
        Rotate zRotate;
        final DoubleProperty angleX = new SimpleDoubleProperty(0);
        final DoubleProperty angleZ = new SimpleDoubleProperty(0);
        final DoubleProperty angleY = new SimpleDoubleProperty(0);

//                red.setRotationAxis(new Point3D(x  % Math.PI * 2, y % Math.PI * 2, z % Math.PI * 2).normalize());

        red.setTranslateY(180);



        red.getTransforms().addAll(
                xRotate = new Rotate(0, Rotate.X_AXIS),
                yRotate = new Rotate(0, Rotate.Y_AXIS),
                zRotate = new Rotate(0, Rotate.Z_AXIS)
        );
        xRotate.angleProperty().bind(angleX);
        yRotate.angleProperty().bind(angleY);
        zRotate.angleProperty().bind(angleZ);
        draw3d.getChildren().add(subScene);
        //  // Rotate the object
        //  rotateX(radians(-pitch));
        //  rotateZ(radians(roll));
        //  rotateY(radians(yaw));

        Data.getInstance().rotateX.subscribe(d -> {angleX.setValue(-d);
            System.out.printf("pitch "+d);

        });
        Data.getInstance().rotateY.subscribe(d -> {angleY.setValue(d);
            System.out.println("yaw "+d);

        });
        Data.getInstance().rotateZ.subscribe(d -> {angleZ.setValue(d);

            System.out.println("roll "+d);
        });
//        draw3d.setClip(new Rectangle(draw3d.getWidth(), draw3d.getHeight()));
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
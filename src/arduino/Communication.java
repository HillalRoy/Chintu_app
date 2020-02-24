package arduino;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import data.Data;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

import javax.management.timer.Timer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


public class Communication {
    private SerialPort mPort;
    private SerialPort[] ports;
    private boolean connected = false;
    private static Communication instance = null;
    private ScheduledExecutorService mExecutorService;
    private ScheduledFuture<?> mReadDataTimer;

    public static Communication getInstance() {
        if (instance == null)
            instance = new Communication();
        return instance;
    }

    private Communication() {
        ports = SerialPort.getCommPorts();
        mExecutorService = Executors.newScheduledThreadPool(1);
//        mReadDataTimer = mExecutorService.scheduleAtFixedRate(() -> {
//            if (connected)
//                send(MsgCode.READ_DATA);
//        }, 1000, 2000, TimeUnit.MILLISECONDS);

    }

    public void updatePortList() {
        ports = SerialPort.getCommPorts();
    }

    public void send(String text) {
        System.out.println(text);
        if (!connected) return;
        new Thread(() -> {
            mPort.writeBytes(text.getBytes(StandardCharsets.UTF_8), text.getBytes().length);
        })
                .start();
    }

    public void send(char chr) {
        send(chr + "");
    }


    private boolean connect(SerialPort port) {

        connected = port.openPort();
        mPort = port;
        mPort.addDataListener(new DataListener(mPort));

        return connected;
    }

    public void stop() {
        if (mPort != null) {
            mPort.clearBreak();
            mPort.closePort();
        }
        ports = null;
        instance = null;
    }

    public static class DataListener implements SerialPortDataListener {
        private NanoData nanoData = new NanoData();
        SerialPort mPort;
        String line = "";

        public DataListener(SerialPort port) {
            mPort = port;
            Data.getInstance().setNanoData(nanoData);
        }

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                return;
            byte[] newData = new byte[mPort.bytesAvailable()];
            mPort.readBytes(newData, newData.length);
            String ln = new String(newData);

            line += ln;
            int index = line.lastIndexOf("--end--");
            if (index != -1) {
                line = line.replaceAll("\\r", "");
                String str = line.replaceAll("(--start--)*(--end--)*", "");
                str = str.replaceAll("\\n", "");
                parseCMD(str);
                line = line.substring(index, line.length() - 1);
                line = line.replaceAll("--end--", "");
            }

        }


        private long currentTime = new Date().getTime();
        double gyroAngleX = 0;
        double gyroAngleY = 0;
        double yaw = 0;

        private void setActValue(double accX, double accY, double accZ, double gyroX, double gyroY, double gyroZ) {
            accX = accX / 16384.0;
            accY = accY / 16384.0;
            accZ = accZ / 16384.0;
            double accAngleX = (Math.atan(accY / Math.sqrt(Math.pow(accX, 2) + Math.pow(accZ, 2))) * 180 / Math.PI) - 0.84; // AccX Error -0.84
            double accAngleY = (Math.atan(-1 * accX / Math.sqrt(Math.pow(accY, 2) + Math.pow(accZ, 2))) * 180 / Math.PI) - 11.93;// AccY Error -11.93
            long previousTime = currentTime;
            currentTime = new Date().getTime();
            double elapsedTime = ((double) currentTime - (double) previousTime) / 1000d;
            gyroX = gyroX / 131.0;
            gyroY = gyroY / 131.0;
            gyroZ = gyroZ / 131.0;

            // TODO: REcalibrate error
            gyroX = gyroX - 03.52; //gyroX Error -3.52
            gyroY = gyroY + 0.43;  //gyroX Error 0.43
            gyroZ = gyroZ + 2.22;  //gyroX Error 2.22
            gyroAngleX = gyroAngleX + gyroX * elapsedTime;
            gyroAngleY = gyroAngleY + gyroY * elapsedTime;
            yaw = yaw + gyroZ * elapsedTime;
            double roll = 0.96 * gyroAngleX + 0.04 * accAngleX;
            double pitch = 0.96 * gyroAngleY + 0.04 * accAngleY;

            Data.getInstance().rotateX.onNext(pitch);
            Data.getInstance().rotateY.onNext(roll);
            Data.getInstance().rotateZ.onNext(yaw);
            System.out.println("updated");

        }

        double AcX = 0;
        double AcY = 0;
        double AcZ = 0;
        double GyX = 0;
        double GyY = 0;
        double GyZ = 0;


        private void parseCMD(String ln) {
            System.out.println("undefine -> " + ln);

            String[] values = ln.split(";");
            for (String val : values) {
                String[] split = val.split(":");
                if (split.length < 2) {
                    if (!split[0].isEmpty())
                        System.out.println("undefine -> " + split[0]);
                    continue;
                }

                switch (split[0].trim()) {
                    case "t":
                        nanoData.tempEmitter.onNext(Double.parseDouble(split[1]));
                        break;
                    case "h":
                        nanoData.humEmitter.onNext(Double.parseDouble(split[1]));
                        break;
                    case "g":
                        nanoData.gasEmitter.onNext(Double.parseDouble(split[1]));
                        break;
                    case "f":
                        nanoData.flameEmitter.onNext(Double.parseDouble(split[1]));
                        break;
                    case "ax":
                        AcX = Double.parseDouble(split[1]);
                        break;
                    case "ay":
                        AcY = Double.parseDouble(split[1]);
                        break;
                    case "az":
                        AcZ = Double.parseDouble(split[1]);
                        break;
                    case "gx":
                        GyX = Double.parseDouble(split[1]);
                        break;
                    case "gy":
                        GyY = Double.parseDouble(split[1]);
                        break;
                    case "gz":
                        GyZ = Double.parseDouble(split[1]);
                        setActValue(AcX, AcY, AcZ, GyX, GyY, GyX);
                        break;
                    default:
                        break;
                }
            }
            System.out.println("finish");
        }
    }

    public static class NanoData {
        ObservableEmitter<Double> tempEmitter = null;
        ObservableEmitter<Double> humEmitter = null;
        ObservableEmitter<Double> gasEmitter = null;
        ObservableEmitter<Double> flameEmitter = null;

        public Observable<Double> Temperature = Observable.create(observableEmitter -> tempEmitter = observableEmitter);
        public Observable<Double> Humidity = Observable.create(observableEmitter -> humEmitter = observableEmitter);
        public Observable<Double> Flame_value = Observable.create(observableEmitter -> flameEmitter = observableEmitter);
        public Observable<Double> Gas_value = Observable.create(observableEmitter -> gasEmitter = observableEmitter);

        NanoData() {
            Temperature = Temperature.publish().autoConnect();
            Humidity = Humidity.publish().autoConnect();
            Flame_value = Flame_value.publish().autoConnect();
            Gas_value = Gas_value.publish().autoConnect();
        }
    }


    public SerialPort[] getPorts() {
        return ports;
    }

    public boolean connect(String text) {
        for (SerialPort port : ports) {
            if (port.getDescriptivePortName().equals(text)) {
                if (connect(port)) {
                    Timer t = new Timer();

                    Data.getInstance().statusSubject.subscribe((status) -> {
                        switch (status) {
                            case Data.MANUAL:
                                send(MsgCode.MANUAL);
                                break;
                            case Data.BACKWARD:
                                send(MsgCode.BACKWARD);
                                break;
                            case Data.FORWARD:
                                send(MsgCode.FORWARD);
                                break;
                            case Data.LEFT:
                                send(MsgCode.LEFT);
                                break;
                            case Data.RIGHT:
                                send(MsgCode.RIGHT);
                                break;
                            case Data.AUTO:
                                send(MsgCode.AUTO);
                                break;
                        }
                    });

                    return true;
                }
            }
        }
        return false;
    }

    public void OnDestroy() {
//        mReadDataTimer.cancel(false);

//        mReadDataTimer.cancel(true);
        mReadDataTimer = null;
        mExecutorService.shutdownNow();
        mExecutorService = null;
        instance = null;
        mPort = null;
        ports = null;
    }


    public static class MsgCode {
        public static final char CAR_ON = 'A';
        public static final char LIGHT_ON = 'B';
        public static final char CAR_OFF = 'C';
        public static final char LIGHT_OFF = 'D';
        public static final char READ_DATA = 'E';
        public static final char FORWARD = 'F';
        public static final char BACKWARD = 'G';
        public static final char LEFT = 'H';
        public static final char RIGHT = 'I';
        public static final char AUTO = 'J';
        public static final char MANUAL = 'K';
    }
}

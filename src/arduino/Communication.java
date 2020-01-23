package arduino;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import data.Data;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

import javax.management.timer.Timer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


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
        mReadDataTimer = mExecutorService.scheduleAtFixedRate(() -> {
            if (connected)
                send(MsgCode.READ_DATA);
        }, 1000, 2000, TimeUnit.MILLISECONDS);

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
                ;
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
        mReadDataTimer.cancel(false);

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

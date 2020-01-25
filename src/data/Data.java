package data;

import arduino.Communication;
import com.fazecast.jSerialComm.SerialPort;
import io.reactivex.subjects.BehaviorSubject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Data {
    public static final String FORWARD = "FORWARD";
    public static final String BACKWARD = "BACKWARD";
    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";
    public static final String AUTO = "AUTO";
    public static final String MANUAL = "MANUAL";
    public static final String MANUAL_ON = "MANUAL ON";


    private static Data instance = null;
    private Communication.NanoData nanoData = null;
    private Communication nano = Communication.getInstance();
    public BehaviorSubject<Boolean> isAuto = BehaviorSubject.createDefault(true);

    public BehaviorSubject<String> statusSubject = BehaviorSubject.createDefault("AUTO");
    public final float gasthres = 700;
    public final float flamethres = 80;


    public SerialPort[] getPorts(){
        return nano.getPorts();
    };

    public void setNanoData(Communication.NanoData _nanoData) {
        nanoData = _nanoData;
    }

    public static Data getInstance() {
        if(instance == null) instance = new Data();
        return instance;
    }

    public Communication.NanoData getNanoData() {
        return nanoData;
    }

    public void stop() {
        statusSubject.onComplete();
        isAuto.onComplete();
        statusSubject = null;
        isAuto = null;
        instance = null;
        nanoData = null;
        nano.stop();
    }

    public String getTime(){
        Date date = new Date();
        String strDateFormat = "HH:mm:ss";
        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
        return dateFormat.format(date);
    }

}
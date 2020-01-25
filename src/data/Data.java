package data;

import arduino.Communication;
import com.fazecast.jSerialComm.SerialPort;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Data {
    private static Data instance = null;
    private Communication.NanoData nanoData = null;
    private Communication nano = Communication.getInstance();
    public final float gasthres = 700;
    public final float flamethres = 100;


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
package com.example.remotejoystick;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/* class that is connecting between the gui (arbitrary joystick, text box, buttons)
   and the FGModel(specific data [rudder, throttle ...] that should be obtain from the joystick). */
public class ViewModel {

    private FGModel model;
    private float aileron = 0;
    private float elevator = 0;
    private float throttle = 0;
    private float rudder = 0;
    private String IP = "";
    private String port = "";

    // event happens when error that related to the socket occurs, or when connect() called but ip/port is invalid.
    public ErrorEventHandler onError = null;

    public void setValues_from_joystick(float px, float py, float pa, float pb) {
        // (joystick_right, joystick_up, horizontal_seekBar_right, vertical_seekBar_up) == (px, py, pa, pb)
        // all px,py,pa,pb are values between 0 to 1,
        // 0 means "no" 1 mean "yes" , 1 == "right" in "joystick_right", for example

        // convert (px,py,pa) to (aileron,elevator,rudder) values between -1 to 1  by  f(x)=(x-0.5)*2
        // pb == throttle remains the same, as value between 0 to 1
        setValues((px-0.5f)*2, (py-0.5f)*2, (pa-0.5f)*2, pb);
    }

    // setters:
    public void setValues(float aileron, float elevator, float rudder, float throttle) {
        this.aileron = aileron;
        this.elevator = elevator;
        this.rudder = rudder;
        this.throttle = throttle;
        // update the model for the changes:
        model.updatePlaneData(aileron,elevator,rudder,throttle);
    }
    public void setIP(String newVal) {
        this.IP = newVal;
        // ( no need to update the model since it only used for the following connect method )
    }
    public void setPort(String newVal) {
        this.port = newVal;
        // ( no need to update the model since it only used for the following connect method )
    }

    // command the method to connect to this.ip,this.port tcp socket
    public void connect() {
        // make sure the given ip is valid format: [num].[num].[num].[num] where num is positive int between 0 to 255
        Pattern p = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
        Matcher m = p.matcher(this.IP);
        boolean possibleIp = m.matches();
        if (possibleIp) {
            String[] parts = this.IP.split("\\.");
            try {
                for (String part : parts)
                    possibleIp = possibleIp && Integer.parseInt(part) < 256;
            } catch (Exception e) {
                possibleIp = false;
            }
        }
        if (!possibleIp) {
            if (onError != null)
                onError.handle(this,
                        new ErrorEventArgs("Invalid IP4 address", null));
            return;
        }

        // make sure the given string port is valid positive integer for tcp port
        int int_port = -1;
        try {
            int_port = Integer.parseInt(this.port);
        } catch ( Exception e) {
        }
        // wikipedia: tcp ports within [1,65535]
        if (int_port <= 0 || int_port > 65535) {
            if (onError != null)
                onError.handle(this,
                                    new ErrorEventArgs("Tcp Port is int between 1 to 65535", null));
        } else {
            model.connect(this.IP, int_port);
        }
    }

    // constructor
    public ViewModel(FGModel model){
        this.model = model;
        final ViewModel self = this;
        // all errors of model go throw the view model: [works if this.onError will be set]
        model.onError = new ErrorEventHandler() {
            @Override
            public void handle(Object sender, ErrorEventArgs args) {
                if (self.onError != null)
                    self.onError.handle(sender, args);
            }
        };
    }

    // never used, but for good practice: getters:

    public float getAileron() { return aileron; }

    public float getElevator() { return elevator; }

    public float getThrottle() { return throttle; }

    public float getRudder() { return rudder; }

    public String getIP() { return IP; }

    public String getPort() { return port; }
}

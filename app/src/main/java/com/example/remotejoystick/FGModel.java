package com.example.remotejoystick;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// class that connect to flight gear tcp socket and send data to it.
// socket action are in different thread, using ExecutorService
public class FGModel {
    // event happens when error that related to the socket occurs.
    public ErrorEventHandler onError = null;
    private volatile PrintWriter telnet = null;
    private ExecutorService es = null;
    // last time updatePlaneData() was called
    private long lastTime = 0;
    // how much times updatePlaneData() was called within time-period (that is set within updatePlaneData)
    private int withinTime = 0;

    public FGModel() {
        this.es = Executors.newFixedThreadPool(1);
    }

    // open new socket and connect to the tcp-flight-gear-server
    public void connect(String ipv4, int port) {
        // close all prev sockets [PrintWrite.close() will close the inner io = socket it this case]
        if (telnet != null)
            telnet.close();
        // setting telnet as null will make updatePlaneData() AND tasks that are waiting within es to be skipped,
        // so it will let the new task below of opening new socket to get to be executed fast.
        telnet = null;

        final FGModel self = this;
        es.execute(() -> {
            try {
                Socket fg = new Socket();
                // check HERE that the fg server is reachable
                fg.connect(new InetSocketAddress(ipv4, port),2000);
                telnet = new PrintWriter(fg.getOutputStream(), true);
            } catch (Exception e) {
                if (self.onError != null)
                    self.onError.handle(this, new ErrorEventArgs("Connection Error", e));
            }
        });
    }

    private PrintWriter getTelnet() {return this.telnet;}

    public void updatePlaneData(float aileron, float elevator, float rudder, float throttle){
        final long timePeriod = 10;
        final int timesAllowedWithinPeriod = 2;
        // within 10 milliSeconds, allow addition of only new 2 task for ExecutorService to send tcp packets only
        long currTime = System.currentTimeMillis();
        if (currTime - lastTime < timePeriod) {
            withinTime++;
        } else {
            // reset counter if new period began
            withinTime = 0;
        }
        this.lastTime = currTime;
        if (withinTime > timesAllowedWithinPeriod) {
            return;
        }

        // telnet_for_now is final reference to PrintWriter, this.getTelnet() may be changed meanwhile
        final PrintWriter telnet_for_now = this.getTelnet();
        final FGModel self = this;
        if (telnet_for_now != null) {
            es.execute(() -> {
                    // send data only if telnet_for_now it is still the current telnet
                    // [telnet_for_now is for sure not null]
                    if (telnet_for_now != this.getTelnet()) {
                        return;
                    }

                    telnet_for_now.print("set /controls/flight/aileron " + aileron + "\r\n");
                    telnet_for_now.print("set /controls/flight/elevator " + elevator + "\r\n");
                    telnet_for_now.print("set /controls/flight/rudder " + rudder + "\r\n");
                    telnet_for_now.print("set /controls/engines/current-engine/throttle " + throttle + "\r\n");
                    telnet_for_now.flush();

                    if (telnet_for_now.checkError()) {
                        // manually close the unreachable socket, and it will ensure not repeating onError.handle()
                        telnet_for_now.close();
                        this.telnet = null;
                        if (self.onError != null)
                            self.onError.handle(this, new ErrorEventArgs("Disconnected", null));
                    }
            });
        }
    }
}

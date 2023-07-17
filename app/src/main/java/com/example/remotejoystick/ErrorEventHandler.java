package com.example.remotejoystick;

// interface of handler that can handle Error/Exception, not with try/catch,
// but treat it as event. (the thrown Throwable is caught by the event raiser and suppress/handled there)
public interface ErrorEventHandler {
    void handle(Object sender, ErrorEventArgs args);
}

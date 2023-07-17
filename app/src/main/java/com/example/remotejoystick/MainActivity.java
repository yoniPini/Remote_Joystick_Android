package com.example.remotejoystick;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

/*
Main (and only) activity of the app, connecting to Flight gear simulator,
and sending mobile-user commands for the movements.
using MVVM:
Loads view from activity_main.xml, [contains component of JoystickView.java according to joystick_view.xml]
 view-model from ViewModel,
 model from FGModel.java
 */
public class MainActivity extends AppCompatActivity {
    // direct access to the instance which responsible
    // to the inner component within this associative view of this activity
    private JoystickView joystickView=null;
    private ViewModel viewModel=null;

    // Override the behavior of creating this activity (once in a run)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load xml
        setContentView(R.layout.activity_main);
        // get the JoystickView instance within the loaded xml
        this.joystickView = (JoystickView) findViewById(R.id.joystickView);

        // create ViewModel and bind this view changes to set the model view properties
        this.viewModel = new ViewModel(new FGModel());
        // anonymous classes can refer local final variables
        final MainActivity self = this;
        // subscribe to viewModel onError event
        // (called when there is network error / invalid port/tcp[when viewModel.connect() is called])
        this.viewModel.onError = new ErrorEventHandler() {
            // handle: show the user info message in the bottom
            @Override
            public void handle(Object sender, ErrorEventArgs args) {
                Handler handler = new Handler(self.getMainLooper());
                handler.post(()-> {
                    Toast.makeText(self.getApplicationContext(), args.description, Toast.LENGTH_LONG).show();
                });
            }
        };

        // bind viewModel to joystick data
        this.joystickView.onChange=new JoystickView.JoystickEventHandler(){
            @Override
            public void handle(Object sender, JoystickView.JoystickEventArgs args) {
                viewModel.setValues_from_joystick(args.px, args.py, args.pa, args.pb);
            }
        };


        /* binding of the top part to viewModel:
          (ip_TextBox, port_TextBox, connect_Button, resetJoystick_Button) */

        // bind viewModel to ip_TextBox text
        ((TextView)findViewById(R.id.ip_TextBox)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {  }
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setIP(s.toString());
            }
        });
        // bind viewModel to port_TextBox text
        ((TextView)findViewById(R.id.port_TextBox)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {  }
            @Override
            public void afterTextChanged(Editable s) { viewModel.setPort(s.toString());  }
        });

        // set onClick event handle to click on connect_Button
        ((Button)findViewById(R.id.connect_Button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // viewModel.connect() will call viewModel.onError.handle()
                // if the port property of viewModel is invalid tcp port, or error in connection(timeout of 2 sec),
                // we already set onError as handler,
                // which notify the user about the problem in message in the bottom of the screen (Toast)
                viewModel.connect();
            }
        });

        // set onClick event handle to click on resetJoystick_Button
        ((Button)findViewById(R.id.resetJoystick_Button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joystickView.resetValues();
            }
        });
    }
}

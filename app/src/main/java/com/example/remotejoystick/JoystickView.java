package com.example.remotejoystick;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.*;

// class to be component of view, to be included within others views / Layouts in xml

// (joystick_right, joystick_up, horizontal_seekBar_right, vertical_seekBar_up) == (px, py, pa, pb)
// all px,py,pa,pb are values between 0 to 1,
// 0 means "no" 1 mean "yes" , 1 == "right" in "joystick_right", for example
// notice py, pb is 1 when it seems "up" to user,
// event though in programming,
// position 30 is lower position in the gui than position 0

public class JoystickView extends FrameLayout {
    
    // class to gather px,py,pa,pb
    public static class JoystickEventArgs {
        public final float px;
        public final float py;
        public final float pa;
        public final float pb;
        public JoystickEventArgs(float px, float py, float pa, float pb) {
            this.px = px;
            this.py = py;
            this.pa = pa;
            this.pb = pb;
        }
    }
    
    // interface of handler that can handle joystick-data values updates == (px, py, pa, pb)
    public static interface JoystickEventHandler {
        // all values between 0 to 1;
        // px,py is the joystick so it isn't accurate -> and probably won't be exactly 0 or 1
        // pa is horizontal bar, pb is vertical bar.
        void handle(Object sender, JoystickEventArgs args) ;
    }

    private float px=0.5f;
    private float py=0.5f;
    private float pa=0.5f;
    private float pb=0;
    
    // on change of px/py/pa/pb notify by updateObserver() below
    public JoystickEventHandler onChange = null;

    private void updateObserver(){
        if (onChange != null)
            onChange.handle(this, new JoystickEventArgs(this.px, this.py, this.pa, this.pb));
    }

    // init method to fill within this view (which extends FrameLayout) all the components from joystick_view.xml
    // should be called only from constructor.
    // param context should be the context that this view (JoystickView extends FrameLayout) is within.
    private void init(Context context) {

        inflate(context ,R.layout.joystick_view,this);

        // set listeners to seekbars, to update this.pa/this.pb on their change, and then updateObserver()
        final JoystickView self = this;

        ((SeekBar)findViewById(R.id.seekBar_value_a)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                self.pa = progress/100f;
                self.updateObserver();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ((SeekBar)findViewById(R.id.seekBar_value_b)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                self.pb = progress/100f;
                self.updateObserver();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    // constructors, will call in the end to this.init(context)
    public JoystickView(Context context) {
        this(context, null);
    }

    public JoystickView(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public JoystickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    // before using those fields, ensureRadiusAndMovementBorder() should be called
    private float joystick_border_padding_from_top = -1;
    private float joystick_border_padding_from_left = -1;
    private float joystick_radius = -1;
    private float joystick_movement_border = -1;

    // get the specific-mobile size of green_frame (joystick_movement area)
    // and init this.joystick_radius, this.joystick_movement_border, joystick_border_padding_from_top / left
    // should be called only after rendering view has finished
    private void ensureRadiusAndMovementBorder() {
        if (this.joystick_radius > 0)
            return;
        Rect r = new Rect();
        findViewById(R.id.green_frame).getDrawingRect(r);
        joystick_movement_border = r.right;
        joystick_radius =  r.right / 5.5f;
        // The green_frame is 200*200dpi and in the center(both left-right and top-bottom) of 300*300dpi joystick_frame
        // (which left-top of this JoystickView)
        // meaning, green_frame is 50dpi from top/left, following lines get dpi=density
        DisplayMetrics m = new DisplayMetrics();
        (findViewById(R.id.joystick_frame)).getDisplay().getRealMetrics(m);
        joystick_border_padding_from_top = 50 * m.density;
        joystick_border_padding_from_left = 50 * m.density;
    }

    // handle touch on screen = trying to moving the joystick
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (event == null ) {return true;}
        ensureRadiusAndMovementBorder();
        // get x,y within the joystick_movement_square[== the green frame layout which R.id.joystick_img is within it]
        float x = event.getX() - joystick_border_padding_from_left;
        float y = event.getY() - joystick_border_padding_from_top;

        // all joystick circle must stay within its movement square-area
        if (x < joystick_radius || y < joystick_radius ||
                     x > joystick_movement_border - joystick_radius || y > joystick_movement_border - joystick_radius) {
            return true;
        }
        // set new joystick position, where (x,y) is the center, and joystick_img is square of 2r*2r
        (findViewById(R.id.joystick_img)).layout((int) (x - joystick_radius), (int) (y - joystick_radius),
                                                             (int) (x + joystick_radius), (int) (y + joystick_radius));

        // consider the reachable area that the joystick can be within :
        // x:[radius, joystick_movement_border - joystick_radius]
        // if we shift "left" we obtain x:[0, joystick_movement_border - 2*joystick_radius]
        // and px is the progress from 0 to (joystick_movement_border - 2*joystick_radius)
        // same in y axis
        this.px = (x-joystick_radius)/(joystick_movement_border - 2*joystick_radius);
        this.py = (y-joystick_radius)/(joystick_movement_border - 2*joystick_radius);

         // the user see up direction, while in programming is the lower Y position value
        this.py = 1 - this.py;

        // notify about the changes
        this.updateObserver();
        return  true;
    }

    // reset VALUES of px, py, pa, pb AND their POSITIONS in the gui.
    public void resetValues() {
        ensureRadiusAndMovementBorder();
        // will automatically update and invoke (3 times) the updateObserver() - which will raise this.onChange.handel()

        // simulate moving the joystick

        // we want 0.5 == (x-joystick_radius)/(joystick_movement_border - 2*joystick_radius)
        // meaning  (joystick_movement_border - 2*joystick_radius) == 2*(x-joystick_radius)
        // therefore x = joystick_movement_border / 2
        // therefore x with considering padding == (joystick_movement_border / 2) + joystick_border_padding_from_left
        // same way: y with considering padding == (joystick_movement_border / 2) + joystick_border_padding_from_top
        float padded_x = (joystick_movement_border / 2) + joystick_border_padding_from_left;
        float padded_y = (joystick_movement_border / 2) + joystick_border_padding_from_top;

        long temp = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(temp, temp,
                MotionEvent.ACTION_DOWN, padded_x, padded_y, 0);
        this.dispatchTouchEvent(event);

        // simulate moving the seek bars
        ((SeekBar)(findViewById(R.id.seekBar_value_a))).setProgress(50);
        ((SeekBar)(findViewById(R.id.seekBar_value_b))).setProgress(0);
    }
}

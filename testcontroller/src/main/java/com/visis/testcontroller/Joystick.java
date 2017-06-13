package com.visis.testcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import static android.graphics.Color.RED;
import static android.graphics.Color.TRANSPARENT;

/**
 * Created by alexaad1 on 6/12/2017.
 * Tutorial: http://www.instructables.com/id/A-Simple-Android-UI-Joystick/
 */


public class Joystick extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener{
    private float centerX, centerY, baseR, thumbpadR;
    private Canvas myCanvas;
    private Paint colors;
    private final int RATIO = 5; //controls the shading
    private JoystickListener joystickCallback;

    public Joystick(Context context) {
        super(context);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
        {
            joystickCallback = (JoystickListener) context;
        }
    }

    public Joystick(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
        {
            joystickCallback = (JoystickListener) context;
        }
    }

    public Joystick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
        {
            joystickCallback = (JoystickListener) context;
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setupDimmensions();
        drawJoystick(centerX, centerY);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    void setupDimmensions(){
        centerX =  getWidth()/2;
        centerY = getHeight()/8;
        baseR = Math.min(getWidth(),getHeight())/3;
        thumbpadR =  baseR = Math.min(getWidth(),getHeight())/5;

    }

    private void drawJoystick(float newX, float newY){
        //specify the thumbpad location

        if(getHolder().getSurface().isValid()){
            //Draw the joystick
            myCanvas = this.getHolder().lockCanvas();
            colors = new Paint();
            myCanvas.drawColor(TRANSPARENT, PorterDuff.Mode.CLEAR);
            float hyp = (float) Math.sqrt(Math.pow(newX - centerX, 2) + Math.pow(newY-centerY, 2));
            float sin = (newY-centerY)/hyp;
            float cos = (newX-centerX)/hyp;

            colors.setARGB(255,45,82,125);
            myCanvas.drawCircle(centerX,centerY,baseR,colors);
            for(int i = 1; i <= (int)(baseR/RATIO); i++) {
                colors.setARGB(150 / i, 255, 0, 0); //Gradually decrease the shade of black drawn to create a nice shading effect

                myCanvas.drawCircle(newX - cos * hyp * (RATIO/baseR) * i,
                        newY - sin * hyp * (RATIO/baseR) * i, i * (thumbpadR * RATIO / baseR), colors); //Gradually increase the size of the shading effect

            }



        //Drawing the joystick hat

        for(int i = 0; i <= (int) (thumbpadR / RATIO); i++)

        {

            colors.setARGB(255, (int) (i * (255 * RATIO / thumbpadR)), (int) (i * (255 * RATIO / thumbpadR)), 255); //Change the joystick color for shading purposes

            myCanvas.drawCircle(newX, newY, thumbpadR - (float) i * (RATIO) / 2 , colors); //Draw the shading for the hat

        }

            getHolder().unlockCanvasAndPost(myCanvas);
        }


    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.equals(this)) {
            float displacement = (float) Math.sqrt((Math.pow(event.getX() - centerX, 2)) + Math.pow(event.getY() - centerY, 2));
            if (event.getAction() != event.ACTION_UP) {
                if(displacement < baseR)
                {
                    drawJoystick(event.getX(), event.getY());
                    joystickCallback.OnJoystickMoved(event.getX() - centerX,event.getY()-event.getY(), getId());

                }
                else
                {
                    float ratio = baseR / displacement;
                    float constrainedX = centerX + (event.getX() - centerX) * ratio;
                    float constrainedY = centerY + (event.getY() - centerY) * ratio;
                    drawJoystick(constrainedX, constrainedY);
                    joystickCallback.OnJoystickMoved((constrainedX-centerX)/baseR, (constrainedY-centerY)/baseR, getId());
                }
            }
            else
            {
                drawJoystick(centerX, centerY);
                joystickCallback.OnJoystickMoved(0, 0, getId()); //centered
            }
        }
        return true;
    }

    public interface JoystickListener{
        void OnJoystickMoved(float xPercent, float yPercent, int source);
    }
}

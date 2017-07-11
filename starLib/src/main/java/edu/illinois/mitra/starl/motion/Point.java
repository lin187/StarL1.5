package edu.illinois.mitra.starl.motion;

/*Using this instead of android.graphics.point and/or java.awt.point since the two are not
compatible when switching from JVM simulations to deploying the StarL app*/

public class Point {
    public int x;
    public int y;
    Point(){
        x = 0;
        y = 0;
    }
}

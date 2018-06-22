package edu.illinois.mitra.starl.objects;

/**
 * Created by Stirling Carter on 6/6/2017.
 *
 * This is a Polygon class that represents a Polygon of n points. Currently only a contains()
 * method is implemented for determining whether a given point lies within the polygon.
 *
 * We would have just used Polygon of java.awt, but this is not supported by Android. As such,
 * we just pulled the awt source code and slightly modified it for this bare bones class.
 *
 * Source:  ***http://grepcode.com/file_/repository.grepcode.com/java/root/
 *          jdk/openjdk/6-b14/java/awt/Polygon.java/?v=source***
 */

public class Polygon {

    int[] xpoints, ypoints;
    int size;

    public Polygon(int[] x, int[] y, int size){
        this.xpoints = x;
        this.ypoints = y;
        this.size = size;
    }

    public boolean contains(int x, int y) {
        if (size <= 2) {
            return false;
        }
        int hits = 0;

        int lastx = xpoints[size - 1];
        int lasty = ypoints[size - 1];
        int curx, cury;

        // Walk the edges of the polygon
        for (int i = 0; i < size; lastx = curx, lasty = cury, i++) {
            curx = xpoints[i];
            cury = ypoints[i];

            if (cury == lasty) {
                continue;
            }

            int leftx;
            if (curx < lastx) {
                if (x >= lastx) {
                    continue;
                }
                leftx = curx;
            } else {
                if (x >= curx) {
                    continue;
                }
                leftx = lastx;
            }

            double test1, test2;
            if (cury < lasty) {
                if (y < cury || y >= lasty) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - curx;
                test2 = y - cury;
            } else {
                if (y < lasty || y >= cury) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - lastx;
                test2 = y - lasty;
            }

            if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
                hits++;
            }
        }

        return ((hits & 1) != 0);
    }
}
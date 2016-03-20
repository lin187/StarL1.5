package edu.illinois.mitra.template;

import edu.illinois.mitra.starl.objects.Common;

/**
 * Created by VerivitalLab on 3/9/2016.
 */
public class BotInfoSelector {

    public String name;
    public String ip;
    public String bluetooth;
    public int type;

    public BotInfoSelector(String color, int type) {
        this.type = type;
        if(color.equals("red")) {
            name = "bot0";
            ip = "192.168.1.110";
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:75:BB:0E";
            }
            else if(type == Common.MINIDRONE) {
                bluetooth = "Mars_122139";
            }
        }

        if(color.equals("green")) {
            name = "bot1";
            ip = "192.168.1.111";
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:75:BB:2F";
            }
            else if(type == Common.MINIDRONE) {
                bluetooth = "green1";
            }
        }

        if(color.equals("blue")) {
            name = "bot2";
            ip = "192.168.1.112";
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:76:CE:B4";
            }
            else if(type == Common.MINIDRONE) {
                bluetooth = "Mars_122317";
            }
        }

        if(color.equals("white")) {
            name = "bot3";
            ip = "192.168.1.113";
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:76:C9:D4";
            }
            else if(type == Common.MINIDRONE) {
                //bluetooth = ""; There isn't a white drone set-up yet
            }
        }

    }
}

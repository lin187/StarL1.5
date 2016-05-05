package edu.illinois.mitra.template;

import edu.illinois.mitra.starl.interfaces.TrackedRobot;
import edu.illinois.mitra.starl.models.Model_iRobot;
import edu.illinois.mitra.starl.models.Model_quadcopter;
import edu.illinois.mitra.starl.objects.Common;

/**
 * Created by VerivitalLab on 3/9/2016.
 */
public class BotInfoSelector {

    public String name;
    public String ip;
    public String bluetooth;
    public TrackedRobot type;


    public BotInfoSelector(String color, int type, int deviceType) {
        if(color.equals("red")) {
            name = "bot0";
            if(deviceType == Common.NEXUS7) {
                ip = "192.168.1.110";
            }
            else if(deviceType == Common.MOTOE) {
                ip = "192.168.1.114";
            }
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:75:BB:0E";
                this.type = new Model_iRobot(name, 0,0);
            }
            else if(type == Common.MINIDRONE) {
                bluetooth = "Mars_122139";
                this.type = new Model_quadcopter(name, 0,0);
            }

        }

        if(color.equals("green")) {
            name = "bot1";
            if(deviceType == Common.NEXUS7) {
                ip = "192.168.1.111";
            }
            else if(deviceType == Common.MOTOE) {
                ip = "192.168.1.115";
            }
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:75:BB:2F";
                this.type = new Model_iRobot(name, 0,0);
            }
            else if(type == Common.MINIDRONE) {
                bluetooth = "green1";
                this.type = new Model_quadcopter(name, 0,0);
            }
        }

        if(color.equals("blue")) {
            name = "bot2";
            ip = "192.168.1.112";
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:76:CE:B4";
                this.type = new Model_iRobot(name, 0,0);
            }
            else if(type == Common.MINIDRONE) {
                bluetooth = "Mars_122317";
                this.type = new Model_quadcopter(name, 0,0);
            }
        }

        if(color.equals("white")) {
            name = "bot3";
            ip = "192.168.1.113";
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:76:C9:D4";
                this.type = new Model_iRobot(name, 0,0);
            }
            else if(type == Common.MINIDRONE) {
                //bluetooth = ""; There isn't a white drone set-up yet
            }
        }

    }
}

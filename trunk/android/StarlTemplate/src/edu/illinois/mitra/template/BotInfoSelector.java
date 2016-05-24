package edu.illinois.mitra.template;

import edu.illinois.mitra.starl.interfaces.TrackedRobot;
import edu.illinois.mitra.starl.models.Model_iRobot;
import edu.illinois.mitra.starl.models.Model_quadcopter;
import edu.illinois.mitra.starl.objects.Common;

/**
 * Created by VerivitalLab on 3/9/2016.
 * This class contains all info for specific hardware addresses
 * Each tablet/phone and robot is assigned a color
 * This files specifies the phone/tablets' IP addresses, robot's names and bluetooth address
 * All addresses and names are based on the color, as you can see in the if statements below
 */
public class BotInfoSelector {

    public String name;
    public String ip;
    public String bluetooth;
    public TrackedRobot type;


    public BotInfoSelector(String color, int type, int deviceType) {
        if(color.equals("red")) {
            name = "bot0"; // assign name: bot0 is always red
            if(deviceType == Common.NEXUS7) {
                ip = "192.168.1.110"; // reserved IP address of red Nexus7 tablet
            }
            else if(deviceType == Common.MOTOE) {
                ip = "192.168.1.114"; // reserved IP address of red MotoE phone
            }
            if(type == Common.IROBOT) {
                bluetooth = "5C:F3:70:75:BB:0E"; // bluetooth address of red raspberry pi on red irobot
                this.type = new Model_iRobot(name, 0,0);
            }
            else if(type == Common.MINIDRONE) {
                bluetooth = "Mars_122139"; // bluetooth address of red minidrone (use free flight 3 app from play store to get this address)
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

/**
 * Created by Mousa Almotairi on 4/28/2015.
 */

package edu.illinois.mitra.starl.gvh;

import java.util.ArrayList;

import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;


public class RobotGroup {

    Integer groupNum;
    public Double theta;
    ArrayList<String> botLists;
    public boolean setAfterBefore;
    public String AfterBot;
    public String BeforeBot;
    public int rank;
    public int rf;
    public boolean isLast;


    public RobotGroup(String id, Integer numOFgroup){

        Integer i = Integer.valueOf(id.substring(3)); // might should make this work with names other than bot0, bot1,... botn
        groupNum = i % numOFgroup;
        setAfterBefore= true;
        rank = 0;
        isLast= false;

        double calcuateAngle = groupNum*(360/Common.numOFgroups);
        if (Common.numOFgroups == 2 && groupNum ==1){
            theta = Double.valueOf(90);

        }
        else{
            theta = calcuateAngle;

        }
        // rf = 500* (groupNum+1);

        rf = 750;


        System.out.println("This robot is "+id+ " and it is assigned to group number "+ getGroupNum().toString()+" and ts theta is "+ theta.toString());
    }

    public Integer getGroupNum(){
        return groupNum;
    }
}

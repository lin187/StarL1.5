package edu.illinois.mitra.starl.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.DSM;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.AttrValue;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.DSMVariable;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * Created by cillashiki on 1/26/16.
 */
public class DSMPubSub implements DSM, MessageListener {
    private static final String TAG = "DSM_Multiple_Attr"; // TAG to use?
    private static final String SUBTAG = "WannaSub";
    private static final String UNSUBTAG = "UNSUB";
    private Map<String, DSMVariable> dsm_map;
    private GlobalVarHolder gvh;
    private String agent_name;
    public publisher MyPub;
    public subscriber MySub;
    public Broker MyBroker;
    public ItemPosition my_position; // to be specified by user defined
    public static final int ARRIVED_MSG = 22; //just to avoid the ERR msg by android studio. To be changed or removed.
    public enum Filter_type{ distance, message_hop, filterfunction } ;
    public Set<String> botset;
    public int predefineddistance = 500;
    public  Filter_type cur_filter = Filter_type.distance; //default type = distance


    public int totalmessagereceived;

    public DSMPubSub(GlobalVarHolder gvh){
        this.gvh = gvh;
        dsm_map = new HashMap<String, DSMVariable>();
        this.agent_name = gvh.id.getName();

        this.MyPub = new publisher(agent_name);
        this.MySub = new subscriber(agent_name);
        this.MyBroker = new Broker(agent_name);
        this.my_position = gvh.gps.getMyPosition();
        this.cur_filter = Filter_type.distance; //set distance to be the default type
        gvh.comms.addMsgListener(this, Common.MSG_DSM_INFORM);
        this.totalmessagereceived = 0;
    }

    public DSMPubSub(GlobalVarHolder gvh, Filter_type userdefined_type){
        this.gvh = gvh;
        dsm_map = new HashMap<String, DSMVariable>();
        this.agent_name = gvh.id.getName();

        this.MyPub = new publisher(agent_name);
        this.MySub = new subscriber(agent_name);
        this.MyBroker = new Broker(agent_name);
        this.my_position = gvh.gps.getMyPosition();
        this.cur_filter  = userdefined_type;
        my_position = this.gvh.gps.getMyPosition();

    }

    public void ModifyList(){
        if(this.cur_filter.equals("distance")) {
            PositionList allmaplist = this.gvh.gps.getPositions();
            botset = gvh.id.getParticipants();
            //iterate through the lists to see who I should sub to, calling filter function.
            for (String m : botset) {
                if (my_position.distanceTo(gvh.gps.getPosition(m)) <= predefineddistance) {
                    this.MyPub.addsub(m);
                    subscribe(m);
                }
                else{
                    if(this.MySub.my_publisher.contains(m)){
                        unsubscribe(m);
                    }
                }
            }
        }
       /* if(this.cur_filter.equals("message_hop")) {
        }*/
        return;
    }

    public void checkmap(){

       // if(this.cur_filter.equals("distance")) {
            PositionList allmaplist = this.gvh.gps.getPositions();
            botset = gvh.id.getParticipants();
            //iterate through the lists to see who I should sub to, calling filter function.
            for (String m : botset) {
                if (my_position.distanceTo(gvh.gps.getPosition(m)) <= predefineddistance) {
                    this.MyPub.addsub(m);
                    subscribe(m);
                }
                else{
                    if(this.MySub.my_publisher.contains(m)){
                        unsubscribe(m);
                    }
                }
            }
        //}
       /* if(this.cur_filter.equals("message_hop")) {
        }*/
        return;
    }


    public void updateVar(DSMVariable oldVar, DSMVariable newVar){
        if(oldVar == null || newVar == null){
            System.out.println("Could not update null DSM Variable");
            return;
        }
        for(String curKey : newVar.values.keySet()){
            if(oldVar.values.containsKey(curKey)){
                AttrValue temp = newVar.values.get(curKey);
                oldVar.values.get(curKey).updateValue(temp.s_value, temp.s_timeS);
            }
            else{
                AttrValue temp = newVar.values.get(curKey);
                oldVar.values.put(curKey, new AttrValue(temp.s_value, temp.s_timeS));
            }
        }
        return;
    }




    @Override
    public List<DSMVariable> getAll(String name, String owner) {//get neighbours.
        return new ArrayList(dsm_map.values());
        // return (List<DSMVariable>) this.dsm_map.values() ;
    }

    @Override
    public DSMVariable get_V(String name, String owner) {
        if(owner == "*"){
            if(dsm_map.containsKey(name)){
                return dsm_map.get(name);
            }
        }
        else{
            if(dsm_map.containsKey(name+owner)){
                return dsm_map.get(name+owner);
            }
            // TODO Auto-generated method stub
        }
        gvh.log.d(TAG, "Variable not found: "+ name + ", owner:" + owner);
        return null;
    }

    @Override
    public String get(String name, String owner) {
        if(owner == "*"){
            if(dsm_map.containsKey(name)){
                return dsm_map.get(name).values.get("default").s_value;
            }
        }
        else{
            if(dsm_map.containsKey(name+owner)){
                return dsm_map.get(name+owner).values.get("default").s_value;
            }
        }
        gvh.log.d(TAG, "Variable not found: "+ name + ", owner:" + owner);
        return null;
    }

    @Override
    public String get(String name, String owner, String attr) {
        if(owner == "*"){
            if(dsm_map.containsKey(name)){
                DSMVariable cur = dsm_map.get(name);
                if(cur.values.containsKey(attr)){
                    return cur.values.get(attr).s_value;
                }
            }
        }
        else{
            if(dsm_map.containsKey(name+owner)){
                DSMVariable cur = dsm_map.get(name+owner);
                if(cur.values.containsKey(attr)){
                    return cur.values.get(attr).s_value;
                }
            }
        }
        return null;
    }

    @Override
    public boolean put(DSMVariable input) {
        if(!input.owner.equals(agent_name) && !input.owner.equals("*")){
            return false;
        }                    //when put a new val, inform my subscribers
        else{
            DSMVariable curVar;
            if(dsm_map.containsKey(input.name)){
                curVar = dsm_map.get(input.name);
            }
            else{
                curVar = new DSMVariable(input.name, input.owner);
                dsm_map.put(input.name, curVar);
            }
            updateVar(curVar, input);
            informOthers(input);
            //System.out.print(this.MyPub.my_subscriber);
            return true;
        }
    }

    private void informOthers(DSMVariable input) {
        MessageContents temp = new MessageContents();
        temp.append(input.toStringList());
      //  System.out.print(MyPub.my_subscriber);
        for(String m:MyPub.my_subscriber) {
            RobotMessage inform = new RobotMessage(m, agent_name, Common.MSG_DSM_INFORM, temp);
            gvh.comms.addOutgoingMessage(inform);
        }
    }





    @Override
    public boolean putAll(List<DSMVariable> inputs) {
        boolean r_code = true;
        for(DSMVariable tuple : inputs){
            boolean temp = put(tuple);
            if(!temp){
                gvh.log.d(TAG, "Fail to put: "+ tuple.toString());
            }
            r_code = r_code && temp;
        }
        return r_code;
    }


    public long getConsistantTS(String owner){
        if(owner.equals(agent_name)){
            return gvh.time();
            // TODO: add local clock and clock de_sync simulation
        }
        else{
            // TODO : add clock sync to this time stamp
            return gvh.time();
        }
    }

    @Override
    public boolean put(String name, String owner, int value) {
        DSMVariable input = new DSMVariable(name, owner, String.valueOf(value), getConsistantTS(owner));
        return put(input);
    }

    @Override
    public boolean put(String name, String owner, String attr, int value) {
        long curTS = getConsistantTS(owner);
        DSMVariable input = new DSMVariable(name, owner, attr, String.valueOf(value), curTS);
        return put(input);
    }

    @Override
    public boolean put(String name, String owner, String... attr_and_value) {
        if(attr_and_value.length %2 != 0){
            return false;
        }
        long curTS = getConsistantTS(owner);
        DSMVariable input = new DSMVariable(name, owner, curTS, attr_and_value);
        return put(input);
    }

    @Override
    public boolean createMW(String name, int value) {
        if(get(name, "*") != null){
            return false;
        }
        DSMVariable input = new DSMVariable(name, "*", String.valueOf(value), -1);
        return put(input);
    }

    @Override
    public boolean createMW(String name, String... attr_and_value) {
        if(attr_and_value.length %2 != 0){
            return false;
        }
        long curTS = -1;
        // use a negative time stamp, if the MW variable already exists, no need to write the value again
        DSMVariable input = new DSMVariable(name, "*", curTS, attr_and_value);
        return put(input);
    }



    public String[] getMySubs(){
        return this.MyPub.my_subscriber.toArray(new String[this.MyPub.my_subscriber.size()]);
    };


    public void subscribe(String name){ //subscribe agent "name"
        MessageContents mymessage = new MessageContents(SUBTAG,agent_name) ;//wanna sub you ! add me to the list of your subscribers!
        RobotMessage inform = new RobotMessage(name, agent_name, ARRIVED_MSG, mymessage);
        gvh.comms.addOutgoingMessage(inform);
    };

    public void unsubscribe(String name){ //subscribe agent "name"
        MessageContents mymessage = new MessageContents(UNSUBTAG,agent_name) ;//wanna sub you ! add me to the list of your subscribers!
        RobotMessage inform = new RobotMessage(name, agent_name, ARRIVED_MSG, mymessage);
        gvh.comms.addOutgoingMessage(inform);
    };



    @Override
    public void cancel() {
        gvh.trace.traceEvent(TAG, "Cancelled", gvh.time());
        gvh.comms.removeMsgListener(Common.MSG_DSM_INFORM);
        // TODO Auto-generated method stub
    }

    @Override
    public void messageReceived(RobotMessage m) {
            String msgfrom = m.getFrom();
            if(m.getContents(0).equals(SUBTAG)){
                this.MyPub.my_subscriber.add(msgfrom);
                return;
            } //check if this is a sub message first
            //if not ,start to put into shared memory
            if(m.getContents(0).equals(UNSUBTAG)){
                if(this.MyPub.my_subscriber.contains(msgfrom)) {
                    this.MyPub.my_subscriber.remove(msgfrom);
                }
                return;
            }
            MessageContents temp = m.getContents();
            String var_name = temp.get(0);
            String owner = temp.get(1);
            long newtimestamp = Long.parseLong(temp.get(2));
            int num_of_attr = Integer.parseInt(temp.get(3));

            if(!dsm_map.containsKey(var_name)) {    //todo:change to my sub.hashmap
                     dsm_map.put(var_name, new DSMVariable(var_name, owner));
            }
            DSMVariable cur = dsm_map.get(var_name);
            for(int i = 0;i<num_of_attr; i++){
                String cur_name = temp.get(4+2*i);
                String cur_value =temp.get(5+2*i);
                //if the specific attr exists in the key
                if (cur.values.containsKey(cur_name)){
                    //get the current attr
                    AttrValue cur_attr = cur.values.get(cur_name);
                    //use the update method
                    cur_attr.updateValue(cur_value,newtimestamp);
                 }
                else	{//else create new attr with specific value
                    cur.values.put(cur_name, new AttrValue(cur_value, newtimestamp));
                    }
            }
    }







    public class subscriber{
        public String my_id;
        public List<String> my_publisher;
        public HashMap<String,Integer> sub_interest;


        subscriber(String id){
            this.my_id = id;
            this.my_publisher = new ArrayList<String>();
            this.sub_interest = new HashMap<String,Integer>();

        }




    }


    public class Broker{
        public String my_id;
        public List<String> my_subscriber;
        public List<String> my_publisher;
        public HashMap<String,Integer> sub_interest;
        //my subscriber, mash map,


        public void addpub(String NewPubName){
            if (!my_publisher.contains(NewPubName)){
                my_publisher.add(NewPubName);
            }
        }

        Broker(String id){
            this.my_id = id;
            this.my_publisher = new ArrayList<String>();
            this.my_subscriber = new ArrayList<String>();
            this.sub_interest = new HashMap<String,Integer>();
        }



        public Boolean FilterFunc(Filter_type param){

            return true;
        }


    }

    public class publisher {
        public String my_id;
        public List<String> my_subscriber;


        publisher(String id){
            this.my_id = id;
            this.my_subscriber = new ArrayList<String>();
        }

        public void addsub(String NewSubName){
            if (!my_subscriber.contains(NewSubName)){
                my_subscriber.add(NewSubName);
            }
        }


    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void reset() {

    }

}

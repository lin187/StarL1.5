package edu.illinois.mitra.demo.formation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.functions.DSMMultipleAttr;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.DSM;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.starl.objects.ItemPosition;

// the function will put all the robots in a place where distance between any two robots will be between 20 and 50.
public class FormationApp extends LogicThread {
	private DSM dsm;
	int robotIndex;
	private int [] destArray;

	final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
	final int distance_max = 1000;
	final int distance_min = 800;
	final int fluc = 100;
	ItemPosition currentDestination = new ItemPosition("cur", 0,0);
	public ItemPosition position;

	private enum Stage {
		PICK, GO, DONE
	};

	private Stage stage = Stage.PICK;

	public FormationApp(GlobalVarHolder gvh) {
		super(gvh);
		MotionParameters.Builder settings = new MotionParameters.Builder();
		settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
		MotionParameters param = settings.build();
		gvh.plat.moat.setParameters(param);
		for(ItemPosition i : gvh.gps.getWaypointPositions())
			destinations.put(i.getName(), i);

		dsm = new DSMMultipleAttr(gvh);		
		destArray = new int[6];
	}

	@Override
	public List<Object> callStarL() {
		int count = 0;

		while(true) {
			position = gvh.gps.getMyPosition();
			switch(stage) {		
			case PICK:
				//dsm.put("mypos", name, "x", String.valueOf(position.getX()), "y", String.valueOf(position.getY()), "z", String.valueOf(0));

				dsm.put("posX"+name, name, position.getX());
				dsm.put("posY"+name, name, position.getY());

				// save the position of others
				int counter = 0;
				for(String othername : gvh.id.getParticipants())
				{
					if(othername.equals(name)== false)
					{						
						String x1 = dsm.get("posX",othername);
						String y1 = dsm.get("posY",othername);
						System.out.println("x position: "+ x1);
						if (x1 != null && y1 != null )
						{
							int a = Integer.parseInt(x1);
							int b =Integer.parseInt(y1);
							if (a != 0 || b!=0)
							{
								destArray[2* counter] = a;
								destArray[2* counter+1] =b;
								counter++;
							}
						}

					}
				}
				System.out.println(counter);		
				if (counter == 0 )
				{

					int moveX = rand.nextInt(fluc) - fluc/2; 
					int moveY = rand.nextInt(fluc) - fluc/2; 
					currentDestination.x = moveX+ position.getX();
					currentDestination.y =moveY + position.getY();
				}
				else
				{
					int moveY,moveX;
					if (counter == 1)
					{
						if( Math.abs(position.getX() - destArray[0]) > Math.abs(position.getY() - destArray[1] ))  // check the difference of x and y
						{			
							if (position.getY() - destArray[1] < 0 )
							{
								moveY = destArray[1] - (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);

							}
							else
							{
								moveY = destArray[1] + (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
							}
							moveX = destArray[0] + rand.nextInt(fluc) - fluc/2;
						}
						else
						{
							if (position.getX() - destArray[0] < 0 )
							{
								moveX =  destArray[0] -  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
							}
							else
							{
								moveX =  destArray[0] +  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
							} 
							moveY = destArray[1] + rand.nextInt(fluc) -fluc/2;

						}


					}
					int x0,y0,x1,y1; // when we have the two other robots' positions

					x0 = destArray[0]; // by default
					y0 = destArray[1];
					x1 = destArray[2];
					y1 = destArray[3];

					int midX = (x0 + x1 )/2;       // calculate the midpoint and the inverse of the slope
					int midY = (y0 + y1 )/2;
					if (y0 != y1)
					{
						int slope = -(x0 - x1 )/(y0 - y1 );

						if( Math.abs(x0 - x1) < Math.abs(y0-y1))  // check the difference of x and y
						{
							if (position.getX() - x1 < 0 )
							{
								moveX =  midX -  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
							}
							else
							{
								moveX =  midX +  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
							} 
							moveY = (moveX - midX)*slope + midY + rand.nextInt(fluc) -fluc/2;
						}
						else
						{
							if (position.getY() - y1 < 0 )
							{
								moveY =  midY -  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
							}
							else
							{
								moveY =  midY +  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
							} 
							moveX = (moveY - midY)/slope + midX + rand.nextInt(50) -25;
						}
					}
					else
					{
						moveX = midX;
						if (position.getY() - y1 < 0 )
						{
							moveY =  midY -  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
						}
						else
						{
							moveY =  midY +  (int)((distance_max-distance_min-fluc)*rand.nextDouble() +distance_min);
						} 
					}

					currentDestination.x = moveX;
					currentDestination.y =moveY;

				} // else for counter != 0
				System.out.println(currentDestination.x+ ","+ currentDestination.y);

				gvh.plat.moat.goTo(new ItemPosition("temp"+ rand.nextInt(), currentDestination.x, currentDestination.y));
				count = 0;
				stage = Stage.GO;
				break;
			case GO:
				if(count>5 || !gvh.plat.moat.inMotion){
					dsm.put("posX"+name, name, position.getX());
					dsm.put("posY"+name, name, position.getY());				
					stage = Stage.PICK;
				}
				else{
					count ++;
				}
				break;
			case DONE:
				return null;
			}
			sleep(100);
		}
	}
	private static final Random rand = new Random();
}

package edu.illinois.mitra.starl.objects;

import java.util.Random;

import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.interfaces.TrackedRobot;

public class Model_iRobot extends ItemPosition implements TrackedRobot{

	public double angle;
	public int radius;
	public int type;
	public double velocity;
	
	public boolean leftbump;
	public boolean rightbump;
	public boolean circleSensor;
		
	public double vFwd = 0;
	public double vRad = 0;
	public Random rand;
	public int x_p;
	public int y_p;
	public double angle_p;
	
	public Model_iRobot(String received) throws ItemFormattingException{
		super(received);
	}
	
	public Model_iRobot(String name, int x, int y) {
		super(name, x, y);
		initial_helper();
	}
	
	public Model_iRobot(String name, int x, int y, double angle) {
		super(name, x, y);
		initial_helper();
		this.angle = angle;
	}
	
	public Model_iRobot(String name, int x, int y, double angle, int radius) {
		super(name, x, y);
		initial_helper();
		this.angle = angle;
		this.radius = radius;
	}
	
	public Model_iRobot(ItemPosition t_pos) {
		super(t_pos.name, t_pos.x, t_pos.y, t_pos.z);
		initial_helper();
		this.angle = 0;
		// TODO Auto-generated constructor stub
	}

	/** 
	 * 
	 * @return true if one robot is facing another robot/point
	 */
	public <T extends ItemPosition> boolean isFacing(T other) { 
		if(other == null) {
			return false;
		}
    	double angleT = Math.toDegrees(Math.atan2((other.y - this.y) , (other.x - this.x)));
    	if(angleT  == 90){
    		if(this.y < other.y)
    			angleT = angleT + 90;
    		double temp = this.angle % 360;
    		if(temp > 0)
    			return true;
    		else
    			return false;
    	}
		if(angleT < 0)
		{
			angleT += 360;
		}
		double angleT1, angleT2, angleself;
		angleT1 = (angleT - 90) % 360;
		if(angleT1 < 0)
		{
			angleT1 += 360;
		}
		angleT2 = (angleT + 90) % 360;
		if(angleT2 < 0)
		{
			angleT2 += 360;
		}
		angleself = this.angle % 360;
		if(angleself < 0)
		{
			angleself += 360;
		}
		if(angleT2 <= 180)
		{
			if((angleself < angleT1) && (angleself > angleT2))
				return false;
			else
				return true;
		}
		else
		{
			if(angleself > angleT2 || angleself < angleT1)
				return false;
			else
				return true;
				
		}
	}

	/** 
	 * @param other The ItemPosition to measure against
	 * @return Number of degrees this position must rotate to face position other
	 */
	public <T extends Point3d> int angleTo(T other) {
		if(other == null) {
			return 0;
		}
		
		int delta_x = other.x - this.x;
		int delta_y = other.y - this.y;
		double angle = this.angle;
		int otherAngle = (int) Math.toDegrees(Math.atan2(delta_y,delta_x));
		if(angle > 180) {
			angle -= 360;
		}
		int retAngle = Common.min_magitude((int)(otherAngle - angle),(int)(angle - otherAngle));
		
		if(retAngle > 180) {
			retAngle = retAngle-360;
		}
		if(retAngle <= -180) {
			retAngle = retAngle+360;
		}
		return  Math.round(retAngle);
	}
	
	public void setPos(int x, int y, int angle) {
		this.x = x;
		this.y = y;
		this.angle = angle;
	}
	
	public void setPos(Model_iRobot other) {
		this.x = other.x;
		this.y = other.y;
		this.angle = other.angle;
	}
	
	
	
	private void initial_helper(){
		angle = 0;
		velocity = 0;
		leftbump = false;
		rightbump = false;
		circleSensor = false;
		radius = 1;
		type = -1;
	}

	@Override
	public Point3d predict(double[] noises, double timeSinceUpdate) {
		if(noises.length != 3){
			System.out.println("Incorrect number of noises parameters passed in, please pass in x noise, y, noise and angle noise");
			return new Point3d(x,y);
		}
		double xNoise = (rand.nextDouble()*2*noises[0]) - noises[0];
		double yNoise = (rand.nextDouble()*2*noises[1]) - noises[1];
		double aNoise = (rand.nextDouble()*2*noises[2]) - noises[2];
		
		int dX = 0, dY = 0;
		double dA = 0;
		// Arcing motion
		dA = aNoise + (vRad*timeSinceUpdate);
		dX = (int) (xNoise + Math.cos(Math.toRadians(angle))*(vFwd*timeSinceUpdate));
		dY = (int) (yNoise + Math.sin(Math.toRadians(angle))*(vFwd*timeSinceUpdate));
		x_p = x+dX;
		y_p = y+dY;
		angle_p = angle+dA;
		return new Point3d(x_p,y_p);
	}

	@Override
	public void collision(Point3d collision_point) {
		if(angleTo(collision_point)%90>(-20)){
			rightbump = true;
		}
		if(angleTo(collision_point)%90<20){
			leftbump = true;
		}
		//TODO update local map
	}

	@Override
	public void updatePos(boolean followPredict, double timeSinceUpdate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean inMotion() {
		return (vFwd != 0 || vRad != 0);
	}

	@Override
	public void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions) {
		
		for(ItemPosition other : sensepoint_positions.getList()) {
			if(distanceTo(other)<600){
				if(!obspoint_positions.badPath(this, other)){
					circleSensor = true;
					return;
				}
			}
		}
		return;
	}

	@Override
	public void initialize() {
		rand = new Random();

	}
	
}

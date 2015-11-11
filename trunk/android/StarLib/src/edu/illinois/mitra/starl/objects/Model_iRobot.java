package edu.illinois.mitra.starl.objects;

import edu.illinois.mitra.starl.exceptions.ItemFormattingException;

public class Model_iRobot extends ItemPosition {

	public int angle;
	public int radius;
	public int type;
	public double velocity;
	
	public boolean leftbump;
	public boolean rightbump;
	public boolean circleSensor;
	
	
	public Model_iRobot(String received) throws ItemFormattingException{
		super(received);
	}
	
	public Model_iRobot(String name, int x, int y) {
		super(name, x, y);
		initial_helper();
	}
	
	public Model_iRobot(String name, int x, int y, int angle) {
		super(name, x, y);
		initial_helper();
		this.angle = angle;
	}
	
	public Model_iRobot(String name, int x, int y, int angle, int radius) {
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
	public <T extends ItemPosition> int angleTo(T other) {
		if(other == null) {
			return 0;
		}
		
		int delta_x = other.x - this.x;
		int delta_y = other.y - this.y;
		int angle = this.angle;
		int otherAngle = (int) Math.toDegrees(Math.atan2(delta_y,delta_x));
		if(angle > 180) {
			angle -= 360;
		}
		int retAngle = Common.min_magitude((otherAngle - angle),(angle - otherAngle));
		
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
	
}

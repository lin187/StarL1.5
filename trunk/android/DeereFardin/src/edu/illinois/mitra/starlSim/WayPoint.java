package edu.illinois.mitra.starlSim;
	class WayPoint
	{
		public WayPoint(int x, int y, int time)
		{
			this.x = x;
			this.y = y;
			this.time = time;
		}
		
		public WayPoint(){
			this.x = 0;
			this.y = 0;
			this.time = 0;
		}
		
		public String toString()
		{
			return "(" + x + ", " + y + ", t=" + time + ")";
		}
		
		public int x;
		public int y;
		public int time;

	
	
	@Override public boolean equals(Object other) {
		
	    boolean result = false;
	    if (other instanceof WayPoint) {
	        WayPoint that = (WayPoint) other;
	        result = (this.x == that.x && this.y == that.y);
	    }
	    
	    return result;
	}
	
	}
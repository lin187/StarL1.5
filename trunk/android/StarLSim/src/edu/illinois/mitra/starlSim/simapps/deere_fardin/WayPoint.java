package edu.illinois.mitra.starlSim.simapps.deere_fardin;
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
	}
package stan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class StanDrawer extends Drawer
{
	public void draw(LogicThread lt, Graphics2D g)
	{
		StanLogicThread instance = (StanLogicThread)lt;
		
		final int SIZE = 20; // for drawing the X
		
		if (!instance.isLeader)
		{
			if (instance.goingToX != Integer.MAX_VALUE)
			{
				g.setColor(Color.gray);
				g.setStroke(new BasicStroke(10));
				
				int x = instance.goingToX;
				int y = instance.goingToY;
				
				g.drawString(x + ", " + y, x + 2*SIZE, y);
				
				g.drawLine(x - SIZE, y - SIZE, x + SIZE, y + SIZE);
				g.drawLine(x - SIZE, y + SIZE, x + SIZE, y - SIZE);
			}
		}
		else
		{
			int sum = 0;
			for (int i : instance.longList)
				sum += i;
			
			if (sum != instance.lastSum)
			{
				instance.lastSum = sum;
				System.out.println("sum = " + sum);
			}
		}
	}
}

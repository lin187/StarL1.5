package edu.illinois.mitra.starlSim.draw;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.UIManager;

import edu.illinois.mitra.starlSim.main.SimSettings;


@SuppressWarnings("serial")
public class DrawFrame extends JFrame
{
	private DrawPanel dp = null;
	
	public DrawFrame(long startTime, int xsize, int ysize)
	{
		try 
		 {
			 // Set Native Look and Feel
		     UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
		 }
		 catch (Exception e) {}
		
		setTitle("StarL Simulator");
		setSize(1000,700);
		setLocation(50, 50);
		
		dp = new DrawPanel();
		dp.setWorld(xsize, ysize, startTime);
		dp.setDefaultPosition(-250, -250, 22);
		getContentPane().add(dp);
	}
	
	public void updateData(ArrayList <RobotData> data, long time)
	{
		dp.updateData(data, time);
	}
}

package edu.illinois.mitra.starlSim.draw;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.UIManager;


@SuppressWarnings("serial")
public class DrawFrame extends JFrame
{
	private DrawPanel dp = null;
	
	public DrawFrame()
	{
		try 
		 {
			 // Set Native Look and Feel
		     UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
		 }
		 catch (Exception e) {}
		
		setTitle("Line Mutex Visualizer");
		setSize(1000,700);
		setLocation(50, 50);
		
		dp = new DrawPanel();
		dp.setDefaultPosition(-250, -250, 22);
		getContentPane().add(dp);
	}
	
	public void updateData(ArrayList <RobotData> data)
	{
		dp.updateData(data);
	}
}

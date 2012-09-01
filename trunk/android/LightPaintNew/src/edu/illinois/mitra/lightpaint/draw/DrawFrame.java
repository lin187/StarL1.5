package edu.illinois.mitra.lightpaint.draw;

import javax.swing.JFrame;
import javax.swing.UIManager;

import edu.illinois.mitra.lightpaint.algorithm.LpAlgorithm;


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
		
		setTitle("Lightpainting visualizer");
		setSize(1000,700);
		setLocation(50, 50);
		
		dp = new DrawPanel();
		dp.setDefaultPosition(-250, -250, 22);
		getContentPane().add(dp);
	}
	
	public void updateData(LpAlgorithm alg)
	{
		dp.updateData(alg);
	}
}

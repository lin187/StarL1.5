package edu.illinois.linemutex;
import javax.swing.JFrame;
import javax.swing.UIManager;


/**
 * Line Mutex Visualizer
 * 3-5-2012
 * @author Stanley Bak (sbak2@illinois.edu)
 *
 */
public class Main
{	
	public static void main(String[] args)
	{	
		try 
		 {
			 // Set Native Look and Feel
		     UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
		 }
		 catch (Exception e) {}
		
		
		DrawFrame d = new DrawFrame();
		
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.setVisible(true);
	}
}

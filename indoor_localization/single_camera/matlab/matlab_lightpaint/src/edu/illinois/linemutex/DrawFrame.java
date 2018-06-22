package edu.illinois.linemutex;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


@SuppressWarnings("serial")
public class DrawFrame extends JFrame implements ActionListener, ChangeListener
{
	DrawPanel dp = null;
	JButton clear = new JButton("Reset");	
	JSpinner waypointSpacing;
	JSpinner robotRadius;
	JSpinner numRobots;
	JSpinner minTravelDistance;
	
	public DrawFrame()
	{
		getContentPane().setLayout(new BorderLayout());
		
		JPanel bottom = new JPanel();
		getContentPane().add(bottom, BorderLayout.PAGE_END);
		
		JPanel top = new JPanel();
		getContentPane().add(top, BorderLayout.PAGE_START);
		
		top.add(new JLabel("<html>Use the mouse to create segment endpoints (left = add, right = undo).<br /> " +
				"The red circle's diameter is twice the robot's radius (the collision distance).</html>"));
		
		bottom.setLayout(new FlowLayout());
		bottom.add(clear);
		
		clear.addActionListener(this);
		
		bottom.add(Box.createRigidArea(new Dimension(25,0)));
		bottom.add(new JLabel("Waypoint Spacing:"));
		
		waypointSpacing = new JSpinner(
		        new SpinnerNumberModel(70, //initial value
		                               1, //min
		                               9999, //max
		                               1));  //step
		
		bottom.add(waypointSpacing);

		bottom.add(Box.createRigidArea(new Dimension(25,0)));
		bottom.add(new JLabel("Robot Radius:"));
		
		robotRadius = new JSpinner(
		        new SpinnerNumberModel(20, //initial value
		                               1, //min
		                              9999, //max
		                               1));                //step
		
		bottom.add(robotRadius);
		
		bottom.add(Box.createRigidArea(new Dimension(25,0)));
		bottom.add(new JLabel("Num Robots:"));
		
		numRobots = new JSpinner(
		        new SpinnerNumberModel(4, //initial value
		                               1, //min
		                              99, //max
		                               1));                //step
		
		bottom.add(numRobots);
		
		bottom.add(Box.createRigidArea(new Dimension(25,0)));
		bottom.add(new JLabel("Min Travel Distance:"));
		
		minTravelDistance = new JSpinner(
		        new SpinnerNumberModel(200, //initial value
		                               1, //min
		                              999999, //max
		                               1));                //step
		
		bottom.add(minTravelDistance);
	
		
		setTitle("Line Mutex Visualizer");
		setSize(1100,600);
		setLocation(100, 100);
		
		dp = new DrawPanel(this);
		getContentPane().add(dp, BorderLayout.CENTER);
		
		minTravelDistance.addChangeListener(this);
		numRobots.addChangeListener(this);
		waypointSpacing.addChangeListener(this);
		robotRadius.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == clear)
			dp.clear();
	}

	public int getRobotRadius()
	{
		return (Integer)robotRadius.getValue();
	}

	public int getWaypointSpacing()
	{
		return (Integer)waypointSpacing.getValue();
	}

	@Override
	public void stateChanged(ChangeEvent arg0)
	{
		dp.compute();
	}

	public int getNumRobots()
	{
		return (Integer)numRobots.getValue();
	}

	public int getMinTravelDistance()
	{
		return (Integer)minTravelDistance.getValue();
	}
}

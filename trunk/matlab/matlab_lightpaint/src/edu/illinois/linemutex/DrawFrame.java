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
	DrawPanel dp = new DrawPanel(this);
	JButton clear = new JButton("Clear Segments");	
	JSpinner waypointSpacing;
	JSpinner robotRadius;
	
	public DrawFrame()
	{
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(dp, BorderLayout.CENTER);
		
		JPanel bottom = new JPanel();
		getContentPane().add(bottom, BorderLayout.PAGE_END);
		
		JPanel top = new JPanel();
		getContentPane().add(top, BorderLayout.PAGE_START);
		
		top.add(new JLabel("<html>Use the mouse to create segment endpoints (left = add, right = undo).<br .> " +
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
		waypointSpacing.addChangeListener(this);

		bottom.add(Box.createRigidArea(new Dimension(25,0)));
		bottom.add(new JLabel("Robot Radius:"));
		
		robotRadius = new JSpinner(
		        new SpinnerNumberModel(20, //initial value
		                               1, //min
		                              9999, //max
		                               1));                //step
		robotRadius.addChangeListener(this);
		bottom.add(robotRadius);
	
		
		setTitle("Line Mutex Visualizer");
		setSize(1024,768);
		setLocation(100, 100);
		
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
}

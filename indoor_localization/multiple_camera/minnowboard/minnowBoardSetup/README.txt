File Name: README.txt
Author: Nate Hamilton
Last Date Edited: 6/5/2017
Purpose: Instructions for setting up a MinnowBoard Turbot with the necessary
		 files for connecting to a Kinect.
Contact info: nathaniel.p.hamilton@vanderbilt.edu

================================================================================ 
	  Setting up your MinnowBoard Turbot for use with the Microsoft Kinect
================================================================================
1. Move the whole minnowBoardSetup folder to the home directory

2. Run the install scripts as root
	Open terminal and navigate to the minnowBoardSetup directory then run the command:
	
	sudo make all
	
3. Wait and watch the updates and installations occur. 
	The script should run without input except for one I could not remove.
	During the libfreenect2 installation you will see
	
	"8 of 16: installing OpenNI2...
	Ros packages for Ubuntu Trusty
	More info: https://launchpad.net/~deb-rob/+archive/ubuntu/ros-trusty
	Press [ENTER] to continue or ctrl-c to cancel adding it"
	
	Press enter and the installation will continue through to the end.
	
5. Verify the Kinect connects
	At the end of the installation process, a window should appear that displays
	4 Kinect images. This verifys that the installation process was completed 
	successfully. If the Kinect was not plugged in, this part will not work. 
	However, you can plug the Kinect in later and run the following command to 
	check:
		
		make play
	
	To exit the program, press the esc key.

================================================================================
				Other Commands
================================================================================	
For your convenience, if an error occurs at any point, I have included a way to  install only certain sections. 
For example, if an error occurs while installing libfreenect2, you can attempt the process again using the 
following command:

	make libfreenect2

There are commands for each section and they are as follows:

	make essential
		This installs updates and all the necessary programs for completing the
		installation.
	make kinect
		This installs the code for accessing and publishing the Kinect images to
		the ROS Master.
	make libfreenect2
		This installs the library essential for interacting with the Kinect 
		camera.
	make opencv
		This installs the OpenCV software used to process the Kinect images.
	make ros
		This install ROS-kinetic and all the necessary ROS libraries and tools.
	make play
		This will run the program for verifying the the Kinect libraries were 
		successfully installed.
	make clean
		This will remove the executablility of the bash files in this folder. 
		The bash files are used for the installation process and are given 
		executable permission in every make command that uses them.
		
================================================================================
				If Issues Occur
================================================================================
In the case where issues occur that you cannot solve on your own, please email 
me with screenshots and any relevent information about the issue and I will try
to help you as best I can.


Nate Hamilton
nathaniel.p.hamilton@vanderbilt.edu
3/28/2018




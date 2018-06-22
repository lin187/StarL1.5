#!/bin/bash
echo -e "\033[01;33m\033[1m\033[4m SETTING UP KINECT IMAGE PARSER... \033[00m"
echo -e "\e[1;32m 1 of 3: creating a catkin work space...\e[0m"
cd 
if [!-d catkin_ws]
then
	source /opt/ros/kinetic/setup.bash
	mkdir ~/catkin_ws/src
	cd ~/catkin_ws/
	catkin_make
fi
echo -e "\e[1;32m 2 of 3: unzipping the package...\e[0m"
cd ~/catkin_ws/src
if [-d camera_parser]
then
	sudo rm -r camera_parser
fi
cd ~/minnowBoardSetup
sudo apt-get install unzip
unzip camera_parser.zip -d ~/catkin_ws/src
echo -e "\e[1;32m 3 of 3: compiling the package...\e[0m"
cd ~/catkin_ws/
catkin_make
echo -e "\033[01;33m\033[1m\033[4m FINISHED SETTING UP KINECT IMAGE PARSER... \033[00m"

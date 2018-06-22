#!/bin/bash
echo -e "\033[01;33m\033[1m\033[4m ROS INSTALLATION STARTING... \033[00m"
echo -e "\e[1;32m 1 of 4: installing ROS kinetic...\e[0m"
sudo sh -c 'echo "deb http://packages.ros.org/ros/ubuntu $(lsb_release -sc) main" > /etc/apt/sources.list.d/ros-latest.list'
sudo apt-key adv --keyserver hkp://ha.pool.sks-keyservers.net:80 --recv-key 421C365BD9FF1F717815A3895523BAEEB01FA116
sudo apt-get -y install ros-kinetic-desktop-full
sudo apt-get -y install ros-kinetic-catkin
sudo apt-get -y install ros-kinetic-cv-bridge
sudo apt-get -y install ros-kinetic-vision-opencv
sudo apt-get -y install rosbash
echo -e "\e[1;32m 2 of 4: updating ROS...\e[0m"
sudo rosdep init
rosdep update
sudo rosdep fix-permissions
echo -e "\e[1;32m 3 of 4: sourcing ROS...\e[0m"
echo "source /opt/ros/kinetic/setup.bash" >> ~/.bashrc
source ~/.bashrc
echo -e "\e[1;32m 4 of 4: installing ROS essentials...\e[0m"
sudo apt-get -y install python-rosinstall python-rosinstall-generator python-wstool build-essential python-roslib python-roslaunch
echo -e "\033[01;33m\033[1m\033[4m ROS INSTALLATION COMPLETE. \033[00m"


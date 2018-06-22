#!/bin/bash
echo -e "\033[01;33m\033[1m\033[4m OPENCV INSTALLATION STARTING... \033[00m"
echo -e "\e[1;31m 1 of 5: navigating to opencv-2.4.9 for installation...\e[0m"
cd ~/opencv-2.4.9
echo -e "\e[1;31m 2 of 5: making and navigating to opencv build folder...\e[0m"
mkdir build
cd build
echo -e "\e[1;31m 3 of 5: cmake the opencv build...\e[0m"
cmake .. 
echo -e "\e[1;31m 4 of 5: make the opencv build...\e[0m"
make
echo -e "\e[1;31m 5 of 5: installing the opencv build...\e[0m"
sudo make install

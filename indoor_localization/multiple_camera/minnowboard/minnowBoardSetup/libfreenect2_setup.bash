#!/bin/bash
echo -e "\033[01;33m\033[1m\033[4m LIBFREENECT2 INSTALLATION STARTING... \033[00m"
echo -e "\e[1;32m 1 of 16: cloning kinect libraries...\e[0m"
cd 
sudo rm -r libfreenect2 freenect2
git clone https://github.com/OpenKinect/libfreenect2.git
echo -e "\e[1;32m 2 of 16: navigating to libfreenect2...\e[0m"
cd ~/libfreenect2
echo -e "\e[1;32m 3 of 16: navigating to dependencies folder and updating...\e[0m"
cd ~/libfreenect2/depends; ./download_debs_trusty.sh
echo -e "\e[1;32m 4 of 16: unpackaging USB library...\e[0m"
sudo dpkg -i debs/libusb*deb
echo -e "\e[1;32m 5 of 16: installing JPEG libraries...\e[0m"
sudo apt-get -y install libturbojpeg libjpeg-turbo8-dev
echo -e "\e[1;32m 6 of 16: unpacking and installing OpenGL...\e[0m"
sudo dpkg -i debs/libglfw3*deb; sudo apt-get install -f; sudo apt-get install libgl1-mesa-dri-lts-vivid
echo -e "\e[1;32m 7 of 16: installing openCL...\e[0m"
sudo apt-get -y install opencl-headers
echo -e "\e[1;32m 8 of 16: installing OpenNI2...\e[0m"
sudo apt-add-repository ppa:deb-rob/ros-trusty && sudo apt-get update
echo -e "\e[1;32m 9 of 16: also installing OpenNI2...\e[0m"
sudo apt-get -y install libopenni2-dev
echo -e "\e[1;32m 10 of 16: navigating to libfreenect2...\e[0m"
cd ~/libfreenect2/
echo -e "\e[1;32m 11 of 16: making and navigating to build folder...\e[0m"
mkdir build
cd build
echo -e "\e[1;32m 12 of 16: cmake the build...\e[0m"
cmake .. -DCMAKE_INSTALL_PREFIX=$HOME/freenect2 -Dfreenect2_DIR=$HOME/freenect2/lib/cmake/freenect2
echo -e "\e[1;32m 13 of 16: make the build...\e[0m"
make
echo -e "\e[1;32m 14 of 16: installing the build...\e[0m"
sudo make install
echo -e "\e[1;32m 15 of 16: copying rule files...\e[0m"
sudo cp ~/libfreenect2/platform/linux/udev/90-kinect2.rules /etc/udev/rules.d/
echo -e "\e[1;32m 16 of 16: navigating back...\e[0m"
cd ~/minnowBoardSetup


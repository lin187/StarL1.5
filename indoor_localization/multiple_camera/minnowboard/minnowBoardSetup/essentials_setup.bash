#!/bin/bash
echo -e "\033[01;33m\033[1m\033[4m ESSENTIALS INSTALLATION STARTING... \033[00m"
echo -e "\e[1;32m 1 of 5: getting updates...\e[0m"
sudo apt-get update
echo -e "\e[1;32m 2 of 5: updating distribution...\e[0m"
sudo apt-get -y dist-upgrade
sudo apt -y autoremove
echo -e "\e[1;32m 3 of 5: installing ssh requirements...\e[0m"
sudo apt-get -y install openssh-server openssh-client
echo -e "\e[1;32m 4 of 5: installing essentials build tools...\e[0m"
sudo apt-get install build-essential cmake pkg-config
echo -e "\e[1;32m 5 of 5: installing git...\e[0m"
sudo apt-get -y install git
echo -e "\033[01;33m\033[1m\033[4m ESSENTIALS INSTALLATION COMPLETE. \033[00m"

function find_robots(botList, cameraNum) 
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose: Find all the bots in their initial positions and identify them

%% Declare global variables that will be used or defined
global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4
global bots
global camera_locations
global camera_number
global colorMsgs

%% turn the botList from a string to a listof integers
specificList = str2num(char(botList));
robot_count = length(specificList);

%% Determine the number of each type of robot present
numDrones = 0;
numCreates = 0;
numARDrones = 0;
num3DRDrones = 0;
numGhostDrones = 0;
numMavicDrones = 0;
numPhant3Drones = 0;
numPhant4Drones = 0;
for i = specificList
    currentType = bots(i).type;
    if currentType == MINIDRONE
        numDrones = numDrones + 1;
    elseif currentType == CREATE2
        numCreates = numCreates + 1;
    elseif currentType == ARDRONE
        numARDrones = numARDrones + 1;
    elseif currentType == THREEDR
        num3DRDrones = num3DRDrones + 1;
    elseif currentType == GHOST2
        numGhostDrones = numGhostDrones + 1;
    elseif currentType == MAVICPRO
        numMavicDrones = numMavicDrones + 1;
    elseif currentType == PHANTOM3
        numPhant3Drones = numPhant3Drones + 1;
    elseif currentType == PHANTOM4
        numPhant4Drones = numPhant4Drones + 1;
    end
end

%% Run findBots to find all the bots and then assign their names
found = false;
while ~found
    % This loop will repeat if the color matching does not work or not all
    % of the robots are identified
    while ~found
    % Get the corresponding Camera images
    camera_number = cameraNum;
    imgColor = readImage(colorMsgs(cameraNum));
%     disp('Read the images')
    camera_number = 0;
    % make this function modify botArray, instead of return so many things
    [found, botArray] = findBots(imgColor, numDrones, numCreates, ...
        numARDrones, num3DRDrones, numGhostDrones, numMavicDrones, ...
        numPhant3Drones, numPhant4Drones, camera_locations(cameraNum,:));
    end
%     disp('I think I found them all')
    % Match each robot found to it's designated name
    numBotsUsed = 0;
%     disp('going into the for loop')
    for i = specificList
%         disp('in the for loop')
        k = 1;
        while k <= robot_count
%             disp('gonna try first if')
%             disp(bots(i).type)
%             disp(botArray(k).type)
            if bots(i).type == botArray(k).type
%                 disp('first if')
                if bots(i).color == botArray(k).color
                    % If the type and color match, then the robot name
                    % is given to that robot
                    botArray(k).name = char(bots(i).name);
                    bots(i) = botArray(k);
                    bots(i).cameraNum = cameraNum;
                    numBotsUsed = numBotsUsed + 1;
                    break;
                else
                    k = k + 1;
                end
            else
                k = k + 1;
            end
        end
    end
    
    % if not all of the robots are identified and color matched, the
    % process needs to start over and try again
    if numBotsUsed ~= robot_count
        found = false;
    end
end
return;
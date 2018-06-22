% central_command_main.m
%
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose:
%
close all;
clear all;
format longg;

load('run_number.mat')

%% Declare global variables
global disp_waypoints;
global disp_waypoint_names;
global fig_closed;
global clear_history;
global waypoints_transmitted;
global send_launch;
global IP;
global bots;
global bot_lists;
global camera_locations;
global frameCount;
global camera_number;
global imgColorAll;
global colorMsgs
global mm_per_pixel;
global camDistToFloor;
global BBoxFactor;
global hysteresis;
global numCameras;
global numBots;
global MINIDRONE;
global CREATE2;
global ARDRONE;
global THREEDR;
global GHOST2;
global MAVICPRO;
global PHANTOM3;
global PHANTOM4;

%% Define values for global variables that shouldn't be changed
MINIDRONE = 100;
CREATE2 = 101;
ARDRONE = 102;
THREEDR = 103;
GHOST2 = 104;
MAVICPRO = 105;
PHANTOM3 = 106;
PHANTOM4 = 107;
disp_waypoints = 1;
disp_waypoint_names = 0;
fig_closed = 0;
clear_history = 0;
waypoints_transmitted = 0;
send_launch = 0;


%% Define values for the settings variables. THESE CAN BE CHANGED
BBoxFactor = 1.7; % intentionally large because it is used for searching for drones not found in previous locations
hysteresis = 10;
camDistToFloor = 2700; % in mm, as measured with Camera
mm_per_pixel = 2.5; % mm in one pixel at ground level
IP = '10.255.24.255';
num_frames = 10000;
USE_SERVER = 1;  % Enable/disable the network server
USE_WPT = 1;     % Enable/disable loading waypoints and walls
USE_HISTORY = 1; % Enable/disable history
BOTLIST_FILENAME = 'robot_list.txt';

% Grid size and spacing parameters
TX_PERIOD = 0.05;	%sec
X_MAX = 3100;       %mm
Y_MAX = 3700;       %mm
LINE_LEN = 167;     %mm (should be iRobot Create radius)

% Path tracing variables
HISTORY_SIZE = 2500; %number of points in each history for drawing

% File export settings
SAVE_TO_FILE = false;
OUTPUT_FILENAME = 'C:\data.xml';

% image variables should default to the largets resolution available
imgColorAll = zeros(480,640,3,numCameras,num_frames,'uint8'); % FIX THIS !!!
camera_number = 0;

%% Setup the figure and save file
% Setup the figure
fig = figure('KeyPressFcn',@fig_key_handler);

% Setup save file
if(SAVE_TO_FILE)
   fileHandle = fopen(OUTPUT_FILENAME,'w+'); 
end

%% Parse the input and process it
% Open the list and parse through it to create the botID_list
[botID_list, cameraID_list, WPT_FILENAME] = parse_input(BOTLIST_FILENAME);

% Compare input to available nodes and do not proceed until all are
% accounted for
found = false;
while ~found
   [found, cameraTags] = verify_cameras_present(cameraID_list); 
   if ~found
       disp('Not all of the cameras are broadcasting. Trying again...')
   end
end

% Load the walls and waypoints (if required)
[walls, waypoints] = load_wpt(WPT_FILENAME, USE_WPT);

% Establish boundaries for each Camera node
establish_boundaries(camera_locations);

%% Setup subscribers
imgColorSubs = robotics.ros.Subscriber.empty(0,numCameras);
for i = 1:numCameras
    colorS = strcat(char(cameraTags(i)), 'imgColor');
    colorMsgs = [colorMsgs rosmessage('sensor_msgs/Image')];
    imgColorSubs(i) = rossubscriber(colorS,'sensor_msgs/Image',{@colorImageCollectionCallback,i});
end
pause(3);

%% Display keyboard shortcuts
disp('L - Launch robots');
disp('A - Abort all');
disp('W - Toggle waypoint viewing');
disp('N - Toggle waypoint name viewing');
disp('S - Resend waypoints');
disp('C - Clear history');
disp('X - Exit');
disp('Q - Track shutdown');

%% Find all the robots in their initial positions on the ground
for i = 1:numCameras
    find_robots(bot_lists(i),i); 
end
% disp('I founded all dem bots')
%% Track the robots and display their position in the figure
frameCount = 0;
tic;
while true
    frameCount = frameCount + 1;
    
    % Read all of the Camera images
    imgColor = read_all_camera_images(numCameras);
%     disp('I done read all the images')
    % Find the robots in each image
    for i = 1:numCameras
        track_bots(i, bot_lists(i), imgColor(:,:,:,i));
    end
    
    % Check each robot's location information for boundary crossing
    incomingList = find_crossings(bots);
    
    % Try to find the bots that crossed boundaries or were not found
    for i = 1:numCameras
        if strcmp(incomingList(i),'') == 0
            check_incoming(incomingList(i), i, imgColor(:,:,:,i));
        end
    end
    
    % Update the figure every 2 times
    if rem(frameCount,2) == 1
        plot_bots(fig, LINE_LEN, X_MAX, Y_MAX, bots, waypoints, walls,...
            disp_waypoints, disp_waypoint_names)
    end
    
    % Check the window command for exit command
    % Interpret an exit key press
    if get(fig,'currentkey') == 'x'
        disp('Exiting...');
        close all;
        shutdown_track = 0;
        judp('SEND',4000,IP,int8('ABORT'));
        break;
    end
    
    if get(fig,'currentkey') == 'q'
        disp('Exiting...');
        close all;
        shutdown_track = 1;
        judp('SEND',4000,IP,int8('ABORT'));
        break;
    end

    if USE_SERVER == 1 
        % Send positions to all connected nodes
        if ((toc > TX_PERIOD && send_launch ~= 1))
            server_send_robots(bots);
            tic;
        end
        
        % Send waypoints and robot positions
         if(waypoints_transmitted == 0)
             waypoints_transmitted = 1;
             server_send_waypoints(waypoints);
             if(SAVE_TO_FILE)
                save_robot_data(bots, fileHandle);
             end
         end

        % If launching the robots
        if send_launch == 1
            server_send_waypoints(waypoints);
            judp('SEND',4000,IP,int8(['GO ' int2str(size(waypoints,2)) ' ' int2str(run_number)]));
            run_number = run_number + 1;
            send_launch = 0;
        end
        
        % If aborting the robots
        if send_launch == -1
            judp('SEND',4000,IP,int8('ABORT'));
            disp('Aborting...');
            send_launch = 0;
            waypoints_transmitted = 1;
        end
    end
end

%% Upon exit...
save('run_number.mat', 'run_number');













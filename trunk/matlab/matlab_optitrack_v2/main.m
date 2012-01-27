% Main OptiTrack interface
% Settings
TTP_FILENAME = 'C:\no_bob.ttp';
WPT_FILENAME = 'C:\game_waypoints.wpt';
USE_SERVER = 1; %Enable/disable the network server
USE_WPT = 1;    %Enable/disable loading waypoints and walls
USE_HISTORY = 1;%Enable/disable robot history tracking
ROBOT_COUNT = 3;

close all;
format longg;

% Start the tracker interface if it isn't running
if exist('cameraCount','var') == 0
    cameraCount = track_init(TTP_FILENAME);
    clc;
end

% Display keyboard shortcuts
disp('L - Launch robots');
disp('W - Toggle waypoint viewing');
disp('E - Exit');

% Grid size and spacing parameters
TX_PERIOD = 0.08;   %sec
X_MAX = 3450;       %mm
Y_MAX = 3700;       %mm
LINE_LEN = 167;     %mm (should be iRobot Create radius)

% Load the walls and waypoints (if required)
[walls waypoints] = load_wpt(WPT_FILENAME, USE_WPT);
global disp_waypoints;
disp_waypoints = 1;

% Only transmit the waypoints once
global waypoints_transmitted;
waypoints_transmitted = 0;

% Launch variable
global send_launch;
send_launch = 0;

% Get all trackable robots, set up a structure to hold them
[robot_count robot_names] = track_getTrackables();

bots = zeros(robot_count, 4);
% bots = struct('X',{0},'Y',{0},'yaw',{0},'visible',{0},'name',robot_names);

% Start the server
if USE_SERVER == 1 
    if ROBOT_COUNT > 0
        [conns, streams, con_count] = server_init(ROBOT_COUNT,4000,-1);
    else
        [conns, streams, con_count] = server_init(robot_count,4000,-1);
    end
end

% Set up the plot
fig = figure('KeyPressFcn',@fig_key_handler);

% Start the frame count for drawing and timer for transmitting
frameCount = 0;
tic;
while 1
    frameCount = frameCount + 1;
    track_updateFrame()
    
    % Get each robots position (if visible)
    for i = 1:robot_count
        if track_isVisible(i-1)
            [x y z roll pitch yaw] = track_getTrackablePosition(i-1);
            % Round and convert to millimeters
            bots(i,:) = [round(x*1000) round(z*1000) round(yaw)+90 1];
        else
            bots(i,4) = 0;
        end
    end
    
    % Update the plot on every 4th frame
    if rem(frameCount,4) == 0
        plot_bots(robot_count, fig, LINE_LEN, X_MAX, Y_MAX, bots, waypoints, walls)
    end
    
    % Interpret an exit key press
    if (get(fig,'currentkey') == 'e')
        disp('Exiting...');
        close all;
        break;
    end

    if USE_SERVER == 1
        % Shut down if no robots remain
        if get_remaining_connections(streams) == 0
            disp('No listening robots, shutting down');
            close all;
            break;
        end
        
        % Send positions to all connected nodes
        if toc > TX_PERIOD && send_launch ~= 1
            server_send(bots, waypoints, streams, conns, con_count);
            tic;
        end

        % If launching the robots
        if send_launch == 1
            server_send_launch(streams, conns, con_count);
            send_launch = 2;
        end
    end
end

%Shut down the server and tracker
if RESTART_ON_EXIT == 0
    track_shutdown();
    close all
    clear
else
    waypoints_transmitted = 0;
    main
end
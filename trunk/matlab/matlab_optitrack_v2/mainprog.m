close all;
format longg;

% Start the tracker interface if it isn't running
if ~exist('cameraCount', 'var')
    cameraCount = track_init(TTP_FILENAME);
    clc;
end

% Display keyboard shortcuts
disp('L - Launch robots');
disp('A - Abort all');
disp('W - Toggle waypoint viewing');
disp('N - Toggle waypoint name viewing');
disp('S - Resend waypoints');
disp('C - Clear history');
disp('X - Exit');
disp('Q - Track shutdown');

% Load the walls and waypoints (if required)
[walls waypoints] = load_wpt(WPT_FILENAME, USE_WPT);
global disp_waypoints;
disp_waypoints = 1;
global disp_waypoint_names;
disp_waypoint_names = 0;
global fig_closed;
fig_closed = 0;
global clear_history;
clear_history = 0;

% Only transmit the waypoints once
global waypoints_transmitted;
waypoints_transmitted = 0;

% Launch variable
global send_launch;
send_launch = 0;

% Get all trackable robots, set up a structure to hold them
[robot_count robot_names] = track_getTrackables();
bots = struct('X',{0},'Y',{0},'yaw',{0},'visible',{0},'name',robot_names,...
    'history',{ones(HISTORY_SIZE,2)*-1},'hist_index',{1});

% Set up the plot
fig = figure('KeyPressFcn',@fig_key_handler);

% IF THIS CRASHES, ERASE THIS LINE:
%set(gcf,'CloseRequestFcn',@close_fig_handler);

% Start the frame count for drawing and timer for transmitting
frameCount = 0;
tic;
while 1
    frameCount = frameCount + 1;
    track_updateFrame()
    
    % If the figure was closed, exit
    if fig_closed == 1
        break;
    end
    
    if clear_history == 1
        disp('Clearing history');
        for i = 1:robot_count
            bots(i).history = ones(HISTORY_SIZE,2)*-1;
        end
        clear_history = 0;
    end
    
    % Get each robots position (if visible)
    for i = 1:robot_count
        if track_isVisible(i-1)
            [x y z roll pitch yaw] = track_getTrackablePosition(i-1);
            % Round and convert to millimeters
            bots(i).X = round(x*1000);
            bots(i).Y = round(z*1000);
            bots(i).yaw = round(yaw)+90;
            bots(i).visible = 1;
            
            % Append the new point to the history
            hist = bots(i).history;
            if ~ismember([bots(i).X bots(i).Y], hist, 'rows')
                hist_index = mod(bots(i).hist_index,HISTORY_SIZE)+1;
                bots(i).history(bots(i).hist_index,:) = [bots(i).X bots(i).Y];
                bots(i).hist_index = hist_index;
            end
        else
            bots(i).visible = 0;
        end
    end
   
    % Update the plot on every 4th frame
    if rem(frameCount,4) == 0
        plot_bots(fig, LINE_LEN, X_MAX, Y_MAX, bots, waypoints, walls,...
            disp_waypoints, disp_waypoint_names)
    end
    
    % Interpret an exit key press
    if get(fig,'currentkey') == 'x'
        disp('Exiting...');
        close all;
        shutdown_track = 0;
        judp('SEND',4000,'192.168.1.255',int8('ABORT'));
        break;
    end
    
    if get(fig,'currentkey') == 'q'
        disp('Exiting...');
        close all;
        shutdown_track = 1;
        judp('SEND',4000,'192.168.1.255',int8('ABORT'));
        break;
    end

    if USE_SERVER == 1       
        % Send positions to all connected nodes
        if ((toc > TX_PERIOD && send_launch ~= 1) || waypoints_transmitted == 0)
            server_send_udp(bots, waypoints);
            tic;
        end

        % If launching the robots
        if send_launch == 1
            judp('SEND',4000,'192.168.1.255',int8(['GO ' int2str(size(waypoints,2))]));
            send_launch = 0;
        end
        
        % If aborting the robots
        if send_launch == -1
            judp('SEND',4000,'192.168.1.255',int8('ABORT'));
            disp('Aborting...');
            send_launch = 0;
            waypoints_transmitted = 1;
        end
    end
    
end

if shutdown_track == 1
    track_shutdown();
    
%     if exist('RETURN_DIR','var')
%         cd(RETURN_DIR);
%     end
    clear
end
close all
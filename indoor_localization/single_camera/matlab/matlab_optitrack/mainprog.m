close all;
format longg;

load('run_number.mat')

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
    'history',{ones(MOTION_HISTORY_SIZE,2)*-1},'histangle',{ones(MOTION_HISTORY_SIZE,1)*-1},...
    'hist_index',{1},'drawhistory',{ones(HISTORY_SIZE,2)*-1},'draw_hist_index',{1});

% Set up the plot
fig = figure('KeyPressFcn',@fig_key_handler);

% Set up the file handle
if(SAVE_TO_FILE)
   fileHandle = fopen(OUTPUT_FILENAME,'w+'); 
end

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
            bots(i).drawhistory = ones(HISTORY_SIZE,2)*-1;
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
            hist_index = mod(bots(i).hist_index,MOTION_HISTORY_SIZE)+1;
            bots(i).history(bots(i).hist_index,:) = [bots(i).X bots(i).Y];
            bots(i).histangle(bots(i).hist_index) = bots(i).yaw;
            bots(i).hist_index = hist_index;
            
            hist = bots(i).drawhistory;
            if ~ismember([bots(i).X bots(i).Y], hist, 'rows')
                draw_hist_index = mod(bots(i).draw_hist_index,HISTORY_SIZE)+1;
                bots(i).drawhistory(bots(i).draw_hist_index,:) = [bots(i).X bots(i).Y];
                bots(i).draw_hist_index = draw_hist_index;
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
        //judp('SEND',4000,'192.168.1.255',int8('ABORT'));
        judp('SEND',4000,'10.255.24.255',int8('ABORT'));
        break;
    end
    
    if get(fig,'currentkey') == 'q'
        disp('Exiting...');
        close all;
        shutdown_track = 1;
        //judp('SEND',4000,'192.168.1.255',int8('ABORT'));
        judp('SEND',4000,'10.255.24.255',int8('ABORT'));
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
             server_send_robots(bots);
             if(SAVE_TO_FILE)
                save_robot_data(bots, fileHandle);
             end
         end

        % If launching the robots
        if send_launch == 1
            server_send_waypoints(waypoints);
            server_send_robots(bots);
            //judp('SEND',4000,'192.168.1.255',int8(['GO ' int2str(size(waypoints,2)) ' ' int2str(run_number)]));
            judp('SEND',4000,'10.255.24.255',int8(['GO ' int2str(size(waypoints,2)) ' ' int2str(run_number)]));
            run_number = run_number + 1;
            send_launch = 0;
        end
        
        % If aborting the robots
        if send_launch == -1
            //judp('SEND',4000,'192.168.1.255',int8('ABORT'));
            judp('SEND',4000,'10.255.24.255',int8('ABORT'));
            disp('Aborting...');
            send_launch = 0;
            waypoints_transmitted = 1;
        end
    end
    
end

if(SAVE_TO_FILE)
    fclose(fileHandle);
end

if shutdown_track == 1
    track_shutdown();
    save('run_number.mat', 'run_number');
    clear
end

close all

close all;
format longg;
warning('off','images:imfindcircles:warnForSmallRadius')
load('run_number.mat')
num_frames = 10000; % number of frames kinect will capture
% allocate space for saving images
global imgColorAll
imgColorAll = zeros(480,640,3,num_frames,'uint8');
global mm_per_pixel;
mm_per_pixel = 5.628;
times = [];

% size of boudning box to be used in localization (x times larger than
% circle diameter)
BBoxFactor = 1.2; 
%fig2 = figure(2);
found = false;
robot_count = 2;

%Set Up Kinect for tracking
stop([vid vid2]); % comment this out the first time code is run
clear vid vid2; % comment this one out too
vid = videoinput('kinect',1); %color 
vid2 = videoinput('kinect',2); %depth

srcDepth = getselectedsource(vid2);
vid.FramesPerTrigger = 1;
vid2.FramesPerTrigger = 1;

vid.TriggerRepeat = num_frames;
vid2.TriggerRepeat = num_frames;

triggerconfig([vid vid2],'manual');
start([vid vid2]);
% tigger a couple times incase first frames are bad
trigger([vid vid2])
trigger([vid vid2])

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
[walls, waypoints] = load_wpt(WPT_FILENAME, USE_WPT);
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
%[robot_count robot_names] = track_getTrackables();
% Set this manually for now when using Kinect tracking

robot_names = cell(1,robot_count);
robot_names{1} = 'bot0';
robot_names{2} = 'bot1';
bots = struct('X',{0},'Y',{0},'yaw',{0},'visible',{0},'name',robot_names,...
    'history',{ones(MOTION_HISTORY_SIZE,2)*-1},'histangle',{ones(MOTION_HISTORY_SIZE,1)*-1},...
    'hist_index',{1},'drawhistory',{ones(HISTORY_SIZE,2)*-1},'draw_hist_index',{1});

% make array of Create2 objects
botArray = Create2.empty(robot_count,0);
for i = 1:robot_count
    botArray(i) = Create2;
end

% Set up the plot
fig = figure('KeyPressFcn',@fig_key_handler);

% Set up the file handle
if(SAVE_TO_FILE)
   fileHandle = fopen(OUTPUT_FILENAME,'w+'); 
end

% Start the frame count for drawing and timer for transmitting
frameCount = 0;
launched = false;
tic;
while 1
    tic;
    frameCount = frameCount + 1;
    % Trigger both objects.
    trigger([vid vid2])
    % Get the acquired frames and metadata.
    [imgColor, ts_color, metaData_Color] = getdata(vid);
    imgColorAll(:,:,:,frameCount) = imgColor;
    % flip the image so the orientation is consistent with the other plot
    %imgColor = flipud(imgColor);
    [imgDepth, ts_depth, metaData_Depth] = getdata(vid2);
    
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

    % Get bots positions with Kinect
    if ~found 
        [centers, radii, BBoxes, yaws, colors] = findCreate2(imgColor, robot_count, BBoxFactor);
        if length(radii) == robot_count
            found = true;
             for j = 1:robot_count
                    botArray(j).center = centers(j,:);
                    center_mm = getMMCoordiRobot(centers(j,:));
                    botArray(j).centers = [botArray(j).centers; centers(j,:)];
                    bots(j).X = center_mm(1,1);
                    bots(j).Y = center_mm(1,2);
                    botArray(j).radius = radii(j);
                    botArray(j).radii = [botArray(j).radii; radii(j)];
                    botArray(j).BBox(1,:) = BBoxes(j,:);
                    botArray(j).color = colors(j);
                    botArray(j).yaw = yaws(j);
                    botArray(j).yaws = [botArray(j).yaws; yaws(j)];
                    bots(j).yaw = botArray(j).yaw;
                    bots(j).visible = 1;
                    if botArray(j).color == 'r'
                        bots(j).name = 'bot0';
                    elseif botArray(j).color == 'g';
                        bots(j).name = 'bot1';
                    end
             end
        end
    else
        for j = 1:robot_count
            % find the drone center, etc.
            [center, radius, BBox, yaw] = trackCreate2(imgColor, botArray(j), BBoxFactor);
            botArray(j).center = center;
            center_mm = getMMCoordiRobot(center);
            botArray(j).centers = [botArray(j).centers; center];
            bots(j).X = center_mm(1,1);
            bots(j).Y = center_mm(1,2);
            botArray(j).radius = radius;
            botArray(j).radii = [botArray(j).radii; radius];
            botArray(j).BBox = BBox;
            botArray(j).yaw = yaw;
            botArray(j).yaws = [botArray(j).yaws; yaw];
            bots(j).yaw = botArray(j).yaw;
            bots(j).visible = 1;
        end      
    end
    times = [times; toc];
    % plot the localized bots
    if launched
       % plotCreate2(imgColor, botArray, robot_count, frameCount, fig2);
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
            judp('SEND',4000,'192.168.1.255',int8(['GO ' int2str(size(waypoints,2)) ' ' int2str(run_number)]));
            run_number = run_number + 1;
            send_launch = 0;
            launched = true;
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

if(SAVE_TO_FILE)
    fclose(fileHandle);
end

stop([vid vid2]);
close all
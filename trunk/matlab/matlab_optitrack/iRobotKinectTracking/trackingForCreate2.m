% most of this code from the following
% http://www.mathworks.com/help/imaq/acquiring-from-the-color-and-depth-devices-simultaneously.html
stop([vid vid2]);
clear;
warning('off','images:imfindcircles:warnForSmallRadius')
global imgColorAll
% 1 to plot, 0 to not plot. not plotting makes it run faster
plot = 1;
% variables
offset = 3; % skips this many frames. Sometimes the first one or two frames are bad
num_frames = 2000; % number of frames to capture
imgColorAll = zeros(420,560,3,num_frames,'uint8');
colorImgs = zeros(480,640,3,num_frames,'uint8');
numBots = 1;
BBoxFactor = 1.5; % scale for BBox. Only BBox is input to imfindcircles
goalRadius = 10;
found = false;
times = zeros(num_frames,1);
% create array of drone objects
botArray = Create2.empty(numBots,0);
for i = 1:numBots
    botArray(i) = Create2;
end

% set up things to track with the kinect
vid = videoinput('kinect',1); %color 
vid2 = videoinput('kinect',2); %depth

srcDepth = getselectedsource(vid2);
vid.FramesPerTrigger = 1;
vid2.FramesPerTrigger = 1;

vid.TriggerRepeat = num_frames;
vid2.TriggerRepeat = num_frames;

triggerconfig([vid vid2],'manual');
start([vid vid2]);

% each loop iteration captures one frame
for i = 1:num_frames + 1
    tic;
    % Trigger both objects.
    trigger([vid vid2])
    % Get the acquired frames and metadata.
    [imgColor, ts_color, metaData_Color] = getdata(vid);
    % Save each color frame in array
    if ~plot
        colorImgs(:,:,:,i) = imgColor(:,:,:,1);
    end
    % Get depth
    [imgDepth, ts_depth, metaData_Depth] = getdata(vid2);
    % Save depth frame in array
    %imgDepthAll(:,:,i) = imgDepth(:,:,1);
    
    if i > offset %skip first few frames that could be bad
        % if drones haven't been found, find with findDrone
       
        if ~ found
            'started'
            [centers, radii, BBoxes, yaws, colors] = findCreate2(imgColor, numBots, BBoxFactor);
            t = clock;
            if length(radii) == numBots
                found = true;
                for j = 1:numBots
                    botArray(j).center = centers(j,:);
                    botArray(j).radius = radii(j);
                    botArray(j).BBox(1,:) = BBoxes(j,:);
                    botArray(j).color = colors(j);
                    botArray(j).yaw = yaws(j);
                end  
            end
            
        else
            for j = 1:numBots
                % find the drone center, etc.
                [center, radius, BBox, yaw] = trackCreate2(imgColor, botArray(j), BBoxFactor);
                t = clock; % get time right after localization
                % set new values for drone parameters
                botArray(j).center = center;
                botArray(j).radius = radius;
                botArray(j).BBox = BBox;
                botArray(j).yaw = yaw;  
            end 
        end
        
        if plot
            plotCreate2(imgColor, botArray, numBots, i - offset);
        end
    end
  times_total(i) =  toc;
end

% stops the kinect so you can use it again
stop([vid vid2]);
%TODO: Add header

% Global variables that will be used throughout the program
global imgColorAll
global mm_per_pixel
global camDistToFloor
global BBoxFactor
global camera_number
global colorMsgs
global depthMsgs
global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4
MINIDRONE = 100;
CREATE2 = 101;
ARDRONE = 102;
THREEDR = 103;
GHOST2 = 104;
MAVICPRO = 105;
PHANTOM3 = 106;
PHANTOM4 = 107;

% Basic Kinect Setup as used in **
warning('off','images:imfindcircles:warnForSmallRadius')
num_frames = 10000; % number of frames kinect will capture
%imgColorAll = zeros(480,640,3,num_frames,'uint8'); % stores all captured imgs
BBoxFactor = 1.5;

%set up the Kinect
camera_number = 0;
colorS = strcat('/camera1/', 'imgColor');
%depthS = strcat('/camera1/', 'imgDepth');
colorMsgs = rosmessage('sensor_msgs/Image');
%depthMsgs = rosmessage('sensor_msgs/Image');
imgColorSub = rossubscriber(colorS,'sensor_msgs/Image',{@colorImageCollectionCallback,1});
%imgDepthSub = rossubscriber(depthS,'sensor_msgs/Image',{@depthImageCollectionCallback,1});

pause(3);

%Now that the Kinect is setup, take a shot and show where all the circles
%found are
camera_number = 1;
imgColor = readImage(colorMsgs);
%imgDepth = readImage(depthMsgs);
camera_number = 0;

% cont = input('Clear Kinect view and press enter to continue.');
% TODO: FIGURE OUT THE VALUES!!!!
mm_per_pixel = 2.5; %5.663295322; % mm in one pixel at ground level
camDistToFloor = 2700; % in mm, as measured with Kinect

type = MINIDRONE;
while true
    disp('Parrot AR minidrone = 100');
    disp('iRobot Create2      = 101'); %TODO add robot numbers
    disp('Parrot AR DRONE2.0  = 102');
    disp('3DR SOLO            = 103');
    disp('GhostDrone 2.0      = 104');
    disp('Mavic Pro           = 105');
    disp('Phantom 3           = 106');
    disp('Phantom 4           = 107');
    type = input('Enter robot number: ');
    if (isGroundRobot(type) == 1 || isAerialDrone(type) == 1)
        break;
    end
    disp('Invalid input. Try again...');
    disp(' ');
end

num_depths = input('Enter the number of depths to be measured (default is 1): ');
if num_depths > 20 || num_depths < 1
    num_depth = 1;
end

iDepths = input('What are the depths that you measured in mm? [input as list separated by commas or spaces] ', 's');
avg_depths = str2num(iDepths)';

num_reads = input('Enter the number of measurements to take at each depth (default is 5): ');
if num_reads < 1
    num_reads = 1;
end

measured_radii = zeros(num_depths, num_reads);
measured_depths = zeros(num_depths, num_reads);

% Ask the user to place the robot on the ground in the middle of the target space
cont = input('Place robot on the ground under the Kinect and press enter to continue.');

for i = 1:num_depths
    % This range is big enough to pick up any robot and it should be reset
    % at each level just in case
    rmin = 40;
    rmax = 80;
    
    % Find the robot for the first time
    cont = 4;
    while cont ~= 1
        if cont == 2
            rmin = rmin + 1;
        elseif cont == 3
            rmax = rmax - 1;
        elseif cont == 4
            rmax = rmax + 1;
        elseif cont == 5
            rmin = rmin - 1;
        end
        camera_number = 1;
        imgColor = readImage(colorMsgs);
        %imgDepth = readImage(depthMsgs);
        camera_number = 0;
        
        % Find all of the circle in the frame
        [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);

        %show all of the circles that were found
        figure(i);
        image(imgColor)
        hold on
        viscircles(centers, radii);
        
        % Make sure the circles detected are the ones desired
        s = sprintf('Look at Figure %i. Enter ''1'' to continue, ''2'' to increase the minimum radius, ''3'' to decrease the maximum radius, or ''4'' to increase the maximum: ',i);
        cont = input(s);
    end

    % Calculate the bounded box for the robot at this level. It shouldn't
    % change because the robot shouldn't be moving.
    BBox = getBBox(mean(centers,1), mean(radii), type, BBoxFactor);
    for j = 1:num_reads
        camera_number = 1;
        imgColor = readImage(colorMsgs);
        %imgDepth = readImage(depthMsgs);
        camera_number = 0;
        
        % Clip the frame to observe only the area around the robot
%         depthFrame = getPixelsInDepthBB(imgDepth, BBox);
        frame = getPixelsInColorBB(imgColor, BBox);
%         
%         % Determine the depth of the robot and record it
%         measured_depths(i,j) = findDepth(depthFrame);
        
        % Find all the circles present in the frame
        [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
        
        % Record the average radius measured
        measured_radii(i,j) = mean(radii);
    end

    % Prompt user to get ready for the next level if this is not the last
    % level to be measured
    if i ~= num_depths
        cont = input('Elevate robot to the next height level to be measured at and then press enter to continue.');
    else
        cont = input('The last depth has been measured. Press enter to continue.');
    end
end

% Average the readings at each depth
%avg_depths = mean(measured_depths,2);
avg_radii = mean(measured_radii,2);

% Fit and plot the data across as many ranges as possible
if num_depths > 1
    for i = 2:num_depths
        s = sprintf('poly%i',i-1);
        f = fit(avg_radii, avg_depths, s);
        figure;
        plot(f, avg_radii, avg_depths);
        title(s);
    end
    
    % Ask the user for their prefered polynomial and display the desired
    % coefficients
    q = sprintf('Enter the number of the prefered polynomial for the coefficients [1 - %i]: ', i-1);
    choice = input(q);
    s = sprintf('poly%i',choice);
    f = fit(avg_radii, avg_depths, s);
    f
else
    % If the number of depths measured is not greater than 1, then a
    % correlation cannot be determined so just display the measured values
    disp('Measured values: ');
    measured_depths
    measured_radii
end

return;
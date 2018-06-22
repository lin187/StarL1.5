%nates_test
% 6/7/2017
% The purpose of this script is to test the Kinect's circle finding ability 
% because it is currently struggling

% Some global variables needed for Kinect
% Matlab didn't like them being in the Kinect if statement below
% They aren't needed when using Optitrack except for the robot types
% MINIDRONE, CREATE2, ARDRONE, THREEDR, GHOST2, MAVICPRO, PHANTOM3,
% and PHANTOM4
global numCreates
global numDrones
global numARDrones
global num3DRDrones
global numGhostDrones
global numMavicDrones
global numPhant3Drones
global numPhant4Drones

global imgColorAll
global mm_per_pixel
global camDistToFloor
global botArray
global BBoxFactor
global hysteresis
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

%variables that will be changed frequently
numCreates = 0;
numDrones = 0; %minidrones
numARDrones = 1;
num3DRDrones = 0;
numGhostDrones = 0;
numMavicDrones = 0;
numPhant3Drones = 0;
numPhant4Drones = 0;
BBoxFactor = 1.5;
hysteresis = 10; % number of consecutive frames bot must be lost for auto-shutdown

% Other things needed for Kinect tracking
warning('off','images:imfindcircles:warnForSmallRadius')
num_frames = 10000; % number of frames kinect will capture
imgColorAll = zeros(480,640,3,num_frames,'uint8'); % stores all captured imgs
mm_per_pixel = 5.663295322; % mm in one pixel at ground level
found = false;
robot_count = numCreates + numDrones + numARDrones + num3DRDrones + numGhostDrones + numMavicDrones + numPhant3Drones + numPhant4Drones;
camDistToFloor = 3058; % in mm, as measured with Kinect
robot_names = cell(1,robot_count);
botArray = Robot.empty(robot_count,0);
for i = 1:robot_count
    botArray(i) = Robot;
end

% close all open function windows
close all

%set up the Kinect
if exist('vid', 'var') && exist('vid2', 'var')
    stop([vid vid2]);
    clear vid vid2;
elseif exist('vid1', 'var') && exist('vid2', 'var')
    stop([vid1 vid2]);
    clear vid1 vid2;
end

vid = videoinput('kinect',1); %color 
vid2 = videoinput('kinect',2); %depth

vid.FramesPerTrigger = 1;
vid2.FramesPerTrigger = 1;

vid.TriggerRepeat = num_frames;
vid2.TriggerRepeat = num_frames;

triggerconfig([vid vid2],'manual');
start([vid vid2]);
% tigger a couple times in case first frames are bad
trigger([vid vid2])
trigger([vid vid2])

%Now that the Kinect is setup, take a shot and show where all the circles
%found are
trigger([vid vid2])
% Get the acquired frames and metadata.
[imgColor, ts_color, metaData_Color] = getdata(vid);
[imgDepth, ts_depth, metaData_Depth] = getdata(vid2);

% find depth
depth = 2900;%2900
% find circles
rmin = 20;
rmax = 50;

%findRadiusRange(depth);
[centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
    'ObjectPolarity', 'dark', 'Sensitivity', 0.92);

%show all the circle found
figure(1);
image(imgColor)
hold on
viscircles(centers, radii);
hold off

% % show the 5 smallest circles
% [radii_sorted, I] = sort(radii);
% centers_sorted = [];
% for k = 1:length(radii)
%     centers_sorted(k,:) = centers(I(k),:);
% end
% radii_sorted_cut = zeros(5,1);
% centers_sorted_cut = zeros(5,2);
% for i = 1:5
%     radii_sorted_cut(i) = radii_sorted(i);
%     centers_sorted_cut(i,:) = centers_sorted(i,:);
% end
% figure();
% image(imgColor)
% hold on
% viscircles(centers_sorted_cut, radii_sorted_cut);
% hold off


%now that the Kinect is setup, find the robots
trigger([vid vid2])
% Get the acquired frames and metadata.
[imgColor, ts_color, metaData_Color] = getdata(vid);
[imgDepth, ts_depth, metaData_Depth] = getdata(vid2);
% make this function modify botArray, instead of return so many things
found = findBotsNateTest(imgColor, imgDepth);
% if not all of the bots were found, try again until they are
while ~found
    disp('Could not find all the bots. Trying again...');
    trigger([vid vid2])
    % Get the acquired frames and metadata.
    [imgColor, ts_color, metaData_Color] = getdata(vid);
    [imgDepth, ts_depth, metaData_Depth] = getdata(vid2);
    % make this function modify botArray, instead of return so many things
    found = findBotsNateTest(imgColor, imgDepth);
end

figure();
image(imgColor)
hold on
for i = 1:robot_count
%     disp('Showing robot #:');
%     disp(i);
    switch botArray(i).type
        case MINIDRONE
            c = 'b';
            l = '-';
        case CREATE2
            c = 'g';
            l = '-';
        case ARDRONE
            c = 'r';
            l = '-';
        case THREEDR
            c = 'y';
            l = '-';
        case GHOST2
            c = 'k';
            l = '-';
        case MAVICPRO
            c = 'r';
            l = '-.';
        case PHANTOM3
            c = 'b';
            l = '-.';
        case PHANTOM4
            c = 'g';
            l = '-.';
        otherwise
            c = 'k';
            l = ':';
    end
    viscircles(botArray(i).center, botArray(i).radius, 'EdgeColor', c, 'LineStyle', l);   
end
hold off

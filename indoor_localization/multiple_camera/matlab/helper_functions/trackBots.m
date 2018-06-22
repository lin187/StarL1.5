function trackBots(imgColor, index, cameraNum)
global bots
global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVIKPRO
global PHANTOM3
global PHANTOM4
global camDistToFloor
global hysteresis
global BBoxFactor
global camera_locations

%% Get pixels in bounding box of bot
frame = getPixelsInColorBB(imgColor, bots(index).BBox);

%% Define some variables that will be used later
centers = [];
radii = [];
metrics = [];
[sizeX, sizeY, sizeZ] = size(frame);

%% Determine the radius of the circle that should be looked for and then find the corresponding circles
if bots(index).type == MINIDRONE
    % calculate rmin and rmax
    [rmin, rmax] = findRadiusRange(sizeX, MINIDRONE, BBoxFactor);
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
    figure(2);
    image(frame);
    hold on
    viscircles(centers, radii);
    hold off
elseif bots(index).type == CREATE2
    rmin = 25;
    rmax = 35;
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.96);
elseif bots(index).type == ARDRONE
    [rmin, rmax] = findRadiusRange(sizeX, ARDRONE, BBoxFactor);
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
    % not enough circles found, clear centers so function will return below
    if length(centers) < 4
%         figure();
%         image(frame);
%         hold on
%         viscircles(centers, radii);
%         hold off
        centers = [];
    else
        centers = centers(1:4,:);
        % find mean of 4 circles to get center 
        ARCenters = [centers(1,:);centers(2,:); ...
            centers(3,:); centers(4,:)];
        % find an average radius value
        ARRadii = [radii(1), radii(2), radii(3), ...
            radii(4)];
        centers = mean(ARCenters);
        radii = mean(ARRadii);  
        metrics = 1;
    end
elseif bots(index).type == GHOST2
    [rmin, rmax] = findRadiusRange(sizeX, GHOST2, BBoxFactor);
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
    % not enough circles found, clear centers so function will return below
    if length(centers) < 4
%         figure();
%         image(frame);
%         hold on
%         viscircles(centers, radii);
%         hold off
        centers = [];
    else
        centers = centers(1:4,:);
        % find mean of 4 circles to get center 
        GhostCenters = [centers(1,:);centers(2,:); ...
            centers(3,:); centers(4,:)];
        % find an average radius value
        GhostRadii = [radii(1), radii(2), radii(3), ...
            radii(4)];
        centers = mean(GhostCenters);
        radii = mean(GhostRadii);  
        metrics = 1;
    end
end

%% If not found, add current value to accum values and return
if isempty(centers)
    [bots(index).color, ' bot not found']
    bots(index).centers = [bots(index).centers; bots(index).center];
    bots(index).depths = [bots(index).depths, bots(index).depth];
    bots(index).radii = [bots(index).radii, bots(index).radius];
    bots(index).yaws = [bots(index).yaws, bots(index).yaw];
    bots(index).hyst = bots(index).hyst + 1;
    return
end

%% keep strongest circle, put back in original coordinates for identification
[~, indexCircle] = max(metrics);

%% Make sure the color matches what it should be
color = getColor(frame, centers(indexCircle,:));
if color ~= bots(index).color
    % If the color is wrong, then the bot wasn't found
    [bots(index).color, ' bot not found due to color mismatch']
    bots(index).centers = [bots(index).centers; bots(index).center];
    bots(index).depths = [bots(index).depths, bots(index).depth];
    bots(index).radii = [bots(index).radii, bots(index).radius];
    bots(index).yaws = [bots(index).yaws, bots(index).yaw];
    bots(index).hyst = bots(index).hyst + 1;
    return
end

%% If this part of the code is reached, the robot has been found 
% hysteresis should reset
bots(index).hyst = 0;

% add center to bots making sure to add BBox to get back in whole image px coord
bots(index).center(1,1) = centers(indexCircle,1) + max([bots(index).BBox(1),1]);
bots(index).center(1,2) = centers(indexCircle,2) + max([bots(index).BBox(2),1]);
bots(index).centers = [bots(index).centers; bots(index).center];

% add radius to bot array
bots(index).radius = radii(indexCircle,:);
bots(index).radii = [bots(index).radii, radii(indexCircle,:)];

% find bbox
bots(index).BBox = getBBox(bots(index).center, bots(index).radius, bots(index).type, BBoxFactor);
bots(index).BBoxes = [bots(index).BBoxes; bots(index).BBox];

% add depth found if drone, add dist to floor if create and find yaws
if isAerialDrone(bots(index).type)
    bots(index).depth = findDepth(bots(index).radius, bots(index).type);
    bots(index).yaw = findYaw(imgColor,  bots(index).BBox,...
        bots(index).yaw, bots(index).center, bots(index).radius, bots(index).type);
    %bots(index).yaw = 0;
elseif isGroundRobot(bots(index).type)
    bots(index).depth = camDistToFloor;
    bots(index).yaw = findYaw(imgColor, bots(index).BBox, ...
        bots(index).yaw, bots(index).center, bots(index).radius, CREATE2);
else
    'error - Not a ground or Aerial Robot'
end

% add accumulated values
bots(index).yaws = [bots(index).yaws, bots(index).yaw];
bots(index).depths = [bots(index).depths, bots(index).depth];
bots(index).cameraNums = [bots(index).cameraNums, cameraNum];

% Update X, Y, and Z coordinates
centerMM = getMMCoord(camera_locations(cameraNum,:), bots(index).center, bots(index).radius, bots(index).type);
bots(index).X = centerMM(1,1);
bots(index).Xs = [bots(index).Xs; bots(index).X];
bots(index).Y = centerMM(1,2);
bots(index).Ys = [bots(index).Ys; bots(index).Y];
bots(index).Z = camDistToFloor - bots(index).depth;
bots(index).Zs = [bots(index).Zs; bots(index).Z];
end




    
    


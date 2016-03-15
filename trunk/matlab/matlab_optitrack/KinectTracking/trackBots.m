function trackBots(imgColor, imgDepth, index)
global botArray
global MINIDRONE
global CREATE2
global camDistToFloor

% get pixels in bouding box of bot
depthFrame = getPixelsInBB(imgDepth, botArray(index).BBox);
frame = getPixelsInBB(imgColor, botArray(index).BBox);
depth= 0;
centers = [];
radii = [];
metrics = [];

if botArray(index).type == MINIDRONE
    % get depth and rmin, rmax
    depth = findDepth(depthFrame);
    [rmin, rmax] = findRadiusRange(depth);
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
elseif botArray(index).type == CREATE2
    rmin = 25;
    rmax = 35;
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.96);
end

% if not found, add current value to accum values and return
if isempty(centers)
    [botArray(index).color, ' bot not found']
    botArray(index).centers = [botArray(index).centers; botArray(index).center];
    botArray(index).depths = [botArray(index).depths, botArray(index).depth];
    botArray(index).radii = [botArray(index).radii, botArray(index).radius];
    botArray(index).yaws = [botArray(index).yaws, botArray(index).yaw];
    return
end

% keep strongest circle, put back in original coordinates
[~, indexCircle] = max(metrics);
% add center to botArray, add BBox to get back in whole image px coord
botArray(index).center(1,1) = centers(indexCircle,1) + max([botArray(index).BBox(1),1]);
botArray(index).center(1,2) = centers(indexCircle,2) + max([botArray(index).BBox(2),1]);
botArray(index).centers = [botArray(index).centers; botArray(index).center];

% add radius to bot array
botArray(index).radius = radii(indexCircle,:);
botArray(index).radii = [botArray(index).radii, radii(indexCircle,:)];

% add depth found if minidrone, add dist to floor if create, find yaws
if botArray(index).type == MINIDRONE
    botArray(index).depth = depth;
    botArray(index).yaw = findMiniDroneYaw(imgColor,  botArray(index).BBox,...
        botArray(index).yaw, botArray(index).center, radii(indexCircle,:), MINIDRONE);
    %botArray(index).yaw = 0;
elseif botArray(index).type == CREATE2
    botArray(index).depth = camDistToFloor;
    botArray(index).yaw = findMiniDroneYaw(imgColor, botArray(index).BBox, ...
        botArray(index).yaw, botArray(index).center, radii(indexCircle,:), CREATE2);
end

% find bbox
botArray(index).BBox = getBBox(botArray(index).center, botArray(index).radius);

% add accumulated values
botArray(index).yaws = [botArray(index).yaws, botArray(index).yaw];
botArray(index).depths = [botArray(index).depths, depth];
end




    
    


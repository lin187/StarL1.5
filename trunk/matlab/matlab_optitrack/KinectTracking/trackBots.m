function trackBots(imgColor, imgDepth, index)
global botArray
global MINIDRONE
global CREATE2
global camDistToFloor

% get pixels in bouding box of bot
depthFrame = getPixelsInBB(imgDepth, botArray(index).BBox);
frame = getPixelsInBB(imgColor, botArray(index).BBox);


if botArray(index).type == MINIDRONE
    % get depth and rmin, rmax
    depth = findDepth(depthFrame);
    [rmin, rmax] = findRadiusRange(depth);
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
    
    % if not found, return
    if isempty(centers)
        [botArray(index).color, ' bot not found']
        return
    end
    
    % keep strongest circle, put back in original coordinates
    [~, indexCircle] = max(metrics);
    botArray(index).center(1,1) = centers(indexCircle,1) + max([botArray(index).BBox(1),1]);
    botArray(index).center(1,2) = centers(indexCircle,2) + max([botArray(index).BBox(2),1]);
    botArray(index).centers = [botArray(index).centers; centers(indexCircle,:)];
    botArray(index).radius = radii(indexCircle,:);
    botArray(index).radii = [botArray(index).radii, radii(indexCircle,:)];
    botArray(index).depth = depth;
    botArray(index).depths = [botArray(index).depths, depth];
    botArray(index).BBox = getBBox(botArray(index).center, botArray(index).radius);
    botArray(index).yaw = 0;
    
elseif botArray(index).type == CREATE2
    rmin = 25;
    rmax = 35;
    % find circles
    [centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.96);
    
    % if not found, return
    if isempty(centers)
        [botArray(index).color, ' bot not found']
        return
    end
    
     % keep strongest circle, put back in original coordinates
    [~, indexCircle] = max(metrics);
    botArray(index).center(1,1) = centers(indexCircle,1) + max([botArray(index).BBox(1),1]);
    botArray(index).center(1,2) = centers(indexCircle,2) + max([botArray(index).BBox(2),1]);
    botArray(index).centers = [botArray(index).centers; centers(indexCircle,:)];
    botArray(index).radius = radii(indexCircle,:);
    botArray(index).radii = [botArray(index).radii, radii(indexCircle,:)];
    botArray(index).depth = camDistToFloor;
    botArray(index).BBox = getBBox(botArray(index).center, botArray(index).radius);
    botArray(index).yaw = findCreateYaw(imgColor, botArray(index).BBox, botArray(index).yaw, centers(indexCircle,:), radii(indexCircle,:));
    botArray(index).yaws = [botArray(index).yaws, botArray(index).yaw];
end




    
    


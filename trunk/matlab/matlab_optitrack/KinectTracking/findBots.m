function found = findBots(imgColor, imgDepth)
global numDrones
global numCreates
global botArray
global MINIDRONE
global CREATE2

% find any drones
if numDrones > 0
    % find depth
    depth = findDepth(imgDepth);
    % find circles
    [rmin, rmax] = findRadiusRange(depth);
    [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
    
    % if not enough circles found, return
    if length(radii) < numDrones
        found = false;
        return
    end
    
    
    for i = 1:numDrones
        [~, index] = max(metrics);
        botArray(i).center = centers(index,:);
        botArray(i).radius = radii(index);
        botArray(i).BBox = getBBox(centers(index,:), radii(index));
        botArray(i).color = getColor(imgColor, centers(index,:));
        botArray(i).type = MINIDRONE;
        botArray(i).yaw = 0;
        % remove selected circle
        centers(index,:) = [];
        radii(index) = [];
        metrics(index) = [];
    end
end

% find any creates
if numCreates > 0
    rmin = 25;
    rmax = 35;
    [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
    
    % if not enough circles found, return
    if length(radii) < numCreates
        found = false;
        return
    end
    
    % add position information from strongest circles to botArray
    for i = numDrones + 1 : numCreates + numDrones
        [~, index] = max(metrics);
        botArray(i).center = centers(index,:);
        botArray(i).radius = radii(index);
        botArray(i).BBox = getBBox(centers(index,:), radii(index));
        botArray(i).color = getColor(imgColor, centers(index,:));
        botArray(i).type = CREATE2;
        botArray(i).yaw = 0;
        % remove selected circle
        centers(index,:) = [];
        radii(index) = [];
        metrics(index) = [];
    end     
end

found = true;

    
function found = findBotsNateTest(imgColor, imgDepth)
global numDrones
global numARDrones
global numCreates
global num3DRDrones
global numGhostDrones
global numMavicDrones
global numPhant3Drones
global numPhant4Drones
global botArray
global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4
numBots = numCreates + numDrones + numARDrones + num3DRDrones + numGhostDrones + numMavicDrones + numPhant3Drones + numPhant4Drones;
% this function calls imfindcircles on whole image to find all robots
% if not enough circles found, it returns false

% The sorting and identification of robots occurs in order of increasing radius
% Robots with smallest search circles are found first

% Start searching for drones first because they have the smallest radii
if numBots - numCreates > 0
    % The drones are searched for in the order:  ARDrone2.0, GhostDrone2.0,
    % minidrone, Phantom3, Phantom4, Mavic Pro
    
    % the depth is made constant because all of the robots should be at ground
    % level. The value is chosen to activate a special setting in
    % findRadiusRange.m
    depth = 4900;
    
    % find GhostDrone2.0
    if numGhostDrones > 0
        % find the radius range
        [rmin, rmax] = findRadiusRange(depth, GHOST2);
        
        % find the circles
        [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
        
        % if not enough circle were found to make up the specified number
        % of GhostDrone2.0's then the function needs to try again
        if length(radii) < numGhostDrones*4 
            found = false;
            disp('Not enough circles found for GhostDrone2.0');
            return
        end
        
%         figure();
%         image(imgColor);
%         hold on
%         viscircles(centers, radii);
%         hold off

        % sort for the smallest circles. The GhostDrones will have slightly
        % smaller circles than the ARDrones
        [radii_sorted, I] = sort(radii);
        centers_sorted = [];
        for k = 1:length(radii)
            centers_sorted(k,:) = centers(I(k),:);
        end
        radii_sorted_cut = zeros((length(radii)-numARDrones*4),1);
        centers_sorted_cut = zeros((length(radii)-numARDrones*4),2);
        for i = 1:(length(radii)-numARDrones*4)
            radii_sorted_cut(i) = radii_sorted(i);
            centers_sorted_cut(i,:) = centers_sorted(i,:);
        end

        % make sure the circles match up to a GhostDrone and then add them
        % to the botArray
        % make array to track which circles have been used
        circlesUsed = ones(1,length(radii_sorted_cut));
        i = 1; % because the GhostDrone is the first to be searched for, this is 1
        index = 1;
        while any(circlesUsed) && index <= length(radii_sorted_cut) && i <= numGhostDrones
            index = 1;
            % get the first unused circle
            while ~circlesUsed(index)
                index = index + 1;
            end
            % determine if the circle belongs to an ARDrone
            isGhost = isGhostDrone(index, centers_sorted_cut, rmin, rmax);
            if length(isGhost) == 4
                % this is an ARDrone
                % find center by finding mean of 4 circles
                GhostCenters = [centers_sorted_cut(isGhost(1),:);centers_sorted_cut(isGhost(2),:); ...
                    centers_sorted_cut(isGhost(3),:); centers_sorted_cut(isGhost(4),:)];
                % find an average radius value
                GhostRadii = [radii_sorted_cut(isGhost(1)), radii_sorted_cut(isGhost(2)), ... 
                    radii_sorted_cut(isGhost(3)), radii_sorted_cut(isGhost(4))];
                botArray(i).center = mean(GhostCenters);
                botArray(i).radius = mean(GhostRadii);
                botArray(i).type = GHOST2;
                botArray(i).BBox = getBBox(botArray(i).center, botArray(i).radius, GHOST2);
                botArray(i).color = getColor(imgColor, botArray(i).center);
                botArray(i).yaw = 0;
                botArray(i).hyst = 0;
                i = i + 1;
            else 
                'error: circle does not belong to GhostDrone'
%                 figure();
%                 image(imgColor)
%                 hold on
%                 viscircles(centers_sorted_cut, radii_sorted_cut);
%                 hold off
%                 found = false;
%                 return 
            end
            % make circles that were used be zero
            for j = 1:length(isGhost)
                circlesUsed(isGhost(j)) = 0;
            end
        end
        % A line useful for debugging displays how many of the drones were
        % found
        s = sprintf('Found %i of %i GhostDrone2.0 drones',(i-1),numGhostDrones);
        disp(s);
        
        % if the function did not find all of the GhostDrones, it should
        % return not found
        if (i-1) ~= numGhostDrones
            found = false;
            return
        end
    end
    
    % find ARDrone2.0
    if numARDrones > 0
        % find the radius range
        [rmin, rmax] = findRadiusRange(depth, ARDRONE);
        
        % find the circles
        [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
        
        % if not enough circle were found to make up the specified number
        % of ARDrone2.0's then the function needs to try again
        if length(radii) < numARDrones*4 
            found = false;
            return
        end
        
        % make sure the circles match up to a GhostDrone and then add them
        % to the botArray
        % make array to track which circles have been used
        circlesUsed = ones(1,length(radii));
        i = numGhostDrones + 1; % because the GhostDrone is the first to be searched for, ARDrones must start 1 after that
        index = 1;
        while any(circlesUsed) && index <= length(radii) && i <= (numGhostDrones + numARDrones)
            index = 1;
            % get the first unused circle
            while circlesUsed(index) == 0
                index = index + 1;
            end
            % determine if the circle belongs to an ARDrone
            isAR = isARDrone(index, centers);
            if length(isAR) == 4
                % this is an ARDrone
                % find center by finding mean of 4 circles
                ARCenters = [centers(isAR(1),:);centers(isAR(2),:); ...
                    centers(isAR(3),:); centers(isAR(4),:)];
                % find an average radius value
                ARRadii = [radii(isAR(1)), radii(isAR(2)), ... 
                    radii(isAR(3)), radii(isAR(4))];
                botArray(i).center = mean(ARCenters);
                botArray(i).radius = mean(ARRadii);
                botArray(i).type = ARDRONE;
                botArray(i).BBox = getBBox(botArray(i).center, botArray(i).radius, ARDRONE);
                botArray(i).color = getColor(imgColor, botArray(i).center);
                botArray(i).yaw = 0;
                botArray(i).hyst = 0;
                i = i + 1;
            else 
                'error: circle does not belong to ARDrone'
%                 found = false;
%                 return 
            end
            % make circles that were used be zero
            for j = 1:length(isAR)
                circlesUsed(isAR(j)) = 0;
            end
        end
        % A line useful for debugging displays how many of the drones were
        % found
        s = sprintf('Found %i of %i ARDrone2.0 drones',(i-numGhostDrones-1),numARDrones);
        disp(s);
        
        % if the function did not find all of the AR drones, it should
        % return not found
        if (i-numGhostDrones-1) ~= numARDrones
            found = false;
            return
        end 
    end
    
    % find minidrones
    if numDrones > 0
        % find the radius range
        [rmin, rmax] = findRadiusRange(depth, MINIDRONE);
        
        % find the circles
        [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
        
        % if not enough circle were found to make up the specified number
        % of minidrones then the function needs to try again
        if length(radii) < numDrones 
            found = false;
            return
        end
        
%         figure(3);
%         image(imgColor);
%         hold on
%         viscircles(centers, radii);
%         hold off
        % sort by size, the minidrone circles will be smaller
        [radii_sorted, I] = sort(radii);
        centers_sorted = [];
        for k = 1:length(radii)
            centers_sorted(k,:) = centers(I(k),:);
        end

        % add the best matching circles to the botArray as minidrones
        index = 1;
        i = 1 + numGhostDrones + numARDrones;
        while index <= length(radii_sorted) && i <= (numGhostDrones + numARDrones + numDrones)
            % make sure the circle hasn't been used for one of the previous
            % drones
            j = 1;
            can_use = 1;
            while j <= numGhostDrones + numARDrones && numGhostDrones + numARDrones > 0
                dist = sqrt((centers_sorted(index,1) - botArray(j).center(1))^2 + (centers_sorted(index,2) - botArray(j).center(2))^2);
                if dist <= botArray(j).radius*2
                    can_use = 0;
                    break;
                end
                j = j + 1;
            end
            if can_use ~= 0
                botArray(i).center = centers_sorted(index,:);
                botArray(i).radius = radii_sorted(index);
                botArray(i).BBox = getBBox(centers_sorted(index,:), radii_sorted(index), MINIDRONE);
                botArray(i).color = getColor(imgColor, centers_sorted(index,:));
                botArray(i).type = MINIDRONE;
                botArray(i).yaw = 0;
                botArray(i).hyst = 0;
                i = i + 1;
            end
            % Move to the next circle
            index = index + 1;
        end
        % A line useful for debugging displays how many of the drones were
        % found
        s = sprintf('Found %i of %i minidrones',(i-1-numGhostDrones-numARDrones),numDrones);
        disp(s);
        
        % if the function did not find all of the minidrones, it should
        % return not found
        if (i-1-numGhostDrones-numARDrones) ~= numDrones
            found = false;
            return
        end 
    end
    
    % find Phantom3 drones
    
    
    % find Phantom4 drones
    if numPhant4Drones > 0
        % find the radius range
        [rmin, rmax] = findRadiusRange(depth, PHANTOM4);
        
        % find the circles
        [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
        
        % if not enough circles were found to make up the specified number
        % of Phantom 4's then the function needs to try again
        if length(radii) < numPhant4Drones
            disp('not enough circles found')
            found = false;
            return
        end
        
%         figure();
%         image(imgColor);
%         hold on
%         viscircles(centers, radii);
%         hold off
        % add position information from strongest circles to botArray
        i = numDrones + numARDrones + num3DRDrones + numGhostDrones + numPhant3Drones + 1;
        index = 1;
        while i <= numDrones + numARDrones + num3DRDrones + numGhostDrones + numPhant3Drones + numPhant4Drones && index <= length(radii)
            [~, index] = max(metrics);
            botArray(i).center = centers(index,:);
            botArray(i).radius = radii(index);
            botArray(i).BBox = getBBox(centers(index,:), radii(index), PHANTOM4);
            botArray(i).color = getColor(imgColor, centers(index,:));
            botArray(i).type = PHANTOM4;
            botArray(i).yaw = 0;
            botArray(i).hyst = 0;
            % remove selected circle
            centers(index,:) = [];
            radii(index) = [];
            metrics(index) = [];
            i = i + 1;
        end 

        % A line useful for debugging displays how many of the drones were
        % found
        s = sprintf('Found %i of %i Phantom4 Drones',...
            (i-1-(numDrones + numARDrones + num3DRDrones + numGhostDrones + numPhant3Drones)),numPhant4Drones);
        disp(s);

        % if the function did not find all of the minidrones, it should
        % return not found
        if (i-1-(numDrones + numARDrones + num3DRDrones + numGhostDrones + numPhant3Drones)) ~= numPhant4Drones
            found = false;
            return
        end
    end
    
    
    %find Mavic Pro drones
end

% find any creates
if numCreates > 0
    [rmin, rmax] = findRadiusRange(depth, CREATE2);
    [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
        'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
    
    % if not enough circles found, return
    if length(radii) < numCreates
        found = false;
        return
    end
    
%     figure();
%     image(imgColor);
%     hold on
%     viscircles(centers, radii);
%     hold off

    % add position information from strongest circles to botArray
    i = numBots - numCreates + 1;
    index = 1;
    while i <= numBots && index <= length(radii)
        [~, index] = max(metrics);
        botArray(i).center = centers(index,:);
        botArray(i).radius = radii(index);
        botArray(i).BBox = getBBox(centers(index,:), radii(index), CREATE2);
        botArray(i).color = getColor(imgColor, centers(index,:));
        botArray(i).type = CREATE2;
        botArray(i).yaw = 0;
        botArray(i).hyst = 0;
        % remove selected circle
        centers(index,:) = [];
        radii(index) = [];
        metrics(index) = [];
        i = i + 1;
    end 
    
    % A line useful for debugging displays how many of the drones were
    % found
    s = sprintf('Found %i of %i Creates',(i-1-numBots+numCreates),numCreates);
    disp(s);

    % if the function did not find all of the minidrones, it should
    % return not found
    if (i-1-numBots+numCreates) ~= numCreates
        found = false;
        return
    end
end

found = true;

% if numBots == 2
%     if (botArray(1).color == 'w' || botArray(2).color == 'w')
%         found = false;
%     end
% end

% this checks to make sure only bot is white
% the first frame is sometimes very bright, so findBots needs to be called
% again on a better frame
% num_whites = 0;
% for i = 1:numBots
%     if botArray(i).color == 'w'
%         num_whites = num_whites + 1;
%     end
% end
% 
% if num_whites > 1
%     found = false;
% end
%     
function [found, bots] = findBots(imgColor, numDrones, numCreates, ...
        numARDrones, num3DRDrones, numGhostDrones, numMavicDrones, ...
        numPhant3Drones, numPhant4Drones, cameraLocation)
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose: Find all the bots in their initial positions by calling 
% imfindcircles on the whole image to find all robots.
% if not enough circles are found, it returns false

global camDistToFloor
global BBoxFactor
global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4
numBots = numCreates + numDrones + numARDrones + num3DRDrones + numGhostDrones + numMavicDrones + numPhant3Drones + numPhant4Drones;

% The sorting and identification of robots occurs in order of increasing radius
% Robots with smallest search circles are found first

%% Create an empty list to store the robots that are found
bots = Robot.empty(numBots,0);
for i = 1:numBots
    bots(i) = Robot;
end

%% Start searching for drones first because they have the smallest radii
if numBots - numCreates > 0
    % The drones are searched for in the order:  ARDrone2.0, GhostDrone2.0,
    % minidrone, Phantom3, Phantom4, Mavic Pro
    
    
    %% find GhostDrone2.0
    if numGhostDrones > 0
        % find the radius range
        [rmin, rmax] = findRadiusRange(depth, GHOST2); %CHANGE THIS!!!
        
        % find the circles
        [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
        
        % if not enough circle were found to make up the specified number
        % of GhostDrone2.0's then the function needs to try again
        if length(radii) < numGhostDrones*4 
            found = false;
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
        % to the bots
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
                bots(i).center = mean(GhostCenters);
                bots(i).radius = mean(GhostRadii);
                bots(i).type = GHOST2;
                bots(i).BBox = getBBox(bots(i).center, bots(i).radius, GHOST2, BBoxFactor);
                bots(i).color = getColor(imgColor, bots(i).center);
                centerMM = getMMCoord(cameraLocation, bots(i).center, bots(i).radius, bots(i).type);
                bots(i).X = centerMM(1,1);
                bots(i).Y = centerMM(1,2);
                bots(i).Z = camDistToFloor;
                bots(i).yaw = 0;
                bots(i).hyst = 0;
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
    
    %% find ARDrone2.0
    if numARDrones > 0
        % find the radius range
        [rmin, rmax] = findRadiusRange(depth, ARDRONE); %CHANGE THIS!!!!!!!
        
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
        % to the bots
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
                bots(i).center = mean(ARCenters);
                bots(i).radius = mean(ARRadii);
                bots(i).type = ARDRONE;
                bots(i).BBox = getBBox(bots(i).center, bots(i).radius, ARDRONE, BBoxFactor);
                bots(i).color = getColor(imgColor, bots(i).center);
                centerMM = getMMCoord(cameraLocation, bots(i).center, bots(i).radius, bots(i).type);
                bots(i).X = centerMM(1,1);
                bots(i).Y = centerMM(1,2);
                bots(i).Z = camDistToFloor;
                bots(i).yaw = 0;
                bots(i).hyst = 0;
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
    
    %% find minidrones
    if numDrones > 0
        disp('I am looking for minidrones')
        % the average radius on the ground is 47.8 so use values right
        % around that for min and max
        rmin = 40;
        rmax = 55;
        
        % find the circles
        [centers, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
            'ObjectPolarity', 'dark', 'Sensitivity', 0.92);
        
        % if not enough circle were found to make up the specified number
        % of minidrones then the function needs to try again
        if length(radii) < numDrones 
            found = false;
            disp('Not enough circles found')
            figure(4);
            image(imgColor);
            hold on
            viscircles(centers, radii);
            hold off
            pause(1)
            return
        end
        
        figure(4);
        image(imgColor);
        hold on
        viscircles(centers, radii);
        hold off
        pause(1)
        
        % sort by size, the minidrone circles will be smaller
        [radii_sorted, I] = sort(radii);
        centers_sorted = [];
        for k = 1:length(radii)
            centers_sorted(k,:) = centers(I(k),:);
        end


        % add the best matching circles to the bots as minidrones
        index = 1;
        i = 1 + numGhostDrones + numARDrones;
        while index <= length(radii_sorted) && i <= (numGhostDrones + numARDrones + numDrones)
            % make sure the circle hasn't been used for one of the previous
            % drones
%             disp('Im in the while loop')
            j = 1;
            can_use = 1;
            while j <= numGhostDrones + numARDrones && numGhostDrones + numARDrones > 0
                dist = sqrt((centers_sorted(index,1) - bots(j).center(1))^2 + (centers_sorted(index,2) - bots(j).center(2))^2);
                if dist <= bots(j).radius*2
                    can_use = 0;
                    break;
                end
                j = j + 1;
            end
            if can_use ~= 0
%                 disp('im in the if')
                bots(i).center = centers_sorted(index,:);
                bots(i).radius = radii_sorted(index);
                bots(i).BBox = getBBox(centers_sorted(index,:), radii_sorted(index), MINIDRONE, BBoxFactor);
                bots(i).color = getColor(imgColor, centers_sorted(index,:));
                bots(i).type = MINIDRONE;
                centerMM = getMMCoord(cameraLocation, bots(i).center, bots(i).radius, bots(i).type);
                bots(i).X = centerMM(1,1);
                bots(i).Y = centerMM(1,2);
                bots(i).Z = camDistToFloor;
                bots(i).yaw = 0;
                bots(i).hyst = 0;
                i = i + 1
            end
            % remove selected circle
            centers_sorted(index,:) = [];
            radii_sorted(index) = [];
%             index = index + 1
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
    
    %% find Phantom3 drones
    
    
    %% find Phantom4 drones
    
    
    %% find Mavic Pro drones
end

%% find any creates aka roombas
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

    % add position information from strongest circles to bots
    i = numBots - numCreates + 1;
    while i <= numBots && index < length(radii)
        [~, index] = max(metrics);
        bots(i).center = centers(index,:);
        bots(i).radius = radii(index);
        bots(i).BBox = getBBox(centers(index,:), radii(index), CREATE2, BBoxFactor);
        bots(i).color = getColor(imgColor, centers(index,:));
        bots(i).type = CREATE2;
        centerMM = getMMCoord(cameraLocation, bots(i).center, bots(i).radius, bots(i).type);
        bots(i).X = centerMM(1,1);
        bots(i).Y = centerMM(1,2);
        bots(i).Z = camDistToFloor;
        bots(i).yaw = 0;
        bots(i).hyst = 0;
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

    % if the function did not find all of the creates, it should
    % return not found
    if (i-1-numBots+numCreates) ~= numCreates
        found = false;
        return
    end
end

found = true;

return;


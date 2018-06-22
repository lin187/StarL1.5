function [ incomingList ] = find_crossings(bots)
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose: Identify bots that might be about to cross from one Camera's
% field of view to another.

%% Globals and variable initializations
global camera_locations;
global camDistToFloor;
global numCameras;

% Reset the incoming lists to start fresh
incomingList = strings(numCameras,1);

%% Parse through all of the bots
% Check to see which ones have a hysteresis value greater than 1. 
% If the bot hasn't been found twice, then it has likely crossed some
% boundary.
for i = 1:length(bots)
    if bots(i).hyst > 1
        % Calculate which two Cameras are the closest (one should be the
        % Camera it is currently assigned to)
        x = bots(i).X;
        y = bots(i).Y;
        z = bots(i).Z;
        closestDist = 100000000;
        closestCamera = 0;
        closestDist2 = 1000000000;
        closestCamera2 = 0;
        for j = 1:numCameras
            cameraX = camera_locations(j,1);
            cameraY = camera_locations(j,2);
            cameraZ = camDistToFloor;
            dist = sqrt((cameraX-x)^2 + (cameraY-y)^2 + (cameraZ-z)^2);
            if dist < closestDist2
                if dist < closestDist
                    closestDist2 = closestDist;
                    closestCamera2 = closestCamera;
                    closestDist = dist;
                    closestCamera = j;
                else
                    closestDist2 = dist;
                    closestCamera2 = j;
                end
            end
        end
        
        % Add them to the list of each Camera as described in the
        % documentation
        incomingList(closestCamera) = strcat(incomingList(closestCamera),...
            num2str(i), ',');
        if closestCamera2 > 0
            incomingList(closestCamera2) = strcat(incomingList(closestCamera2),...
                num2str(i), ',');
        end
    end
end

end


function check_incoming(incomingList, cameraNum, imgColor)
% Author: Nate Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%  
% Purpose: To check fringes for possible crossing over of robots from
% outside the field of view to inside the field of view.

global bots
global bot_lists
factor = 3.0;

%% Parse the incoming list
botList = str2num(char(incomingList));

%% Search the image for incomming robots and update lists
for i = botList
    prevCameraNum = bots(i).cameraNum;
    
    % Determine what pixel location the robot will be at
    [center, radius] = getPixelCoord(cameraNum, i, bots(i).X, bots(i).Y, bots(i).Z);
    
    % Capture a space around the robot and search it for the specified
    % robot
    bots(i).BBox = getBBox(center, radius, bots(i).type, factor);
    s = sprintf('Attempting to find lost drone %i',i);
    disp(s)
    trackBots(imgColor, i, cameraNum);
    
    % If the robot was found, then the corresponding bot_lists need to be
    % updated
    if bots(i).hyst == 0
        figure(3);
        frame = getPixelsInColorBB(imgColor, bots(i).BBox);
        image(frame);
        
        % Remove the number from the previous Camera's list
        bot_lists(prevCameraNum) = strrep(bot_lists(prevCameraNum), ...
            num2str(i), '');
        % Remove any double commas
        bot_lists(prevCameraNum) = strrep(bot_lists(prevCameraNum), ...
            ',,', ',');
        
        % Add the robot number to the new list where it was found
        bot_lists(cameraNum) = strcat(bot_lists(cameraNum), ',', num2str(i));
    end
end

end


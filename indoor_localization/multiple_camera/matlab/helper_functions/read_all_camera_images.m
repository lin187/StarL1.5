function [ imgColor ] = read_all_camera_images( numCameras )
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose: This function reads the newest available image from each Camera
%          in numerical order.

%% Declare global variables
global camera_number
global camera_locations
global colorMsgs
global imgColorAll
global frameCount

% %% Initialize variables
% imgColor = zeros(480,640,3,numCameras,'uint8');

%% Read the images
% Set the camera_number to 1 so the associated callback function won't try
% to overwrite the image as it is being read
camera_number = 1;

while camera_number <= numCameras
    % Read the next image
    imgColor(:,:,:,camera_number) = imrotate(readImage(colorMsgs(camera_number)),90*(camera_locations(camera_number,3)-1));
%     imgColorAll(camera_number,frameCount,:,:,:) = imgColor(camera_number,:,:,:); % THIS NEEDS FIXING!!!!!!!!!!!!!!!!!!!!!!!!!
    % Increase the camera_number so that the next image read is not
    % overwrtitten in its associated callback function
    camera_number = camera_number + 1;
end

% After all the images have been read, set the camera_number back to 0 so
% that all of the callback functions will resume their previous behavior
camera_number = 0;

end
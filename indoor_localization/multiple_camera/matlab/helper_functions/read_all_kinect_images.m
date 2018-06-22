function [ imgColor, imgDepth ] = read_all_kinect_images( numKinects )
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose: This function reads the newest available image from each Kinect
%          in numerical order.

%% Declare global variables
global kinect_number
global kinect_locations
global colorMsgs
global depthMsgs
global imgColorAll

% %% Initialize variables
% imgColor = zeros(480,640,3,numKinects,'uint8');
% imgDepth = zeros(480,640,3,numKinects,'uint8');

%% Read the images
% Set the kinect_number to 1 so the associated callback function won't try
% to overwrite the image as it is being read
kinect_number = 1;

while kinect_number <= numKinects
    % Read the next image
    imgColor(kinect_number) = imrotate(readImage(colorMsgs(kinect_number)),90*(kinect_locations(kinect_number,3)-1));
    imgDepth(kinect_number) = imrotate(readImage(depthMsgs(kinect_number)),90*(kinect_locations(kinect_number,3)-1));
    imgColorAll(:,:,:,kinect_number,frameCount) = imgColor(kinect_number);
    % Increase the kinect_number so that the next image read is not
    % overwrtitten in its associated callback function
    kinect_number = kinect_number + 1;
end

% After all the images have been read, set the kinect_number back to 0 so
% that all of the callback functions will resume their previous behavior
kinect_number = 0;

end
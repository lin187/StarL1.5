function out = getMMCoord(kinectLocation, coordinates, radius, type)
% Author: Nate Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%  
% Purpose: This function converts location from pixel coordinates to mm 
% coordinates
global mm_per_pixel

%% Gather relevant information
% these are the center pixel values of the image. If using a camera with
% different resolution than 640x480, this will need to be changed. If (0,0)
% is the center of the image, use them. If the corner is (0,0) then use 0's
xCenterPx = 1920/2;
yCenterPx = 1080/2;
% xCenterPx = 0;
% yCenterPx = 0;

xCenterMM = kinectLocation(1);
yCenterMM = kinectLocation(2);

x = coordinates(1,1);
y = coordinates(1,2);

%% Get a millimeter per pixel value
% If drone call mmPerPixel, if iRobot use constant value
if isAerialDrone(type) == 1
    mmpp = mmPerPixel(radius, type);
elseif isGroundRobot(type) == 1
    mmpp = mm_per_pixel;
end

%% Calculate the millimeter coordinate
out(1,1) = xCenterMM + (x - xCenterPx) * mmpp;
out(1,2) = yCenterMM + (y - yCenterPx) * mmpp;
end
function out = getMMCoord(coordinates, radius, type)
global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4
global mm_per_pixel
% this function converts from pixel coordinates to mm coordinates

xCenterMM = 0;
yCenterMM = 0;
% these are the center pixel value of the image. If using a camera with
% different resolution than 640x480, this will need to be changed.
xCenterPx = 320;
yCenterPx = 240;

x = coordinates(1,1);
y = coordinates(1,2);

% get a millimeter per pixel value. If drone call mmPerPixel, if iRobot use
% constant value
if isAerialDrone(type) == 1
    mmpp = mmPerPixel(radius, type);
elseif isGroundRobot(type) == 1
    mmpp = mm_per_pixel;
end
    
out(1,1) = xCenterMM + (x - xCenterPx) * mmpp;
out(1,2) = yCenterMM + (y - yCenterPx) * mmpp;

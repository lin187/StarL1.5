function out = mmPerPixel(radius, type)
% this function gives the number of mm in one pixel for the drones
% the radius of the minidrone is ~115 mm
% thre radius of one of the ARDrone circles is ~125 mm
global MINIDRONE
global ARDRONE
if type == MINIDRONE
    out = 115/radius;
elseif type == ARDRONE
    out = 125/radius;
end
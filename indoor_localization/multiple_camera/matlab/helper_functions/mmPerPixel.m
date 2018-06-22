function out = mmPerPixel(radius, type)
% this function gives the number of mm in one pixel for the drones
% the radius of the minidrone is ~115mm
% the radius of one of the ARDrone circles is ~125mm
% the radius of the 3DR drone circle is ~_mm
% the radius of one of the GhostDrone2.0 circles is ~120mm
% the radius of the Mavic Pro drone circle is ~_mm
% the radius of the Phantom3 drone circle is ~_mm
% the radius of the Phantom4 drone circle is ~335mm
global MINIDRONE
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4
if type == MINIDRONE
    out = 115/radius;
elseif type == ARDRONE
    out = 125/radius;
elseif type == THREEDR
    out = 125/radius;
elseif type == GHOST2
    out = 120/radius;
elseif type == MAVICPRO
    out = 125/radius;
elseif type == PHANTOM3
    out = 125/radius;
elseif type == PHANTOM4
    out = 335/radius;
end
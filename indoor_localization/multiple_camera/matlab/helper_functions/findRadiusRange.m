function [rmin, rmax] = findRadiusRange(BBoxWidth, type, BBoxFactor)
% The purpose of this function is to calculate and return the radius range of the desired type of robot in terms of pixels

global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4

% If the drone is a single-circle drone
if (type == MINIDRONE)
    % The maximum radius cannot be larger than half the width and the
    % shouldn't be smaller than half the last recorded value
    maxRadius = ceil(BBoxWidth/2);
    lastRadius = floor(maxRadius/BBoxFactor);
    minRadius = round(lastRadius/2);
    
    rmin = minRadius;
    rmax = maxRadius;

% If the drone is a 4-circle drone
elseif ((type == ARDRONE) || (type == GHOST2))
	% The maximum radius cannot be larger than 1/4 tge width and shouldn't 
    % be smaller than half the last recorded value 
    maxRadius = ceil(BBoxWidth/4);
    lastRadius = floor(maxRadius/BBoxFactor);
    minRadius = round(lastRadius/2);
    
    rmin = minRadius;
    rmax = maxRadius;

% If it's a CREATE2
elseif (type == CREATE2)
	% These values are hard coded and shouldn't change BUT THESE NEED TO BE
	% UPDATED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	rmin = 25;
    rmax = 35;

% If the type is unknown	
elseif (type == THREEDR)
	% TODO: Make this more specific to the THREEDR
	rmin = 0;
    rmax = 0;
	
elseif (type == MAVICPRO)
	% TODO: Make this more specific to the MAVICPRO
	rmin = 0;
    rmax = 0;
	
elseif (type == PHANTOM3)
	% TODO: Make this more specific to the PHANTOM3
	rmin = 0;
    rmax = 0;
	
elseif (type == PHANTOM4)
	% TODO: Make this more specific to the PHANTOM4
	rmin = 0;
    rmax = 0;
	
else
	disp('No valid type of drone entered in findRadiusRange');
	rmin = 0;
    rmax = 0;
	
end
end

function [ ground_boundaries air_boundaries ] = establish_boundaries( cameraLocations )
% Author: Nate Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%  
% Purpose: The purpose of this function is to establish the boundaries of
% each Camera's field of view based on the Camera's location. The ground
% boundaries are the entire field of view. The air boundaries are the
% bounds at which the Cameras' field of view overlap.
%
% TODO: make the boundary a function based on height. Might be useful
% later.

global numCameras

% These constants have to be changed for different setups
xRadius = 318/2;
yRadius = 234/2;

[numCameras,p] = size(cameraLocations); % The ans might need to be flipped
bounds = zeros(numCameras,4);

for i = 1:numCameras
    bounds(i,1) = cameraLocations(i,1) - xRadius;
    bounds(i,2) = cameraLocations(i,1) + xRadius;
    bounds(i,3) = cameraLocations(i,2) - yRadius;
    bounds(i,4) = cameraLocations(i,2) + yRadius;
end
ground_boundaries = bounds;

%TODO: Calculate air boundaries
air_boundaries = bounds;
end


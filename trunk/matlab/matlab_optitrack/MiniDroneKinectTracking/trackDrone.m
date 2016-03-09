function [center, radius, depth, BBox] = trackDrone(frame, depthFrame, drone, BBoxFactor)

% get smaller img from bouding box
BBox = drone.BBox;
depthFrame = depthFrame(max([BBox(2),1]):min([BBox(2) + BBox(4),480]), ...
    max([BBox(1),1]):min([BBox(1) + BBox(3), 640]));
BBox(2) = max([BBox(2),1]);
BBox(1) = max([BBox(1),1]);
frame = frame(BBox(2):min([BBox(2) + BBox(4),480]), ...
    BBox(1):min([BBox(1) + BBox(3), 640]),:);

% get depth and rmin, rmax
depth = findDepth(depthFrame);
% if depth == 2800
%     depth = drone.depth;
% end
[rmin, rmax] = findRadiusRange(depth);

% find circles
[centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
    'ObjectPolarity', 'dark', 'Sensitivity', 0.92);

% if no circles found, return previous values
if isempty(centers)
    center = drone.center;
    radius = drone.radius;
    BBox = drone.BBox; % maybe make this larger so to increase change of finding in next frame
    'circle not found'
    return
end

% keep strongest circle
[~, index] = max(metrics);
center = centers(index,:);
radius = radii(index,:);

% put back in original coordinates
center(:,1) = center(:,1) + BBox(1);
center(:,2) = center(:,2) + BBox(2);
% find new bounding box

BBox = [[center(1,1) - BBoxFactor * radius, center(1,2) - BBoxFactor * radius], ...
        [2*BBoxFactor*radius, 2*BBoxFactor*radius]];
BBox = round(BBox);    
function [center, radius, BBox, yaw] = trackCreate2(frame, create2, BBoxFactor)

% get smaller img from bouding box
BBox = create2.BBox;
BBox(2) = max([BBox(2),1]);
BBox(1) = max([BBox(1),1]);
frame = frame(BBox(2):min([BBox(2) + BBox(4),480]), ...
    BBox(1):min([BBox(1) + BBox(3), 640]),:);

rmin = 25;
rmax = 35;

% find circles
[centers, radii, metrics] = imfindcircles(frame, [rmin,rmax], ...
    'ObjectPolarity', 'dark', 'Sensitivity', 0.96);

% if no circles found, return previous values
if isempty(centers)
    center = create2.center;
    radius = create2.radius;
    BBox = create2.BBox; % maybe make this larger so to increase change of finding in next frame
    yaw = create2.yaw;
    'circle not found'
    return
end

% keep strongest circle
[~, index] = max(metrics);
center = centers(index,:);
radius = radii(index,:);

% black out pixels that aren't contained in bot's circle
% this is so yaw estimation won't pick up other bot's circles when too
% close
% would be better to do this without a loop if possible
for i = 1:size(frame, 1)
    for j = 1:size(frame,2);
        if (i - center(1,1))^2 + (j - center(1,2))^2 > radius^2
            frame(j,i,:) = 0;
        end
    end
end

% find yaw using two small circles
yaw = findCreateYaw(frame, create2.yaw);

% put back in original coordinates
center(:,1) = center(:,1) + BBox(1);
center(:,2) = center(:,2) + BBox(2);

% find new bounding box

BBox = [[center(1,1) - BBoxFactor * radius, center(1,2) - BBoxFactor * radius], ...
        [2*BBoxFactor*radius, 2*BBoxFactor*radius]];
BBox = round(BBox);    
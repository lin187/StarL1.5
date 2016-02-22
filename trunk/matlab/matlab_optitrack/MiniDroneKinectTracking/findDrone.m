function [centers, radiis, depth, BBox, colors] = findDrone(imgColor, imgDepth, numDrones, BBoxFactor)
centers = [];
radiis = [];
colors = [];
color = ['r', 'g', 'b'];

% find depth
depth = findDepth(imgDepth);
% find circles
[rmin, rmax] = findRadRangeFileFolder(depth);
[center, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
    'ObjectPolarity', 'dark', 'Sensitivity', 0.92);

% if not enough circles found, return
if length(radii) < numDrones
    centers = [];
    radiis = [];
    BBox = [];
    depth = [];
    return
end

% add strongest circles to array and find BBoxes
for i = 1:numDrones
    [max_val, index] = max(metrics);
    centers = [centers; center(index,:)];
    radiis = [radiis, radii(index)];
    BBox(i,:) = [[center(index,1) - BBoxFactor * radii(index), center(index,2) - BBoxFactor * radii(index)], ...
        [2*BBoxFactor*radii(index), 2*BBoxFactor*radii(index)]];
    centerRounded = round(center(index,:));
    [v,colorIndex] = max(imgColor(centerRounded(1,2), centerRounded(1,1), :));
    colors = [colors; color(colorIndex(1))];
    center(index,:) = [];
    radii(index) = [];
    metrics(index) = [];
end
BBox = round(BBox);
    



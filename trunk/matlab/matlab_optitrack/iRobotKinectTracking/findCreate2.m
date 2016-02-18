function [centers, radiis, BBox, yaw, colors] = findCreate2(imgColor, numBots, BBoxFactor)
centers = [];
radiis = [];
colors = [];
color = ['r', 'g', 'b'];
rmin = 25;
rmax = 35;
rmin_yaw = 5;
rmax_yaw = 10;

[center, radii, metrics] = imfindcircles(imgColor, [rmin,rmax], ...
    'ObjectPolarity', 'dark', 'Sensitivity', 0.92);

% if not enough circles found, return
if length(radii) < numBots
    centers = [];
    radiis = [];
    BBox = [];
    yaw = [];
    colors = [];
    return
end

% add strongest circles to array and find BBoxes
for i = 1:numBots
    [max_val, index] = max(metrics);
    centers = [centers; center(index,:)];
    radiis = [radiis, radii(index)];
    BBox(i,:) = [[center(index,1) - BBoxFactor * radii(index), center(index,2) - BBoxFactor * radii(index)], ...
        [2*BBoxFactor*radii(index), 2*BBoxFactor*radii(index)]];
    centerRounded = round(center(index,:));
    % find bot's color
    [v,colorIndex] = max(imgColor(centerRounded(1,2), centerRounded(1,1), :));
    colors = [colors; color(colorIndex(1))];
    % find bot's yaw
    BBoxYaw(2) = max([BBox(i,2),1]);
    BBoxYaw(1) = max([BBox(i,1),1]);
    BBoxYaw(3) = BBox(i,3);
    BBoxYaw(4) = BBox(i,4);
    frame = imgColor(BBoxYaw(2):min([BBoxYaw(2) + BBoxYaw(4),480]), ...
    BBoxYaw(1):min([BBoxYaw(1) + BBoxYaw(3), 640]),:);
    yaw(i,1) = findCreateYaw(frame,0);
    center(index,:) = [];
    radii(index) = [];
    metrics(index) = [];
end
BBox = round(BBox);

    



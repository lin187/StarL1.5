function BBox = getBBoxTight(center, radius, type)

% this function returns a BBox with BBoxFactor = 1
% this currently is only used with drones made up of 4 circles
x = 4;
y = 2;

BBoxFactor = 1;
BBox = [[center(1,1) - BBoxFactor * radius*y, center(1,2) - BBoxFactor * radius*y], ...
        [x*BBoxFactor*radius, x*BBoxFactor*radius]];
BBox = round(BBox);
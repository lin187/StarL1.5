function BBox = getBBoxTight(center, radius, type)
global ARDRONE
% this function returns a BBox with BBoxFactor = 1
% this currently is only used to find ARDRONE yaw
if type == ARDRONE
    x = 4;
    y = 2;
else
    x = 2;
    y = 1;
end

BBoxFactor = 1;
BBox = [[center(1,1) - BBoxFactor * radius*y, center(1,2) - BBoxFactor * radius*y], ...
        [x*BBoxFactor*radius, x*BBoxFactor*radius]];
BBox = round(BBox);
function BBox = getBBox(center, radius)
global BBoxFactor
BBox = [[center(1,1) - BBoxFactor * radius, center(1,2) - BBoxFactor * radius], ...
        [2*BBoxFactor*radius, 2*BBoxFactor*radius]];
BBox = round(BBox);
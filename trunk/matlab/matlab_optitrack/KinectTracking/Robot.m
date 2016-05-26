classdef Robot
    properties
        center
        centers = [];
        radius
        radii = [];
        yaw
        yaws = [];
        depth;
        depths = [];
        BBox
        BBoxes = [];
        BBoxTight % BBox with BBox factor of 1
        color
        type
        hyst
    end
end
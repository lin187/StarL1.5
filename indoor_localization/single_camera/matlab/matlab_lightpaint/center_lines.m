function [lines] = center_lines(lines, CENTER_LOCATION)

% Find the min x and y coordinates
min_x = min(min(lines(:,[1 3])));
min_y = min(min(lines(:,[2 4])));

% Find current width and height
height = max(max(lines(:,[2 4]))) - min_y;
width = max(max(lines(:,[1 3]))) - min_x;

% Find the image center
centr_x = min_x + (width/2);
centr_y = min_y + (height/2);

% Calculate the difference needed to move the centroid to the img center
delta = CENTER_LOCATION - [centr_x centr_y];
deltamat_l = repmat([delta delta], size(lines,1),1);
lines = lines + deltamat_l;
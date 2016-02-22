function out = mmPerPixel(radius)
% y = 8e-06 * depth^2 - 0.0497 * depth + 99.409;
% out = 113.4/y;
%out = 0.0018 * depth + 0.1457;
out = 115/radius;
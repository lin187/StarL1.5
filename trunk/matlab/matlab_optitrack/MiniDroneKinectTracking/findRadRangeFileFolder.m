function [rmin, rmax] = findRadRangeFileFolder(depth)
r = 1.2298e-05*depth.^2 - 0.0654*depth + 112.3607;
rmin = max(floor(r-5), 1);
rmax = ceil(r+5);

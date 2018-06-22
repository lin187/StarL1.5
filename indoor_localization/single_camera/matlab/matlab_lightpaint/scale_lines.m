function [lines] = scale_lines(lines, SCALE_MAX)

l_max = max(max(lines));
l_min = min(min(lines));

scalefactor = SCALE_MAX/(l_max-l_min);

lines = lines*scalefactor;
function dist = get_line_distance(cur, other, SPACING)
% Divide the current line into points with SPACING separation
[curx cury] = subdivide_line(cur([1 3]),cur([2 4]), SPACING);
pts = [curx' cury'];
dist = inf;

loopstart = 1;
loopend = size(pts,1);

if loopend > 2
    loopstart = 2;
    loopend = loopend - 1;
end

for i=loopstart:loopend
    dist = min(dist, abs(det([other(3:4)-other(1:2);pts(i,:)-other(1:2)]))/norm(other(3:4)-other(1:2)));    
end
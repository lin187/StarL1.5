% p1 and p2 are vectors of the form [x1 y1] and [x2 y2]
function distance = p_dist(p1, p2)
distance = sqrt((p1(1)-p2(1))^2 + (p1(2)-p2(2))^2);
function [next need_ghost data closest_dist idx] = find_nearest(cur, data)
need_ghost = 0;
next = [0 0 0 0];

n_lines = size(data,1);

% Find the point thats closest to the current endpoint
closest = -1;
closest_dist = inf;
for i = 1:n_lines   
   % Check both ends of the line to see which one is closest
   dist = p_dist(cur,data(i,1:2));
   if dist < closest_dist
       closest_dist = dist;
       closest = i;  
   end

   dist = p_dist(cur,data(i,3:4));
   if dist < closest_dist
       closest_dist = dist;
       closest = i + 1i;  
   end
end
idx = real(closest);
next = data(idx,:);
data(real(closest),:) = [];
need_ghost = (closest_dist ~= 0);

% If the closest point has an imaginary component, it's the x2 y2 end of a
% line segment. This uses fftshift (I FEEL CLEVER!)
if imag(closest) ~= 0
    next = fftshift(next);
end
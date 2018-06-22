function [lines ghosts colors] = add_ghosts(data, colorin, snap, radius)
% Insert "ghost" lines connecting discontinuous line segments, creating a
% single walk through the image. This will start with line 1, walking along
% lines until the end of the last line is reached or no line connects. If
% no line connects, a ghost line is drawn to the nearest line end.
%
% Ghost lines are indicated by a 1 in the returned ghosts vector, normal
% lines are marked with a 0.
num_lines = size(data,1);
snapcount = 0;

if nargin == 1
    snap = false;
    radius = -1;
end

% Leave enough space for all possible ghost lines (no lines connected)
lines = ones(num_lines+num_lines-1,4)*-1;
ghosts = ones(num_lines+num_lines-1,1)*-1;
colors = repmat('000000' ,[num_lines+num_lines-1 1]);

lines(1,:) = data(1,:);
colors(1,:) = colorin{1};
ghosts(1) = 0;

cur_A = data(1,1:2);
cur_B = data(1,3:4);
data(1,:) = [];
colorin(1) = [];

% Determine which of them has a closer neighbor
closest_A = inf;
closest_B = inf;
for i = 1:size(data,1)
    d_A = min([p_dist(cur_A, data(i,1:2)) p_dist(cur_A, data(i,3:4))]);
    closest_A = min([d_A closest_A]);
    
    d_B = min([p_dist(cur_B, data(i,1:2)) p_dist(cur_B, data(i,3:4))]);
    closest_B = min([d_B closest_B]);
end
if closest_A < closest_B
    cur = cur_A;
    lines(1,:) = fftshift(lines(1,:));
else
    cur = cur_B;
end

i = 1;

% Iterate through each solid line
while ~isempty(data)
    i = i + 1;
    % Find the nearest line end to the current line end
    [next need_ghost data dist idx] = find_nearest(cur,data);
    
    % If the next line is closer than the snapping distance, snap the two
    % points together
    if snap && need_ghost && dist < radius
        % Set 1:2 of the next to cur
        %disp('Endpoint snapped!');
        next(1:2) = cur;
        need_ghost = 0;
    end
    
    % If the nearest solid line end is not connected to the current line,
    % add a ghost line connecting them
    if need_ghost
        lines(i,:) = [cur next(1:2)];
        lines(i+1,:) = next;
        
        colors(i,:) = '000000';
        colors(i+1,:) = colorin{idx};
        colorin(idx) = [];
        
        ghosts(i:i+1) = [1 0];
        i = i + 1;
    else 
        lines(i,:) = next;
        colors(i,:) = colorin{idx};
        ghosts(i) = 0;
    end
    
    cur = next(3:4);
end

lines(lines == -1) = [];
ghosts(ghosts == -1) = [];
lines = reshape(lines,[],4);
colors = colors(1:size(lines,1),:);

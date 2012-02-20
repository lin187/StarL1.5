function cLines = divide_lines(lines, SPACING, ints, intsA, intsB, ABSORPTION_RADIUS)
num_lines = size(lines,1);
cLines = cell(num_lines,3);
for i = 1:num_lines
    matching_idx = union(find(intsA == i), find(intsB == i));
    intsX = ints(matching_idx,1);
    intsY = ints(matching_idx,2); 
    [xi yi] = subdivide_line(lines(i,[1 3]),lines(i,[2 4]), SPACING, intsX, intsY);
    cLines(i,1) = {round(xi)};
    cLines(i,2) = {round(yi)};
    cLines(i,3) = {ones(size(xi))*-1};
end

% If any non-intersection points are within ABSORPTION_RADIUS of a
% line endpoint, remove them
for i = 1:num_lines
    % If there are at least 4 points in the line
    if size(cLines{i,1},2) > 3
        points = reshape([cLines{i,1:2}],[],2);
        % If the first endpoint and next waypoint are too close
        if (p_dist(points(1,:), points(2,:)) < ABSORPTION_RADIUS) && cLines{i,3}(2) == -1
            cLines{i,1} = cLines{i,1}([1 3:end]);
            cLines{i,2} = cLines{i,2}([1 3:end]);
            cLines{i,3} = cLines{i,3}([1 3:end]);
            %fprintf('Absorbed point on line %u\n', i);
        end
        
        % If the last endpoint and next previous waypoint are too close
        if (p_dist(points(end,:), points(end-1,:)) < ABSORPTION_RADIUS) && cLines{i,3}(end-1) == -1
            cLines{i,1} = cLines{i,1}([1:end-2 end]);
            cLines{i,2} = cLines{i,2}([1:end-2 end]);
            cLines{i,3} = cLines{i,3}([1:end-2 end]);
            %fprintf('Absorbed point on line %u\n', i);
        end
    end
end

%% Run the standard processing algorithm on each frame

% Connect nonadjacent lines with "ghost" lines when neccessary
% To find the minimal path, run this once with each line as the first line
% and record the total length.
lengths = zeros(size(lines,1),3);
for i=1:size(lines,1)
    % Run add_ghosts with line i as the first line
    reorder_lines = lines;
    reorder_lines([1 i],:) = reorder_lines([i 1],:);
    [outlines outghosts outcolor] = add_ghosts(reorder_lines, color, END_SNAPPING, END_SNAP_RADIUS);
    lengths(i,1:2) = statistics(outlines, outghosts);
    lengths(i,3) = sum(lengths(i,1:2));
end
[min_dist startline] = min(lengths(:,3));
%fprintf('Minimum path distance with starting line %u\n',startline);
lines([1 startline],:) = lines([startline 1],:);
color([1 startline],:) = color([startline 1],:);

[lines ghosts color] = add_ghosts(lines, color, END_SNAPPING, END_SNAP_RADIUS);

% Calculate intersections
[ints intsA intsB] = find_intersections(lines);

% Round the lines
% TODO: Determine if this makes any appreciable difference!
lines = round(lines);

% Break each of the lines into equal length segments
cLines = divide_lines(lines, SPACING, ints, intsA, intsB, ABSORPTION_RADIUS);

if ~isempty(find(isnan(lines),1)) || ~isempty(find(isnan(ints),1)) 
   error(scalefactor); 
end

% For each intersection point, load the two lines which intersect and apply
% a label to the matching point.
n_ints = size(ints,1);
cLines = label_intersections(cLines, ints, intsA, intsB, INTERSECTION_RADIUS);

% Need to flip the coordinates of each line such that the last point of a
% line shares its coordinates with the first point of the next line
cLines = fliplines(cLines);
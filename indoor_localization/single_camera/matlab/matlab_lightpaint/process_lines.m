function [cLines ghosts] = process_lines(lines,color, END_SNAPPING, END_SNAP_RADIUS, ROBOT_RADIUS, SPACING, MIN_TRAVEL_DIST, N_ROBOTS, WGRID)
% Connect nonadjacent lines with "ghost" lines when neccessary
% To find the minimal path, run this once with each line as the first line
% and record the total length.
lengths = zeros(size(lines,1),3);
for i=1:size(lines,1)
    % Run add_ghosts with line i as the first line
    reorder_lines = lines;
    reorder_lines([1 i],:) = reorder_lines([i 1],:);
    [outlines outghosts ~] = add_ghosts(reorder_lines, color, END_SNAPPING, END_SNAP_RADIUS);
    lengths(i,1:2) = statistics(outlines, outghosts);
    lengths(i,3) = sum(lengths(i,1:2));
end
[~,startline] = min(lengths(:,3));
lines([1 startline],:) = lines([startline 1],:);
color([1 startline],:) = color([startline 1],:);

[lines,ghostidx,colors] = add_ghosts(lines, color, END_SNAPPING, END_SNAP_RADIUS);
ghosts = lines(ghostidx==1,:);
lines
cLines = computeMutex(SPACING, ROBOT_RADIUS, MIN_TRAVEL_DIST, N_ROBOTS, WGRID, lines, colors);
clear,clc;
%% Options section
% Input SVG and output WPT filenames
DIR = 'C:\pictures\';
INPUT = 'videodemo2.svg'; %'currentImg.svg';
OUTPUT = 'currentImg.wpt';

LAUNCH_TRACKER = false;

% Waypoint spacing and intersection radius constants
SPACING = 275;
ABSORPTION_RADIUS = 150;
INTERSECTION_RADIUS = 300;

% Enable/disable image scaling and centering
CENTER = true;
SCALE = true;
SCALE_MAX = 2800;
CENTER_LOCATION = [1700 1700];

% Enable/disable endpoint snapping
END_SNAPPING = true;
END_SNAP_RADIUS = 10;

%% Process the image
% Load the SVG image, convert it to usable data
lines = load_replace(fullfile(DIR,INPUT));

% Flip vertically
% Find the min x and y coordinates
min_y = min(min(lines(:,[2 4])));
height = max(max(lines(:,[2 4]))) - min_y;
centr_y = (min_y + (height/2))*2;
lines(:,2) = centr_y - lines(:,2);
lines(:,4) = centr_y - lines(:,4);

% Connect nonadjacent lines with "ghost" lines when neccessary
% To find the minimal path, run this once with each line as the first line
% and record the total length.
lengths = zeros(size(lines,1),3);
for i=1:size(lines,1)
    % Run add_ghosts with line i as the first line
    reorder_lines = lines;
    reorder_lines([1 i],:) = reorder_lines([i 1],:);
    [outlines outghosts] = add_ghosts(reorder_lines, END_SNAPPING, END_SNAP_RADIUS);
    lengths(i,1:2) = statistics(outlines, outghosts);
    lengths(i,3) = sum(lengths(i,1:2));
end
[min_dist startline] = min(lengths(:,3));
fprintf('Minimum path distance with starting line %u\n',startline);
lines([1 startline],:) = lines([startline 1],:);
[lines ghosts] = add_ghosts(lines, END_SNAPPING, END_SNAP_RADIUS);

% Calculate intersections
[ints intsA intsB] = find_intersections(lines);

% Scale and center the lines if enabled
if SCALE
    [lines ints] = scale_lines(lines, ints, SCALE_MAX);
end
if CENTER
    [lines ints] = center_lines(lines, ints, CENTER_LOCATION);
end

lines = round(lines);
ints = round(ints);

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

% Export to a .WPT file
export_wpt(cLines,ghosts,size(ints,1), INPUT,fullfile(DIR,OUTPUT));

%% Plot the processed lines
hold on
gi = find(ghosts);
li = find(ghosts == 0);
line([lines(li,1)';lines(li,3)'],[lines(li,2)';lines(li,4)'],'Color','r','Marker','.');
line([lines(gi,1)';lines(gi,3)'],[lines(gi,2)';lines(gi,4)'],'Color','b');
linedata = reshape([cLines{:,:}]',[],3);
linedata_ints = find(linedata(:,3) ~= -1);
plot(linedata(linedata_ints,1),linedata(linedata_ints,2),'gs');
plot(linedata(:,1),linedata(:,2),'k.');
axis([0 3450 0 3700])
hold off;

% Calculate and print statistics
n_ghosts = sum(ghosts);
n_lines = size(ghosts,1)-n_ghosts;
idx_ghosts = find(ghosts);

[l_ghosts l_lines] = statistics(lines, ghosts);

fprintf('----------------------\nImage line segments: %u\nGhost segments added: %u\n',n_lines,n_ghosts);
fprintf('Intersection points: %u\n',size(ints,1));
fprintf('Total ghost length: %0.f\nTotal drawn length: %0.f\nTotal path length: %0.f\n',...
    l_ghosts, l_lines, (l_lines+l_ghosts));

if LAUNCH_TRACKER
    cd '..\matlab_optitrack_v2';
    main_udp;
end


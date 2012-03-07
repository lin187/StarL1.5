clear,clc,close all;format longg;

%% Options section
FNAME = 'pineapple';
DIR = 'D:\';

LAUNCH_TRACKER = false;
MAXFRAMES = 3;

% Waypoint spacing and intersection radius constants
SPACING = 500;
ABSORPTION_RADIUS = 150;
INTERSECTION_RADIUS = 350;
SAFE_TRAVEL_RADIUS = 250;
ROBOT_RADIUS = 160;

% Enable/disable image scaling and centering
CENTER = true;
SCALE = true;
SCALE_MAX = 2900;
CENTER_LOCATION = [1700 1700];

SNAP_TO_GRID = true;
GRIDSIZE = [3200 3200];
ROBOTSIZE = 350;

% Enable/disable endpoint snapping
END_SNAPPING = true;
END_SNAP_RADIUS = 50;

OUTPUT = [FNAME '.wpt'];
INPUT = [FNAME '.svg'];
%% Load and pre-process the image
% Load the SVG image, convert it to usable data
[lines colors] = load_replace(fullfile(DIR,INPUT));

if size(lines,1) == 0
    error('There were no parseable lines in the specified image!');
end

% Flip vertically
% Find the min x and y coordinates
min_y = min(min(lines(:,[2 4])));
height = max(max(lines(:,[2 4]))) - min_y;
centr_y = (min_y + (height/2))*2;
lines(:,2) = centr_y - lines(:,2);
lines(:,4) = centr_y - lines(:,4);

% Scale and center the lines if enabled
if SCALE
    [lines] = scale_lines(lines, SCALE_MAX);
end
if CENTER
    [lines] = center_lines(lines, CENTER_LOCATION);
end

if SNAP_TO_GRID
    lines = remap_to_grid(lines, ROBOTSIZE, GRIDSIZE);
end

%% Separate the lines into multiple frames
[flines fcolors] = separate_frames(lines, colors, SAFE_TRAVEL_RADIUS, SPACING, MAXFRAMES);
n_frames = size(flines,3);

%% Process each frame
output = struct('framenum',{},'cLines',{},'lines',{});
for b=1:n_frames
    lines = flines(:,:,b);
    lines(lines == -1) = [];
    lines = reshape(lines,[],4);
    
    % Eliminate any lines with only a single point (occasional byproduct of gridsnapping)
    [lines colors elims] = eliminateSingletons(lines,colors);
    
    if isempty(lines)
        n_frames = b-1;
        break;
    end
    
    colors = [];
    for i=1:size(fcolors,1)
        if ~isequal(fcolors{i,b},-1)
           colors = [colors; fcolors{i,b}] ;
        end
    end
    
    [output(b).cLines output(b).lines] = process_lines(lines,colors, END_SNAPPING, END_SNAP_RADIUS, ROBOT_RADIUS, SPACING);
    output(b).framenum = b;
end

%% Output lines
% Export to a .WPT file
export_wpt(output, INPUT, SPACING, fullfile(DIR,OUTPUT));

% Print statistics
for i=1:n_frames      
    plot_lines(output(i).cLines, output(i).lines, i, n_frames);
end

set(gcf,'Position',[400 400 400*n_frames 400]);

if LAUNCH_TRACKER
    pause;
    cd '..\matlab_optitrack_v2';
    load_settings
    WPT_FILENAME = fullfile(DIR, OUTPUT);
    mainprog;
end

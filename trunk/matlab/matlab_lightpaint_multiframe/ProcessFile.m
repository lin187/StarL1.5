function [] = ProcessFile(FNAME, MAXFRAMES, LAUNCH_TRACKER)
%clear,clc,close all;format longg;
format longg;clc;close all;
%% Options section
DIR = 'C:\pictures\tests';

% Waypoint spacing and intersection radius constants
SPACING = 275;
ABSORPTION_RADIUS = 150;
INTERSECTION_RADIUS = 375;
SAFE_TRAVEL_RADIUS = 250;

% Enable/disable image scaling and centering
CENTER = true;
SCALE = true;
SCALE_MAX = 2850;
CENTER_LOCATION = [1600 1750];

% Snap to grid options
SNAP_TO_GRID = true;
GRIDSIZE = [3200 3200];
ROBOTSIZE = 350;

% Enable/disable endpoint snapping
END_SNAPPING = true;
END_SNAP_RADIUS = 50;

OUTPUT = [FNAME '.wpt'];
INPUT = [FNAME '.svg'];

if nargin == 1
    LAUNCH_TRACKER = true;
    MAXFRAMES = 1;
elseif nargin == 2
    LAUNCH_TRACKER = false;
end

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
output = struct('framenum',{},'cLines',{},'lines',{},'ghosts',{},'ints',{},'colors',{});
for b=1:n_frames
    lines = flines(:,:,b);
    lines(lines == -1) = [];
    lines = reshape(lines,[],4);

    if isempty(lines)
        n_frames = b-1;
        break;
    end
    
    color = [];%cell(0);
    for i=1:size(fcolors,1)
        if ~isequal(fcolors{i,b},-1)
           color = [color; fcolors{i,b}] ;
        end
    end
    
    process_lines;
    
    output(b).framenum = b;
    output(b).cLines = cLines;
    output(b).ghosts = ghosts;
    output(b).ints = ints;
    output(b).lines = lines;
    output(b).colors = color;
end;

%% Output lines
% Export to a .WPT file
export_wpt(output, INPUT, SPACING, fullfile(DIR,OUTPUT));

% Print statistics
total_len = 0;
for i=1:n_frames      
    plot_lines(output(i).cLines, output(i).lines, output(i).ghosts, i, n_frames);
    
	[len_ghosts len_lines] = statistics(output(i).lines, output(i).ghosts);
    fprintf('\nFrame %u\nLine lengths: %6.0f\nGhost lengths:%6.0f\nTotal lengths:%6.0f\n', i,len_lines, len_ghosts, (len_lines + len_ghosts));    
    total_len = total_len + (len_lines + len_ghosts);
end
fprintf('\nTotal length:%7.0f\n', total_len);    

set(gcf,'Position',[400 400 400*n_frames 400]);

if LAUNCH_TRACKER
    pause;
    cd '..\matlab_optitrack_v2';
    load_settings
    RETURN_DIR = '..\matlab_lightpaint_multiframe';
    WPT_FILENAME = fullfile(DIR, OUTPUT);
    mainprog;
end

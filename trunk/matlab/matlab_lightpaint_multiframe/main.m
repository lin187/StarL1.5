clear,clc;
%% Options section
% Input SVG and output WPT filenames
DIR = 'C:\pictures\';
INPUT = 'multiframe_test.svg';
OUTPUT = 'multiframe_test.wpt';

LAUNCH_TRACKER = false;

MAXFRAMES = 3;

% Waypoint spacing and intersection radius constants
SPACING = 325;
ABSORPTION_RADIUS = 150;
INTERSECTION_RADIUS = 225;
SAFE_TRAVEL_RADIUS = 250;

% Enable/disable image scaling and centering
CENTER = true;
SCALE = true;
SCALE_MAX = 2800;
CENTER_LOCATION = [1700 1700];

% Enable/disable endpoint snapping
END_SNAPPING = false;
END_SNAP_RADIUS = 15;

%% Load and pre-process the image
% Load the SVG image, convert it to usable data
lines = load_replace(fullfile(DIR,INPUT));

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

%% Separate the lines into multiple frames
flines = separate_frames(lines, SAFE_TRAVEL_RADIUS, SPACING, MAXFRAMES);
n_frames = size(flines,3);

%% Process each frame
output = struct('framenum',{},'cLines',{},'lines',{},'ghosts',{},'ints',{});
for b=1:n_frames
    lines = flines(:,:,b);
    lines(lines == -1) = [];
    lines = reshape(lines,[],4);

    if isempty(lines)
        n_frames = b-1;
        break;
    end
    
    process_lines;
    
    output(b).framenum = b;
    output(b).cLines = cLines;
    output(b).ghosts = ghosts;
    output(b).ints = ints;
    output(b).lines = lines;
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
end

set(gcf,'Position',[400 400 400*n_frames 400]);

if LAUNCH_TRACKER
    cd '..\matlab_optitrack_v2';
    main_udp;
end

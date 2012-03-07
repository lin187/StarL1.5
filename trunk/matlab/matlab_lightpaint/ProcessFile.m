function [] = ProcessFile(varargin)
%PROCESSFILE Process SVG file for painting.
%   PROCESSFILE(filename) Processes the SVG file named filename.svg in the
%   default directory, 'C:\pictures\tests'. Automatically launches the
%   tracking program upon completion.
%
%   PROCESSFILE(...,OPTION,VALUE) Processes the file with optional
%   arguments. Available options:
%
%     'frames' - maximum number of frames, default 1
%     'track'  - enable/disable launching the tracker, default true
%     'spacing' - point spacing, default 500
%     'gridsize' - size of grid used for grid snapping, default 300
%     'snap' - enable/disable snap to grid, default false
%     'radius' - radius of the robot, default 160
%     'dir' - the directory images and wpt files are stored in

format longg;
FNAME = varargin{1};
%% Options section
DIR = 'C:\pictures\tests';

% Waypoint spacing and intersection radius constants
SPACING = 500;
SAFE_TRAVEL_RADIUS = 250;
ROBOT_RADIUS = 160;

% Enable/disable image scaling and centering
CENTER = true;
SCALE = true;
SCALE_MAX = 2850;
CENTER_LOCATION = [1600 1750];

% Snap to grid options
SNAP_TO_GRID = false;
GRIDDIM = [3200 3200];
GRIDSIZE = 300;

% Enable/disable endpoint snapping
END_SNAPPING = true;
END_SNAP_RADIUS = 50;

LAUNCH_TRACKER = true;
MAXFRAMES = 1;

OUTPUT = [FNAME '.wpt'];
INPUT = [FNAME '.svg'];

% Parse input arguments
if nargin > 1 && mod(nargin,2) == 0
    error('Invalid number of arguments!');
elseif nargin > 1
    for i = 2:2:nargin
        if strcmpi(varargin{i},'frames')
            MAXFRAMES = varargin{i+1};
        elseif strcmpi(varargin{i},'track')
            LAUNCH_TRACKER = varargin{i+1};
        elseif strcmpi(varargin{i},'spacing')
            SPACING = varargin{i+1};
        elseif strcmpi(varargin{i},'gridsize')
            GRIDSIZE = varargin{i+1};
        elseif strcmpi(varargin{i},'snap')
            SNAP_TO_GRID = varargin{i+1};
        elseif strcmpi(varargin{i},'radius')
            ROBOT_RADIUS = varargin{i+1};
        elseif strcmpi(varargin{i},'dir')
            DIR = varargin{i+1};
        else
            warning(['Unrecognized argument: ' varargin{i}]);
        end
    end
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
    lines = remap_to_grid(lines,GRIDSIZE, GRIDDIM);
end

%% Separate the lines into multiple frames
[flines fcolors] = separate_frames(round(lines), colors, SAFE_TRAVEL_RADIUS, SPACING, MAXFRAMES);
n_frames = size(flines,3);

%% Process each frame
output = struct('framenum',{},'cLines',{},'lines',{});
for b=1:n_frames
    lines = flines(:,:,b);
    lines(lines == -1) = [];
    lines = reshape(lines,[],4);
    
    % Eliminate any lines with only a single point (occasional byproduct of gridsnapping)
    [lines,~,~] = eliminateSingletons(lines,colors);
    
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
    addpath('..\matlab_optitrack_v2');
    load_settings
    WPT_FILENAME = fullfile(DIR, OUTPUT);
    mainprog;
end

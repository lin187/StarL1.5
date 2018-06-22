function [] = ProcessFile(varargin)
%PROCESSFILE Process SVG file for painting.
%   PROCESSFILE(filename) Processes the SVG file named filename.svg in the
%   default directory, 'C:\pictures\tests'. Automatically launches the
%   tracking program upon completion.
%
%   PROCESSFILE(...,OPTION,VALUE) Processes the file with optional
%   arguments. Available options:
%
%     'track'  - enable/disable launching the tracker, default true
%     'spacing' - point spacing, default 500
%     'gridsize' - size of grid used for grid snapping, default 300
%     'snap' - enable/disable snap to grid, default false
%     'radius' - radius of the robot, default 160
%     'dir' - the directory images and wpt files are stored in
%     'dist' - the minimum robot travel distance, default 1000
%     'robots' - the number of participating robots, default 4
%     'worldsize' - the size of the world in [WIDTH HEIGHT] form

format longg;
FNAME = varargin{1};
%% Options section
DIR = 'C:\';

% Waypoint spacing and intersection radius constants
SPACING = 900;
ROBOT_RADIUS = 325;

% Enable/disable image scaling and centering
CENTER = true;
SCALE = true;
SCALE_MAX = 2700;

% Snap to grid options
SNAP_TO_GRID = false;
GRIDDIM = [3000 3200];
CENTER_LOCATION = round(GRIDDIM./2);
GRIDSIZE = 300;

% Settings
N_ROBOTS = 4;
MIN_TRAVEL_DIST = 1000;
WGRID = [100 100 GRIDDIM-GRIDSIZE];

% Enable/disable endpoint snapping
END_SNAPPING = true;
END_SNAP_RADIUS = 50;

LAUNCH_TRACKER = true;

OUTPUT = [FNAME '.wpt'];
INPUT = [FNAME '.svg'];

% Parse input arguments
if nargin > 1 && mod(nargin,2) == 0
    error('Invalid number of arguments!');
elseif nargin > 1
    for i = 2:2:nargin
        if strcmpi(varargin{i},'track')
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
        elseif strcmpi(varargin{i},'robots')
            N_ROBOTS = varargin{i+1};
        elseif strcmpi(varargin{i},'dist')
            MIN_TRAVEL_DIST = varargin{i+1};
		elseif  strcmpi(varargin{i},'worldsize')
			GRIDDIM = varargin{i+1};
			CENTER_LOCATION = round(GRIDDIM./2);
        else
            warning(['Unrecognized argument: ' varargin{i}]);
        end
    end
end

%% Load and pre-process the image
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

%% Process the lines
% Eliminate any lines with only a single point (occasional byproduct of gridsnapping)
[lines,colors,~] = eliminateSingletons(lines,colors);

[cLines ghosts] = process_lines(round(lines),colors, END_SNAPPING, END_SNAP_RADIUS, ROBOT_RADIUS, SPACING, MIN_TRAVEL_DIST, N_ROBOTS, WGRID);

%% Output lines
% Export to a .WPT file
export_wpt(cLines, INPUT, fullfile(DIR,OUTPUT));

if ~LAUNCH_TRACKER
    for i=-1:N_ROBOTS-1
        plot_lines(cLines, lines, ghosts,i, GRIDDIM);
        if i ~= N_ROBOTS-1
            pause;
        end
    end
else
    plot_lines(cLines, lines, ghosts, GRIDDIM);
    pause;
    addpath('..\matlab_optitrack');
    load_settings
    WPT_FILENAME = fullfile(DIR, OUTPUT);
    mainprog;
end

function [cameraCount] = track_init(project_file)

if nargin < 1
    project_file = 'C:\single_robot.ttp';
end

% load the NPTrackingTools library if it is not already loaded
if ~libisloaded('NPTrackingTools')
    disp('THE LIBRARY HAS NOT BEEN LOADED');
	addpath('C:\Program Files\OptiTrack\Tracking Tools\lib'); % change if necessary
	addpath('C:\Program Files\OptiTrack\Tracking Tools\inc'); % change if necessary
	[notfound,warnings]=loadlibrary('NPTrackingTools','NPTrackingTools.h');
end

% initialise cameras
calllib('NPTrackingTools', 'TT_Initialize');

% load the project file which sets up cameras correctly
calllib('NPTrackingTools', 'TT_LoadProject', project_file);

% abort if the library isn't loaded
track_checklibload()

cameraCount = calllib('NPTrackingTools', 'TT_CameraCount');
%libfunctionsview NPTrackingTools %--> use this to see available functions
TTP_FILENAME = 'C:\Users\StarL\Dropbox\research\optitrack\ALL_ROBOTS_PROJECT_newTryWithParams.ttp';
%TTP_FILENAME = 'C:\Users\StarL\Dropbox\research\optitrack\fardin.ttp';
USE_SERVER = 1; %Enable/disable the network server
USE_WPT = 1;    %Enable/disable loading waypoints and walls
USE_HISTORY = 1;%Enable/disable history
WPT_FILENAME = 'square.wpt';
% Grid size and spacing parameters
TX_PERIOD = 0.05;	%sec
X_MAX = 3100;       %mm
Y_MAX = 3700;       %mm
LINE_LEN = 167;     %mm (should be iRobot Create radius)

% Path tracing variables
MOTION_HISTORY_SIZE = 5; % number of points used to determine if motion has occurred
HISTORY_SIZE = 2500; %number of points in each history for drawing

% File export settings
SAVE_TO_FILE = false;
OUTPUT_FILENAME = 'C:\data.xml';
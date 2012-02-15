% Main OptiTrack interface
% Settings
TTP_FILENAME = 'C:\ALL_ROBOTS_PROJECT.ttp';
WPT_FILENAME = 'C:\pictures\multiframe_test.wpt';
USE_SERVER = 1; %Enable/disable the network server
USE_WPT = 1;    %Enable/disable loading waypoints and walls
USE_HISTORY = 1;%Enable/disable history

% Grid size and spacing parameters
TX_PERIOD = 0.1;	%sec
X_MAX = 3450;       %mm
Y_MAX = 3700;       %mm
LINE_LEN = 167;     %mm (should be iRobot Create radius)

% Path tracing variables
HISTORY_SIZE = 2500; %number of points in each history

mainprog;

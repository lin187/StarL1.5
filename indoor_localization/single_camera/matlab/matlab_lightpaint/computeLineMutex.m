javaclasspath('LineMutexVisualizer.jar');
import edu.illinois.linemutex.*;

robotRadius = 20;
waypointSpacing = 60;
numRobots = 4;
minTravelDistance = 500;
ghostLineColor= java.lang.String('ghost');
world = java.awt.Rectangle(100, 100, 1000, 1000); % x,y, width, height

% First define the lines using two endpoints and a color and store each
% line in 'linesIn'

linesIn = java.util.ArrayList;

% add the first line

% constructor is LineInput(Point s, Point e, String color)
startPoint = java.awt.Point(100, 200);
endPoint = java.awt.Point(600, 200);
color = java.lang.String('red');

aLine = LineInput(startPoint, endPoint, color);
linesIn.add(aLine);

% add the second line
startPoint = java.awt.Point(600, 200);
endPoint = java.awt.Point(100, 400);
color = java.lang.String('red');

aLine = LineInput(startPoint, endPoint, color);
linesIn.add(aLine);

% call the computation function; signature is:
% ArrayList <LineOutput> compute(
%			ArrayList <LineInput> lines, 
%			int waypointSpacing, 
%			int robotRadius,
%           int numRobots,
%			int minTravelDistance, 
%			Rectangle world,
%   		String ghostLineColor)
allRobots = edu.illinois.linemutex.LineMutexCompute.compute(linesIn, waypointSpacing, ...
            robotRadius, numRobots, minTravelDistance, world, ghostLineColor) ;

% print out the result
numRobots = allRobots.size() - 1;
idx = 1;
cLines = struct('robot',{},'waypoint',{},'ptx',{},'pty',{},'color',{},'mutex',{},'start',{},'end',{});
for i = 0:numRobots % for each line
    waypoints = allRobots.get(i).waypoints;
    numWaypoints = waypoints.size() - 1;
    
    for j = 0:numWaypoints % for each waypoint in the line
        wp = waypoints.get(j);
        x = wp.point.x;
        y = wp.point.y;
        
        color = char(wp.color);
        mutexId = wp.mutexId;
        
        if (wp.start) isStart = 'true'; else isStart = 'false'; end;
        if (wp.end) isEnd = 'true'; else isEnd = 'false'; end;
        
        disp(['Robot ', num2str(i) , ', Waypoint ', num2str(j) , ': Point=(', ...
            num2str(x) , ',', num2str(y), ') color=' , color , ' mutex=' , ...
            num2str(mutexId) , ' start=', isStart , ...
            ' end=', isEnd]);
        cLines(idx).robot = i;
        cLines(idx).waypoint = j;
        cLines(idx).ptx = x;
        cLines(idx).pty = y;
        cLines(idx).color = char(wp.color);
        cLines(idx).mutex = wp.mutexId;
        cLines(idx).start = wp.start;
        cLines(idx).end = wp.end;        
        idx = idx + 1;
    end
end
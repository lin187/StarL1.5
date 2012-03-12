function cLines = computeMutex(SPACING, ROBOT_RADIUS, MIN_TRAVEL_DIST, N_ROBOTS, WGRID, lines, colors)
if isequal(javaclasspath('-dynamic'),{})
    javaclasspath('LineMutexVisualizer.jar');
end
import edu.illinois.linemutex.*;

linesIn = java.util.ArrayList;

lines = int16(lines);

% constructor is LineInputData(DoublePoint s, DoublePoint e, Color c)
for i=1:size(lines,1)
    startPoint = java.awt.Point(lines(i,1), lines(i,2));
    endPoint = java.awt.Point(lines(i,3), lines(i,4));
    aLine = LineInput(startPoint, endPoint, colors(i,:));
    linesIn.add(aLine);
end

world = java.awt.Rectangle(WGRID(1), WGRID(2), WGRID(3), WGRID(4));

% call the computation function; signature is:
% ArrayList <LineOutput> compute(
%			ArrayList <LineInput> lines, 
%			int waypointSpacing, 
%			int robotRadius,
%           int numRobots,
%			int minTravelDistance, 
%			Rectangle world,
%   		String ghostLineColor)
allRobots = edu.illinois.linemutex.LineMutexCompute.compute(linesIn, SPACING, ...
            ROBOT_RADIUS, N_ROBOTS, MIN_TRAVEL_DIST, world, '000000') ;
        
% print out the result
idx = 1;
cLines = struct('robot',{},'waypoint',{},'ptx',{},'pty',{},'color',{},'mutex',{},'start',{},'end',{});
numRobots = allRobots.size() - 1;
for i = 0:numRobots % for each line
    points = allRobots.get(i).waypoints;
    numWaypoints = points.size() - 1;
     
    for j = 0:numWaypoints % for each waypoint in the line
        wp = points.get(j);
        cLines(idx).robot = i;
        cLines(idx).waypoint = j;
        wppoint = wp.point;
        cLines(idx).ptx = wppoint.x;
        cLines(idx).pty = wppoint.y;
        cLines(idx).color = char(wp.color);
        cLines(idx).mutex = wp.mutexId;
        cLines(idx).start = wp.start;
        cLines(idx).end = wp.end;        
        idx = idx + 1;
    end
end

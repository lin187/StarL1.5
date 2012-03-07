function cLines = computeMutex(SPACING, ROBOT_RADIUS, lines, colors)

% replace the path here with the path to the .jar file
%clear java;% <- you need this if you plan on modifying the .jar file to get rid of the previous definition
if isequal(javaclasspath('-dynamic'),{})
    javaclasspath('LineMutexVisualizer.jar');
end
import edu.illinois.linemutex.*;

% First define the lines using two endpoints and a color and store each
% line in 'linesIn'
linesIn = java.util.ArrayList;

% constructor is LineInputData(DoublePoint s, DoublePoint e, Color c)
for i=1:size(lines,1)
    startPoint = DoublePoint(lines(i,1), lines(i,2));
    endPoint = DoublePoint(lines(i,3), lines(i,4));
    aLine = LineInputData(startPoint, endPoint, colors(i,:));
    linesIn.add(aLine);
end

% call the computation function
% signature is 
% ArrayList <LineOutputData> compute(ArrayList <LineInputData> in, int spacing, int robotRadius)
allLines = edu.illinois.linemutex.LineMutexCompute.compute(linesIn, SPACING, ROBOT_RADIUS) ;

% print out the result
numLines = allLines.size() - 1;
cLines = cell(numLines,4);
for i = 0:numLines % for each line
    lineOutput = allLines.get(i);
    numWaypoints = lineOutput.points.size() - 1;
    
    x = zeros(numWaypoints,1);
    y = zeros(numWaypoints,1);
    mutex = zeros(numWaypoints,1);
    cLines(i+1,4) = {lineOutput.colors.get(0)};
    
    for j = 0:numWaypoints % for each waypoint in the line
        point = lineOutput.points.get(j);
        x(j+1) = point.x;
        y(j+1) = point.y;

        mutex(j+1) = lineOutput.mutexId.get(j)-1;
        
        disp(['(', num2str(x(j+1)), ',', num2str(y(j+1)), ') color=', lineOutput.colors.get(j), ' mutex=', num2str(mutex(j+1))]);
    end
    
    cLines(i+1,1) = {x};
    cLines(i+1,2) = {y};
    cLines(i+1,3) = {mutex};
end

% replace the path here with the path to the .jar file
% clear java <- you need this if you plan on modifying the .jar file to get rid of the previous definition
if isequal(javaclasspath('-dynamic'),{})
    javaclasspath('LineMutexVisualizer.jar');
end
import edu.illinois.linemutex.*;

robotRadius = 20;
waypointSpacing = 60;


% First define the lines using two endpoints and a color and store each
% line in 'linesIn'

linesIn = java.util.ArrayList;

% add the first line

% constructor is LineInputData(DoublePoint s, DoublePoint e, Color c)
startPoint = DoublePoint(100, 200);
endPoint = DoublePoint(600, 200);
color = java.lang.String('red');

aLine = LineInputData(startPoint, endPoint, color);
linesIn.add(aLine);

% add the second line
startPoint = DoublePoint(600, 200);
endPoint = DoublePoint(100, 400);
color = java.lang.String('red');

aLine = LineInputData(startPoint, endPoint, color);
linesIn.add(aLine);

% call the computation function
% signature is 
% ArrayList <LineOutputData> compute(ArrayList <LineInputData> in, int spacing, int robotRadius)
allLines = edu.illinois.linemutex.LineMutexCompute.compute(linesIn, waypointSpacing, robotRadius) ;

% print out the result
numLines = allLines.size() - 1;
for i = 0:numLines % for each line
    lineOutput = allLines.get(i);
    numWaypoints = lineOutput.points.size() - 1;
    
    for j = 0:numWaypoints % for each waypoint in the line
        point = lineOutput.points.get(j);
        x = point.x;
        y = point.y;
        
        color = lineOutput.colors.get(j);
        mutexId = lineOutput.mutexId.get(j);
        
        disp(['(', num2str(x), ',', num2str(y), ') color=', color, ' mutex=', num2str(mutexId)]);
    end
end


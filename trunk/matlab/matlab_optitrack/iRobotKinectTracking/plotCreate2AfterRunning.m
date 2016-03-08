global imgColorPlotted
imgColorPlotted = zeros(420,560,3,num_frames,'uint8');
showCoordinates = 1;
videoOnly = 0;
offset = frameCount - length(botArray(1).centers);
goal_radius = 50;

if ~isempty(waypoints)
    goal_centers = [];
    goal_radii = [];
    for i = 1:size(waypoints,2)
        goal_centers = [goal_centers; waypoints(i).X, waypoints(i).Y];
        goal_radii = [goal_radii; goal_radius];
    end
else
    goal_centers = 0;
end
%goal_centers = 0;
for i = offset + 1:frameCount - offset
    plotCreate2(imgColorAll(:,:,:,i), botArray, robot_count, i - offset, goal_centers, goal_radii, showCoordinates, videoOnly);
end
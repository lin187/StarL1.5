global imgColorPlotted
% use this script to make an array of images with robot positions/circles
% plotted on top of the captured images. To make an avi video, run
% makeVideo.m after running this script.
imgColorPlotted = zeros(420,560,3,num_frames,'uint8');
showCoordinates = 1;
showBBox = 0;
videoOnly = 0; % 0 - show detected circles 1 - just get video, no plotting
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
    goal_radii = 0;
end
goal_centers = 0;
goal_radii = 0;

for i = offset + 1:frameCount - offset
    plotBotsKinect(imgColorAll(:,:,:,i), i - offset, goal_centers, goal_radii, showCoordinates, videoOnly, showBBox);
end
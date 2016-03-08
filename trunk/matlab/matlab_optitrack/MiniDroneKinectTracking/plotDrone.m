function plotDrone(img, droneArray, numDrones, index, fig, waypoints)
global imgColorAll
global mm_per_pixel
sfigure(fig);
clf;
xCenterMM = 0;
yCenterMM = 0;
mm_per_pixel_now = mmPerPixel(droneArray(1).radius);
image([xCenterMM - mm_per_pixel_now*320, xCenterMM + mm_per_pixel_now*320],...
    [yCenterMM - mm_per_pixel_now*240, yCenterMM + mm_per_pixel_now*240], img)
set(gca, 'YDir', 'normal');
xlabel('mm')
ylabel('mm')
hold on
for i = 1:numDrones
    if ~isempty(droneArray(i).center)
        %figure(1);
        coord_mm = getMMCoord(droneArray(i).center, droneArray(i).radius);
        plot(coord_mm(1,1), coord_mm(1,2), 'yx')
        str = [int2str(coord_mm(1,1)), ' (mm), ', int2str(coord_mm(1,2)), ' (mm)'];
        text(coord_mm(1,1) - 50, coord_mm(1,2) - 250, str, 'Color', 'y');  
        viscircles(coord_mm, droneArray(i).radius*mmPerPixel(droneArray(i).radius), 'EdgeColor', droneArray(i).color);
%         rectangle('Position', droneArray(i).BBox, 'EdgeColor',droneArray(i).color, 'LineWidth', 3);
% %         viscircles([droneArray(i).destinations(1,1),...
% %             droneArray(i).destinations(1,2)], 10, 'EdgeColor', droneArray(i).color);
        goal_centers = [];
        goal_radii = [];
        for j = 1:length(waypoints)
            goal_centers = [goal_centers; [waypoints(j).X, waypoints(j).Y]]
            goal_radii = [goal_radii, 25];
        end
        viscircles(goal_centers, goal_radii);
    end
end
f = getframe(gcf);
imgColorAll(:,:,:,index) = f.cdata;

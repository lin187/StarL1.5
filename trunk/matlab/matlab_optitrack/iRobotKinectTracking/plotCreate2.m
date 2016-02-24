function plotCreate2(img, botArray, numBots, index, fig)
global imgColorAll
global mm_per_pixel
sfigure(fig);
clf;
image([-320*mm_per_pixel, 320*mm_per_pixel], [-240*mm_per_pixel, mm_per_pixel*240], img)
set(gca, 'YDir', 'normal');
xlabel('mm')
ylabel('mm')
hold on
for i = 1:numBots
    if ~isempty(botArray(i).center)
        center_mm = getMMCoordiRobot(botArray(i).center);
        plot(center_mm(1,1),center_mm(1,2), 'yx')
        str = [int2str(center_mm(1,1)), ', ', int2str(center_mm(1,2)), ', ', num2str(botArray(i).yaw), sprintf('45%c', char(176))];
        text(center_mm(1,1) + 50, center_mm(1,2) + 200, str, 'Color', 'y');  
        viscircles(center_mm, botArray(i).radius*mm_per_pixel, 'EdgeColor', botArray(i).color);
        %rectangle('Position', botArray(i).BBox, 'EdgeColor',botArray(i).color, 'LineWidth', 3);
        goal_centers = [160,120; 160,360; 480,120; 480,360];
        goal_radii = [10 10 10 10];
        viscircles(goal_centers*mm_per_pixel, goal_radii*mm_per_pixel);
    end
end
f = getframe(fig);
imgColorAll(:,:,:,index) = f.cdata;

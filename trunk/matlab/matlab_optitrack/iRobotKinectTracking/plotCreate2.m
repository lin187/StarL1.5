function plotCreate2(img, botArray, numBots, index, showCoordinates)
global imgColorPlotted
global mm_per_pixel
sfigure(2);
clf;
image([-320*mm_per_pixel, 320*mm_per_pixel], [-240*mm_per_pixel, mm_per_pixel*240], img)
set(gca, 'YDir', 'normal');
xlabel('mm')
ylabel('mm')
hold on
for i = 1:numBots
    if ~isempty(botArray(i).center)
%         center_mm = getMMCoordiRobot(botArray(i).center);
        center_mm = botArray(i).centers(index,:);
        plot(center_mm(1,1),center_mm(1,2), 'yx')
        yaw = round(botArray(i).yaws(index));
        str = ['X: ', int2str(center_mm(1,1)), ', Y: ', int2str(center_mm(1,2)), ', ', num2str(yaw), sprintf('%c', char(176))];
        if showCoordinates
            text(center_mm(1,1) - 500, center_mm(1,2) + 250, str, 'Color', 'm');
        end
        viscircles(center_mm, botArray(i).radii(index)*mm_per_pixel, 'EdgeColor', botArray(i).color);
        %rectangle('Position', botArray(i).BBox, 'EdgeColor',botArray(i).color, 'LineWidth', 3);
%         goal_centers = [160,120; 160,360; 480,120; 480,360];
%         goal_radii = [10 10 10 10];
%         viscircles(goal_centers*mm_per_pixel, goal_radii*mm_per_pixel);
    end
end
f = getframe(gcf);
imgColorPlotted(:,:,:,index) = f.cdata;

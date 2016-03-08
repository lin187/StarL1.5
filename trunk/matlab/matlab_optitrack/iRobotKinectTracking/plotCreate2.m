function plotCreate2(img, botArray, numBots, index, goal_centers, goal_radii, showCoordinates, videoOnly)
global imgColorPlotted
global mm_per_pixel
global optBotType
sfigure(2);
clf;
image([-320*mm_per_pixel, 320*mm_per_pixel], [-240*mm_per_pixel, mm_per_pixel*240], img)  
set(gca, 'YDir', 'normal');
xlabel('mm')
ylabel('mm')
hold on
if ~videoOnly
    for i = 1:numBots
        if ~isempty(botArray(i).center)
            center_mm = getMMCoordiRobot(botArray(i).centers(index,:));
            %center_mm = botArray(i).centers(index,:);
            plot(center_mm(1,1),center_mm(1,2), 'yx')
            str = ['X: ', int2str(center_mm(1,1)), ', Y: ', int2str(center_mm(1,2))];
            if optBotType == 0
                yaw = round(botArray(i).yaws(index));
                str = [str, ', ', num2str(yaw), sprintf('%c', char(176))];
            end
            if showCoordinates
                text(center_mm(1,1) - 500, center_mm(1,2) + 250, str, 'Color', 'y');
            end
            viscircles(center_mm, botArray(i).radii(index)*mm_per_pixel, 'EdgeColor', botArray(i).color);
            %rectangle('Position', botArray(i).BBox, 'EdgeColor',botArray(i).color, 'LineWidth', 3);
            if goal_centers ~= 0
                viscircles(goal_centers, goal_radii);
            end
        end
    end
end
f = getframe(gcf);
imgColorPlotted(:,:,:,index) = f.cdata;

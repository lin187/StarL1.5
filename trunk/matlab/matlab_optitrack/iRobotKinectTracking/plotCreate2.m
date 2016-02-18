function plotCreate2(img, botArray, numBots, index, fig)
global imgColorAll
global mm_per_pixel
sfigure(fig);
clf;
image([0, 640*mm_per_pixel], [0, mm_per_pixel*480], img)
set(gca, 'YDir', 'normal');
xlabel('mm')
ylabel('mm')
hold on
for i = 1:numBots
    if ~isempty(botArray(i).center)
        plot(botArray(i).center(1)*mm_per_pixel, botArray(i).center(2)*mm_per_pixel, 'yx')
        str = [int2str(botArray(i).center(1,1)*mm_per_pixel), ', ', int2str(botArray(i).center(1,2)*mm_per_pixel), ', ', num2str(botArray(i).yaw), sprintf('45%c', char(176))];
        text(botArray(i).center(1)*mm_per_pixel + 50, botArray(i).center(2)*mm_per_pixel + 200, str, 'Color', 'y');  
        viscircles(botArray(i).center*mm_per_pixel, botArray(i).radius*mm_per_pixel, 'EdgeColor', botArray(i).color);
        %rectangle('Position', botArray(i).BBox, 'EdgeColor',botArray(i).color, 'LineWidth', 3);
        goal_centers = [160,120; 160,360; 480,120; 480,360];
        goal_radii = [10 10 10 10];
        viscircles(goal_centers*mm_per_pixel, goal_radii*mm_per_pixel);
    end
end
f = getframe(fig);
imgColorAll(:,:,:,index) = f.cdata;

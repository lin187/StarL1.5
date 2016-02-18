function plotCreate2(img, botArray, numBots, index, fig)
global imgColorAll
global mm_per_pixel
sfigure(fig);
clf;
image(img)
hold on
for i = 1:numBots
    if ~isempty(botArray(i).center)
        plot(botArray(i).center(1), botArray(i).center(2), 'yx')
        str = [int2str(botArray(i).center(1,1)*mm_per_pixel), ', ', int2str(botArray(i).center(1,2)*mm_per_pixel), ', ', num2str(botArray(i).yaw)];
        text(botArray(i).center(1) - 10, botArray(i).center(2) - 80, str, 'Color', 'y');  
        viscircles(botArray(i).center, botArray(i).radius, 'EdgeColor', botArray(i).color);
        rectangle('Position', botArray(i).BBox, 'EdgeColor',botArray(i).color, 'LineWidth', 3);
        goal_centers = [160,120; 160,360; 480,120; 480,360];
        goal_radii = [10 10 10 10];
        viscircles(goal_centers, goal_radii);
    end
end
f = getframe(fig);
imgColorAll(:,:,:,index) = f.cdata;

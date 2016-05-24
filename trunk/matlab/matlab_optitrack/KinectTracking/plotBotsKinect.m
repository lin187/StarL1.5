function plotBotsKinect(img, index, goal_centers, goal_radii, showCoordinates, videoOnly, showBBox)
global imgColorPlotted
global mm_per_pixel
global MINIDRONE
global CREATE2
global numDrones
global numCreates
global botArray
numBots = numDrones + numCreates;
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
            % get coordinates in ground frame 
            center_mm = getMMCoord(botArray(i).centers(index,:), 1, CREATE2);
            plot(center_mm(1,1),center_mm(1,2), 'yx')
            % get actual coordinates for showing positions
            center_mm_text = getMMCoord(botArray(i).centers(index,:), botArray(i).radii(index), botArray(i).type);
            str = ['X: ', int2str(center_mm_text(1,1)), ', Y: ', int2str(center_mm_text(1,2))];
            
            yaw = round(botArray(i).yaws(index));
            str = [str, ', ', num2str(yaw), sprintf('%c', char(176))];
            
            if showCoordinates
                text(center_mm(1,1) - 500, center_mm(1,2) + 350, str, 'Color', 'y');
            end
            viscircles(center_mm, botArray(i).radii(index)*mm_per_pixel, 'EdgeColor', botArray(i).color);
            % below is not finished
            if showBBox
                BBox = botArray(i).BBoxes(index,:);
                BBox(1,1:2) = getMMCoord(BBox(1,1:2), botArray(i).radii(index), CREATE2);
                BBox(1,3:4) = BBox(1,3:4)*mm_per_pixel;
                rectangle('Position', BBox, 'EdgeColor','y', 'LineWidth', 3);
            end
            if goal_centers ~= 0
                if botArray(i).type == MINIDRONE
%                     m = mmPerPixel(botArray(i).radii(index));
%                     minimum = 1000000;
%                     for j = 1:length(goal_centers)
%                         dist = norm2(center_mm_text - goal_centers(i,:));
%                         if dist < minimum
%                             minimum = dist;
%                             ind = j;
%                         end
%                     end
%                     viscircles(goal_centers(ind,:) * (m/mm_per_pixel), goal_radii(ind) * (m/mm_per_pixel));
                else
                    viscircles(goal_centers, goal_radii);
                end
            end
        end
    end
end
f = getframe(gcf);
imgColorPlotted(:,:,:,index) = f.cdata;

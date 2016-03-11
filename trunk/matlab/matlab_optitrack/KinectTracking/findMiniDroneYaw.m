function  angle  = findMiniDroneYaw( imgColor, BBox, yaw, center, radius)
% finds the yaw of the minidrone using yellow and cyan markers

% take only pixels in bounding box
frame = getPixelsInBB(imgColor, BBox);

% black out pixels that aren't contained in bot's circle
% this is so yaw estimation won't pick up other bot's circles when too
% close
% would be better to do this without a loop if possible
for i = 1:size(frame, 1)
    for j = 1:size(frame,2);
        if (i - center(1,1))^2 + (j - center(1,2))^2 > radius^2
            frame(j,i,:) = 0;
        end
    end
end

red = frame(:,:,1);
green = frame(:,:,2);
blue = frame(:,:,3);

% threshold for magenta
mag_red = red > 120 & red < 190;
mag_green = green < 70;
mag_blue = blue > 60 & blue < 130;
mag_img = mag_red .* mag_green .* mag_blue;

% threshold for yellow
yel_red = red > 140 & red < 220;
yel_green = green > 140 & green < 205;
yel_blue = blue < 95;
yel_img = yel_red .* yel_green .* yel_blue;

% label areas
mag_img_labeled = bwlabel(mag_img);
yel_img_labeled = bwlabel(yel_img);
areas = [];

% find centroids
mag_props = regionprops(mag_img_labeled, 'Area', 'Centroid');
yel_props = regionprops(yel_img_labeled, 'Area', 'Centroid');

% if regionprops were found, find yaw
if ~isempty(mag_props) > 0 && ~isempty(yel_props)
    % find centroid from region with largest area
    for i = 1:length(mag_props)
        areas(i) = mag_props(i).Area;
    end
    [~,index] = find(areas == max(areas));
    mag_center = mag_props(index(1)).Centroid(:)';
    
    areas = [];
    for i = 1:length(yel_props)
        areas(i) = yel_props(i).Area;
    end
    [~,index] = find(areas == max(areas));
    yel_center = yel_props(index(1)).Centroid(:)';
    
    A = yel_center - mag_center;
    angle = rad2deg(angleBtwVectors(A,[1,0]));
%     figure(2);
%     imshow(frame)
%     hold on
%     plot([mag_center(1),yel_center(1)], [mag_center(2),yel_center(2)], 'g');
%     text(center(1,1), center(1,2), num2str(angle));
% if regionprops not found, return prev yaw
else
    angle = yaw;
end

end


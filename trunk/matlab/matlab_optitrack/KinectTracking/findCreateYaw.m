function angle = findCreateYaw(imgColor, BBox, yaw, center, radius)
% input the image, returns yaw
% if yaw cannot be found, it returns the yaw value input
rmin_yaw = 5;
rmax_yaw = 10;

% take only pixels in bounding box
frame = getPixelsInBB(imgColor, BBox);

% black out pixels that aren't contained in bot's circle
% this is so yaw estimation won't pick up other bot's circles when too
% close
% would be better to do this without a loop if possible
% for i = 1:size(frame, 1)
%     for j = 1:size(frame,2);
%         if (i - center(1,1))^2 + (j - center(1,2))^2 > radius^2
%             frame(j,i,:) = 0;
%         end
%     end
% end

% make matrices with with x and y coordinates as values
x = (1:size(frame,2));
X = repmat(x,size(frame,2),1);
y = (1:size(frame,1))';
Y = repmat(y,1,size(frame,1));

% make a matrix with with 1's inside circle, 0's outside
imgfilt = (X - center(1,1)).^2 + (Y - center(1,2)).^2 <= radius^2;
% make the matrix NxNx3
imgfilt = repmat(imgfilt,1,1,3);
% multiple frame by the matrix to black out pixels
frame = frame .* uint8(imgfilt);

[centersYaw, radiiYaw, ~] = imfindcircles(frame, [rmin_yaw,rmax_yaw], ...
    'ObjectPolarity', 'bright', 'Sensitivity', 0.94);
    if size(centersYaw,1) < 2
        'yaw not found'
        angle = yaw;
        return
    end
    rnd = round(centersYaw);
    % sum rgb values in row 1 for white, rg values for yellow 
    rgb_sums = zeros(2,length(radiiYaw));
    for i = 1:length(radiiYaw)
        rgb_sums(1,i) = sum(frame(rnd(i,2), rnd(i,1), :));
        rgb_sums(2,i) = sum(frame(rnd(i,2), rnd(i,1), 1:2)) - frame(rnd(i,2), rnd(i,1), 3);
    end
    % white and yellow are max of sums of rgb and rg
    [~, white] = max(rgb_sums(1,:));
    [~, yellow] = max(rgb_sums(2,:));
    A = centersYaw(white,:) - centersYaw(yellow,:);
    angle = rad2deg(angleBtwVectors(A,[0,1]));
   % angle = angle - 90;
   
    
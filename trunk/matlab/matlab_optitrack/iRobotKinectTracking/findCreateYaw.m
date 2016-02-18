function angle = findCreateYaw(frame, yaw)
% input the bounding box cropped image, returns yaw
% if yaw cannot be found, it returns the yaw value input
rmin_yaw = 5;
rmax_yaw = 10;
[centersYaw, radiiYaw, metricsYaw] = imfindcircles(frame, [rmin_yaw,rmax_yaw], ...
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
   
    
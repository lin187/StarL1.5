function frame = getPixelsInColorBB(img, BBox)
% this function returns an image containing only the pixels in the bounding
% box. If the bounding box would go outside the image, it accounts for that
if length(size(img)) == 2 % depth image
    frame = img(max([BBox(2),1]):min([BBox(2) + BBox(4),1080]), ...
        max([BBox(1),1]):min([BBox(1) + BBox(3), 1920]));
elseif length(size(img)) == 3 % RGB image
    frame = img(max([BBox(2),1]):min([BBox(2) + BBox(4),1080]), ...
        max([BBox(1),1]):min([BBox(1) + BBox(3), 1920]),:);
end


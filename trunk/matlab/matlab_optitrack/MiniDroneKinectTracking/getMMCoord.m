function out = getMMCoord(coordinates, radius)

xCenterMM = 1812.254503;
yCenterMM = 1359.190877;
xCenterPx = 320;
yCenterPx = 240;

x = coordinates(1,1);
y = coordinates(1,2);

mm_per_pixel = mmPerPixel(radius);

out(1,1) = xCenterMM + (x - xCenterPx) * mm_per_pixel;
out(1,2) = yCenterMM + (y - yCenterPx) * mm_per_pixel;

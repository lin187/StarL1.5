function out = getMMCoord(coordinates, radius, type)
global MINIDRONE
global CREATE2
global ARDRONE
global mm_per_pixel

xCenterMM = 0;
yCenterMM = 0;
xCenterPx = 320;
yCenterPx = 240;

x = coordinates(1,1);
y = coordinates(1,2);

if type == MINIDRONE || type == ARDRONE
    mmpp = mmPerPixel(radius, type);
elseif type == CREATE2
    mmpp = mm_per_pixel;
end
    
out(1,1) = xCenterMM + (x - xCenterPx) * mmpp;
out(1,2) = yCenterMM + (y - yCenterPx) * mmpp;

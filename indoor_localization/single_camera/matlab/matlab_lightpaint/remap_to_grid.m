function data2 = remap_to_grid(data, SPACING, GRIDRANGE)
% Gridrange is of the form [XMAX YMAX]
% Data is of the form [x1 y1 x2 y2;....]

xgrid = 0:SPACING:GRIDRANGE(1) + SPACING/2;
ygrid = 0:SPACING:GRIDRANGE(2) + SPACING/2;

datax = [data(:,1); data(:,3)];
datay = [data(:,2); data(:,4)];

newX = interp1(xgrid, xgrid, datax, 'nearest');
newY = interp1(ygrid, ygrid, datay, 'nearest');

datalen = length(newX)/2;

data2 = [newX(1:datalen) newY(1:datalen) newX(datalen+1:end) newY(datalen+1:end)];

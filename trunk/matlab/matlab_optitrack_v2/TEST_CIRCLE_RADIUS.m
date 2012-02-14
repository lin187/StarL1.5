x0 = 0;
y0 = 0;

x1 = -100;
y1 = -100;

theta = -45;

alpha = atand((y1-y0)/(x1-x0));

r = sqrt((x1-x0)^2 + (y1-y0)^2)/(2*sind(alpha-theta))

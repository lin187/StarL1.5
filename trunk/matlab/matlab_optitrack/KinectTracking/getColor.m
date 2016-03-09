function color = getColor(imgColor, center)
colors = ['r', 'g', 'b'];
centerRounded = round(center);
[~,colorIndex] = max(imgColor(centerRounded(1,2), centerRounded(1,1), :));
color = colors(colorIndex(1));
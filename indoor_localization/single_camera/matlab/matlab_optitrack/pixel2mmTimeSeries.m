function out = pixel2mmTimeSeries(botIndex)
global botArray

centers = botArray(botIndex).centers;
radii = botArray(botIndex).radii;
type = botArray(botIndex).type;

for i = 1:length(centers)
    out(i,1:2) = getMMCoord(centers(i,:), radii(i), type);
end
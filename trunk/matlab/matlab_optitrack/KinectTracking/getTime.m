function out = getTime(times)
% input: array of elapsed times for each frame
% output: array of times in seconds, starting at t = 0
% use for plotting robot positions/angles after running
out(1) = 0;
for i = 2:length(times)
    out(i) = out(i-1) + times(i);
end
function out = getTime(times)

out(1) = 0;
for i = 2:length(times)
    out(i) = out(i-1) + times(i);
end
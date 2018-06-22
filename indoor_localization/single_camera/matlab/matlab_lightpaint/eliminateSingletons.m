function [lines,colors,e] = eliminateSingletons(lines,colors)
i = 1;
e = 0;
while i <= size(lines,1)
    if isequal(lines(i,1:2),lines(i,3:4))
       lines(i,:) = [];
       colors(i,:) = [];
       e = e + 1;
    else
        i = i + 1;
    end
end
% Calculate intersections between all possible line pairs
function [ints line_A line_B] = find_intersections(lines)
num_lines = size(lines,1);
max_ints = num_lines*(num_lines-1)/2;
ints = ones(max_ints,2)*-1;
line_A = ones(max_ints,1)*-1;
line_B = ones(max_ints,1)*-1;
int_count = 1;

% Separate the loaded line into X Y coordinate pairs
x = lines(:,1:2:3);
y = lines(:,2:2:4);

for i0 = 1:num_lines-1
    for i1 = (i0+1):num_lines
        [xi yi] = intersections(x(i0,:),y(i0,:),x(i1,:),y(i1,:));
        % Make sure this intersection isn't a line endpoint
        if ~isempty(xi) && (~isequal([xi yi],[x(i0,1) y(i0,1)]) && ~isequal([xi yi],[x(i0,2) y(i0,2)]))
            ints(int_count,:) = [xi yi];
            line_A(int_count) = i0;
            line_B(int_count) = i1;
        end
        int_count = int_count + 1;
    end
end

% Remove any intersections that match line coordinates
%ints(ints == -1) = [];
%ints = reshape(ints,[],2);
linematch = [lines(:,1:2); lines(:,3:4)];
for i = 1:size(ints,1)
   if ints(i,:) ~= [-1 -1]
       if ismember(round(ints(i,:)),linematch,'rows')
           ints(i,:) = [-1 -1];
           line_A(i) = -1;
           line_B(i) = -1;
       end
   end
end

% Remove any blank intersections
ints(ints == -1) = [];
ints = reshape(ints,[],2);
line_A(line_A == -1) = [];
line_B(line_B == -1) = [];
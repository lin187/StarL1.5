function cLines = label_intersections(cLines, ints, intsA, intsB, INTERSECTION_RADIUS)
n_ints = size(ints,1);
for i = n_ints:-1:1
    lineA = intsA(i);
    lineB = intsB(i);
%     lineA_contents = reshape(cell2mat(cLines(lineA,:)),[],3);
%     lineB_contents = reshape(cell2mat(cLines(lineB,:)),[],3);    
% 
%     % Apply the label to points closer than SPACING from the intersection
%     matching_idx = [];
%     for b = 1:size(lineA_contents(:,1:2),1)
%         if p_dist(lineA_contents(b,1:2), ints(i,:)) < INTERSECTION_RADIUS
%             matching_idx = [matching_idx b];
%         end
%     end
%     lineA_contents(matching_idx,3) = i-1;
%     
%     matching_idx = [];
%     for b = 1:size(lineB_contents(:,1:2),1)
%         if p_dist(lineB_contents(b,1:2), ints(i,:)) < INTERSECTION_RADIUS
%             matching_idx = [matching_idx b];
%         end
%     end
%     lineB_contents(matching_idx,3) = i-1;
    
    for b = 1:size(cLines,1)
        line_contents = reshape(cell2mat(cLines(b,:)),[],3);
        for j = 1:size(line_contents,1)
           if p_dist(line_contents(j,1:2), ints(i,:)) < INTERSECTION_RADIUS
               line_contents(j,3) = i-1;
           end
        end
        cLines{b,3} = line_contents(:,3)';
    end
    
    
    
%     for b = 1:3
%         cLines(lineA,b) = {lineA_contents(:,b)'};
%         cLines(lineB,b) = {lineB_contents(:,b)'};
%     end
end
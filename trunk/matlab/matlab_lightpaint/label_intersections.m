function cLines = label_intersections(cLines, ints, intsA, intsB, INTERSECTION_RADIUS)
n_ints = size(ints,1);
for i = 1:n_ints
    lineA = intsA(i);
    lineB = intsB(i);
    lineA_contents = reshape(cell2mat(cLines(lineA,:)),[],3);
    lineB_contents = reshape(cell2mat(cLines(lineB,:)),[],3);    

    % Apply the label to points closer than SPACING from the intersection
    matching_idx = [];
    for b = 1:length(lineA_contents(:,1:2))
        if p_dist(lineA_contents(b,1:2), ints(i,:)) < INTERSECTION_RADIUS
            matching_idx = [matching_idx b];
        end
    end
    lineA_contents(matching_idx,3) = i-1;
    
    matching_idx = [];
    for b = 1:length(lineB_contents(:,1:2))
        if p_dist(lineB_contents(b,1:2), ints(i,:)) < INTERSECTION_RADIUS
            matching_idx = [matching_idx b];
        end
    end
    lineB_contents(matching_idx,3) = i-1;
    
    for b = 1:3
        cLines(lineA,b) = {lineA_contents(:,b)'};
        cLines(lineB,b) = {lineB_contents(:,b)'};
    end
end
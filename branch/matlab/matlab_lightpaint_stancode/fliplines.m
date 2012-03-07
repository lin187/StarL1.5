function cLines = fliplines(cLines)
num_lines = size(cLines,1);

for i = 1:(num_lines-1)
    cur = round(reshape([cLines{i,1:3}],[],3));
    next = round(reshape([cLines{i+1,1:3}],[],3));
    
    cursize = size(cur,1);
    nextsize = size(next,1);
    
    if cur(1,1:2) == next(1,1:2)
        cur = flipud(cur);
    elseif cur(1,1:2) == next(nextsize,1:2)
        cur = flipud(cur);
        next = flipud(next);
    elseif cur(cursize,1:2) == next(nextsize,1:2)
        next = flipud(next);
    end
    
    % Put everything back where you found it
    cLines{i,1} = cur(:,1);
    cLines{i,2} = cur(:,2);
    cLines{i,3} = cur(:,3);
    cLines{i+1,1} = next(:,1);
    cLines{i+1,2} = next(:,2);
    cLines{i+1,3} = next(:,3);
end
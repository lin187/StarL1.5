function [outlines outcolor] = parse_path(path, color)
outlines = [];
outcolor = [];
for i=1:length(path)
    path(i) = regexprep(path(i), 'm', '');
    
    % Error if there are any curves present
    findcurve = regexp(path(i), 'c', 'match');
    if ~cellfun('isempty', findcurve)
       error('Curves are not currently supported'); 
    end
    
    parts = regexprep(path(i),'[l,]', ' ');
    pval = textscan(parts{:},'%d ');
    pval = reshape(double(cell2mat(pval)),2,[])';
    
    if size(pval,1) == 1
        warning('Single point path detected! Ignoring.');
        break;
    end
    
    out = pval(1,:);
    idx = 2;
    pval(1,:) = [];
    
    while(~isequal(pval,zeros(0,2)))
        out(idx,:) = out(idx-1,:) + pval(1,:);
        out(idx+1,:) = out(idx,:);
        pval(1,:) = [];
        idx = idx + 2;
    end
    
    out = out(1:length(out)-1,:);
    nout = zeros(length(out)/2,4);
    
    for b=1:(length(out)/2)
       nout(b,:) = [out((2*b)-1,:) out(2*b,:)];
    end
    
    outlines = [outlines ; nout];
    outcolor = [outcolor ; repmat(color(i),size(nout,1),1)];
end

function export_wpt(cLines,ghosts,intcount,INPUT,OUTPUT)
num_lines = size(cLines,1);

fid = fopen(OUTPUT,'w');
fprintf(fid, '%% Waypoints for image: %s\n%% Processed: %s\n%% Name format: [Line #]-[Color]-[Point #]-[Line Length]\n\n',INPUT, datestr(now));

% Print the metadata points
% THIS MUST BE THE FIRST WAYPOINT IN THE FILE:
fprintf(fid, '%% Metadata:\nWAY,-1,-1,%u,NUM_LINES\n',(num_lines-1));
fprintf(fid, 'WAY,-1,-1,%u,NUM_INTS\n',intcount);
for i = 1:num_lines
   % Print the metadata point for this line segment
   n_points = size((reshape([cLines{i,:}]',[],3)),1);
   fprintf(fid, 'WAY,-1,%u,%u,LINE%u\n',(i-1),(n_points-1),(i-1));
end

for i = 1:num_lines
   fprintf(fid, '\n%% Line %u\n',(i-1));
   linedata = reshape([cLines{i,:}]',[],3);

   % Find the number of points in the line, the locations of
   % intersections, and the color value
   n_points = size(linedata,1);
   linedata_ints = find(linedata(:,3) ~= -1);
   color = ~ghosts(i);
   
   for b = 1:n_points
       if ismember(b,linedata_ints)
           fprintf(fid, 'WAY,%u,%u,0,%u-%u-%u-%u-%u\n',linedata(b,1),...
               linedata(b,2),(i-1),color,(b-1),(n_points-1),linedata(b,3));
       else
           fprintf(fid, 'WAY,%u,%u,0,%u-%u-%u-%u\n',linedata(b,1),...
               linedata(b,2),(i-1),color,(b-1),(n_points-1));
       end
   end
end
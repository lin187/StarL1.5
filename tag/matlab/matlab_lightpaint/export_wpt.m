function export_wpt(output,INPUT, SPACING, OUTPUT)
n_frames = size(output,2);
fid = fopen(OUTPUT,'w');
fprintf(fid, '%% Waypoints for image: %s\n%% Processed: %s\n%% Name format: [frame #]-[line #]-[point #]-(intersection #)\n\n',INPUT, datestr(now));

fprintf(fid, '%% Metadata:\nWAY,0,0,%u,NUM_FRAMES\nWAY,0,0,%u,SPACING\n',n_frames, SPACING);

for frame = 1:n_frames
    cLines = output(frame).cLines;
    num_lines = size(cLines,1);
    
    ints = [];
    for b=1:num_lines
        ints = [ints;cLines{b,3}];
    end
    intcount = max(ints)+1;

    fprintf(fid, '\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n%% FRAME %u of %u\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n', (frame-1), (n_frames-1));
    % Print the metadata points
    fprintf(fid, 'WAY,0,0,%u,NUM_LINES_IN_FRAME_%u\n',num_lines,(frame-1));
    fprintf(fid, 'WAY,0,0,%u,NUM_INTS_IN_FRAME_%u\n',intcount,(frame-1));
    for i = 1:num_lines
        fprintf(fid, 'WAY,0,0,0,F%uL%uC_%s\n',(frame-1),(i-1),output(frame).cLines{i,4});
    end

    for i = 1:num_lines
       % Print the metadata point for each line segment in this frame
       n_points = size((reshape([cLines{i,1:3}]',[],3)),1);
       fprintf(fid, 'WAY,%u,0,-1,F%u_L%u\n',n_points,(frame-1),(i-1));
    end

    for i = 1:num_lines
       fprintf(fid, '\n%% Line %u\n',(i-1));
       linedata = reshape([cLines{i,1:3}],[],3);

       % Find the number of points in the line, the locations of
       % intersections, and the color value
       n_points = size(linedata,1);
       linedata_ints = find(linedata(:,3) ~= -1);

       for b = 1:n_points
           if ismember(b,linedata_ints)
               fprintf(fid, 'WAY,%u,%u,0,%u-%u-%u-%u\n',linedata(b,1),...
                   linedata(b,2),(frame-1),(i-1),(b-1),linedata(b,3));
           else
               fprintf(fid, 'WAY,%u,%u,0,%u-%u-%u\n',linedata(b,1),...
                   linedata(b,2),(frame-1),(i-1),(b-1));
           end
       end
    end
end

fclose(fid);
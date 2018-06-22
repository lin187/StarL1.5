function export_wpt(cLines,INPUT,OUTPUT)
n_wpt = size(cLines,2);
fid = fopen(OUTPUT,'w');
fprintf(fid, '%% Waypoints for image: %s\n%% Processed: %s\n%% Name format: [Robot #]-[Waypoint #]-[Color]-[Mutex]-[Start]-[End]\n\n',INPUT, datestr(now));

for i=1:n_wpt
    fprintf(fid, 'WAY,%u,%u,0,%u:%u:%s:%d:%u:%u\n',cLines(i).ptx, cLines(i).pty,...
        cLines(i).robot, cLines(i).waypoint, cLines(i).color,...
        cLines(i).mutex, cLines(i).start, cLines(i).end);
end

fclose(fid);
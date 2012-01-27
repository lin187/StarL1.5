function [msg] = server_pack(robots, waypoints)
msg = '';
for i = 1:length(robots)
    msg = [msg '#|' robots(i).name '|' int2str(robots(i).X) '|' int2str(robots(i).Y) '|' int2str(robots(i).yaw) '|&' char(10)];
end
global waypoints_transmitted;
if waypoints_transmitted == 0
    fprintf('Sending all %d waypoints', length(waypoints));
    for i = 1:length(waypoints)
        msg = [msg '@|' waypoints(i).name '|' int2str(waypoints(i).X) '|' int2str(waypoints(i).Y) '|' int2str(waypoints(i).angle) '|&' char(10)];
    end
    waypoints_transmitted = 1;
end
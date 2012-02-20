function [msg] = server_pack(robots, waypoints)
msg = {};
for i = 1:length(robots)
	msg = [msg; [char(10) '#|' robots(i).name '|' int2str(robots(i).X) '|' int2str(robots(i).Y) '|' int2str(robots(i).yaw) '|&']];
end
global waypoints_transmitted;
if waypoints_transmitted == 0
    fprintf('Sending all %d waypoints\n', length(waypoints));
    for i = 1:length(waypoints)
        msg = [msg; [char(10) '@|' waypoints(i).name '|' int2str(waypoints(i).X) '|' int2str(waypoints(i).Y) '|' int2str(waypoints(i).angle) '|&']];
    end
    waypoints_transmitted = 1;
end
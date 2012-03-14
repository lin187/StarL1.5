function [msg] = server_pack_robots(robots)
msg = {};
for i = 1:length(robots)
	msg = [msg; [char(10) '#|' robots(i).name '|' int2str(robots(i).X) '|' int2str(robots(i).Y) '|' int2str(robots(i).yaw) '|&']];
end
function [msg] = server_pack_robots(robots)
global MINIDRONE
global CREATE2
global ARDRONE
msg = {};
for i = 1:length(robots)
    if robots(i).type == CREATE2
        msg = [msg; [char(10) '#|' robots(i).name '|' int2str(robots(i).X) '|' int2str(robots(i).Y) '|' int2str(robots(i).Z) '|' int2str(robots(i).yaw) '|&']];
    elseif robots(i).type == MINIDRONE || robots(i).type == ARDRONE
        msg = [msg; [char(10) '$|' robots(i).name '|' int2str(robots(i).X) '|' int2str(robots(i).Y) '|' int2str(robots(i).Z) '|' int2str(robots(i).yaw) '|' int2str(robots(i).pitch) '|' int2str(robots(i).roll) '|&']];
    end
end
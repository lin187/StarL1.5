function [msg] = server_pack_robots(indeces)

global bots
global numBots

msg = {};
for i = 1:numBots
    if isGroundRobot(bots(i).type) == 1
        msg = [msg; [char(10) '#|' bots(i).name '|' int2str(bots(i).X) '|' int2str(bots(i).Y) '|' int2str(bots(i).Z) '|' int2str(bots(i).yaw) '|&']];
    elseif isAerialDrone(bots(i).type) == 1
        msg = [msg; [char(10) '$|' bots(i).name '|' int2str(bots(i).X) '|' int2str(bots(i).Y) '|' int2str(bots(i).Z) '|' int2str(bots(i).yaw) '|' '0' '|' '0' '|' '&']];
    end
end
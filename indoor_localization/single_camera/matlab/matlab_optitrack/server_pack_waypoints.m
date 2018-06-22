function [msg] = server_pack_waypoints(waypoints)
msg = {};
fprintf('Sending all %d waypoints\n', length(waypoints));
for i = 1:length(waypoints)
    % zero is being sent as index value, because all index values in
    % simulation are set to zero
    msg = [msg; [char(10) '@|' waypoints(i).name '|' int2str(waypoints(i).X) '|' int2str(waypoints(i).Y) '|' int2str(waypoints(i).angle) '|' '0' '|&']];
end
function server_send_waypoints(waypoints)

global IP

[message] = server_pack_waypoints(waypoints);
for i = 1:40:length(message)
    msgToSend = strcat(message{i:min(i+40,length(message))});
    %judp('SEND',4000,'10.255.24.255',int8(msgToSend));
    judp('SEND',4000,IP,int8(msgToSend));
end
end
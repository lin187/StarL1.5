function server_send_udp(bots, waypoints)
message = server_pack(bots, waypoints);
judp('SEND',4000,'192.168.1.255',int8(message));
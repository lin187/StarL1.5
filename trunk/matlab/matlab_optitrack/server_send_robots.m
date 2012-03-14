function server_send_robots(bots)
    [message] = server_pack_robots(bots);
    for i = 1:40:length(message)
        msgToSend = strcat(message{i:min(i+40,length(message))});
        judp('SEND',4000,'192.168.1.255',int8(msgToSend));
    end
end
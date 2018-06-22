function server_send_launch(streams, conns, con_count)
    message = 'GO!';
    for i=1:con_count
        if streams(i) ~= []
            try
                streams(i).writeBytes(char(message));
                streams(i).flush;
            catch
                disp(['-> CONNECTION #' int2str(i) ' LOST!']);
                conns(i).close;
                streams(i) = [];
            end
        end
    end
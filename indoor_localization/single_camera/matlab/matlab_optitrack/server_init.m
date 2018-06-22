function [OS, STREAMS, conns] = server_init(robot_count, output_port, number_of_retries)
    import java.net.ServerSocket
    import java.net.Socket
    import java.io.*

    if nargin == 2
        number_of_retries = 40;
    end
    retry = 0;

    %Set up the server socket and array of empty connection sockets
    server_socket = [];  
    OS(1:robot_count) = Socket();
       
    %Number of connections
    conns = 0;
    
    disp('Starting the server...');
    while((conns < robot_count) && (retry < number_of_retries || number_of_retries == -1))
        retry = retry + 1;
        try
            server_socket = ServerSocket(output_port);
            server_socket.setSoTimeout(1000);
            OS(conns+1) = server_socket.accept;
            conns = conns + 1;
            disp(['Accepted connection #' int2str(conns)]);
            output_stream = OS(conns).getOutputStream;
            STREAMS(conns) = DataOutputStream(output_stream);
        catch
            if ~isempty(server_socket)
                server_socket.close
            end
            OS(conns+1).close
        end
        %pause(1);
    end
    
    %Close the server once all robots have connected or timed out
    server_socket.close
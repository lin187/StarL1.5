function server_shutdown(connections, connection_count)
for i=1:connection_count
   connections(i).close 
end
disp('All connections closed');
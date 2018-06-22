function save_robot_data(bots, handle)
for i = 1:length(bots)
    fprintf(handle, '%f,%s,%d,%d,%d,%d\r\n',now, bots(i).name, bots(i).X,...
        bots(i).Y, bots(i).Z, bots(i).hyst);
end
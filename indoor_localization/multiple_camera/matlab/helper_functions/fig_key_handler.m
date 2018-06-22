function fig_key_handler(src,event)
global send_launch;
global disp_waypoints;
global waypoints_transmitted;
global disp_waypoint_names;
global clear_history;

disp(event.Character);
    if event.Character == 'w'
        disp('Toggling waypoint viewing');
        disp_waypoints = ~disp_waypoints;
    end
    
    if event.Character == 'n'
        disp('Toggling waypoint names');
        disp_waypoint_names = ~disp_waypoint_names;
    end
    
    if event.Character == 's'
        waypoints_transmitted = 0;
    end
    
    if event.Character == 'a'
        send_launch = -1;
    end
    
    if event.Character == 'c'
        clear_history = 1;
    end
    
    if event.Character == 'l'
        if send_launch == 0
            disp('Sending launch command!');
            send_launch = 1;
        end
    end
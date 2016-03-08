classdef MiniDrone
    properties
        center % x,y center of drone (could extend to height as well)
        radius
        depth
        depths = []
        BBox % BBox for drone
        centers = [];
        radii = [];
        Vxs = []; 
        Vys = []; 
        Vxsfilt = [];
        Vysfilt = [];
        time_localized; % date vector with time the drone was localized
        Vx; % Drone's x velocity, px/s
        Vy; % Drone's y velocity, px/s
        Ex; % Drone's x distance from goal 
        Ey; % Drone's y distance from goal
        roll;
        pitch;
        XCommands = [];
        YCommands = [];
        prevEx = 0;
        prevEy = 0;
        cumErrorX = 0;
        cumErrorY = 0;
        destinations;
        dest_i = 1;
        color; % this is currently assigned based on position and corresponds to prop color (b or w)
        %centers % x,y centers of four circles for use with circle tracking
        %color % string identifying color for use with color tracking
        % maybe add ip address
        % maybe add object that sends UDP commands
    end
end
        
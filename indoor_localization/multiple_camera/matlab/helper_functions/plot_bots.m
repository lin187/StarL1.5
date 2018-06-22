function plot_bots(fig, LINE_LEN, X_MAX, Y_MAX, bots, waypoints, walls, disp_waypoints, disp_waypoint_names)
global mm_per_pixel
sfigure(fig);
n_wpt = length(waypoints);
n_wal = length(walls);
robot_count = size(bots,2);
origin = 2;

%% Plot the waypoints and robot centers
clf(fig);
hold on;
title('Window')

plot([bots.X], [bots.Y],'ro','MarkerFaceColor','r');
if n_wpt > 0 && disp_waypoints == 1
    plot([waypoints.X], [waypoints.Y], 'bo');
end
hold off;

%% Print waypoint names
if n_wpt > 0 && disp_waypoint_names == 1
    for i = 1:n_wpt
        text(double(waypoints(i).X-120),double(waypoints(i).Y-80),waypoints(i).name,'Color','b');
    end
end


%% Plot the walls
for i = 1:n_wal
   line([walls(i).X1 walls(i).X2],[walls(i).Y1 walls(i).Y2], 'LineWidth',2,'Color','k');
end

%% Plot the line, circle, and name
for i = 1:robot_count
    line([bots(i).X cosd(bots(i).yaw)*LINE_LEN+bots(i).X],[bots(i).Y sind(bots(i).yaw)*LINE_LEN+bots(i).Y],'Color','k');
    rectangle('Position',[bots(i).X-LINE_LEN, bots(i).Y-LINE_LEN, 2*LINE_LEN, 2*LINE_LEN],'Curvature',[1 1],'LineWidth',1);
    text(double(bots(i).X-LINE_LEN),double(bots(i).Y-LINE_LEN/.65),bots(i).name)
    %If the robot is not currently visible, mark it
    if bots(i).hyst > 0
       rectangle('position',[bots(i).X-LINE_LEN, bots(i).Y-LINE_LEN, 2*LINE_LEN, 2*LINE_LEN],'LineWidth',1,'EdgeColor','r');
    end
end
if origin == 1
    axis([0 X_MAX 0 Y_MAX]);
else
    axis([-960*mm_per_pixel 960*mm_per_pixel -540*mm_per_pixel, mm_per_pixel*540]); %THIS NEEDS TO BE FIXED!!!!
end
drawnow
end

    
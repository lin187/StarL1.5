function [X,Y,Z,roll,pitch,yaw] = track_getTrackablePosition(tracknum)
X = 0;Y = 0;Z = 0;
qx = 0;qy = 0;qz = 0;qw = 0;
yaw = 0;pitch = 0;roll = 0;

[Xq,Yq,Zq,~,~,~,~,yawq,~,~] = calllib('NPTrackingTools', 'TT_TrackableLocation',tracknum,X,Y,Z,qx,qy,qz,qw,yaw,pitch,roll);

if ~isnan(Xq)
    X = Xq;
end

if ~isnan(Yq)
    Y = Yq;
end

if~isnan(Zq)
    Z = Zq;
end

if ~isnan(yawq)
    yaw = yawq;
end
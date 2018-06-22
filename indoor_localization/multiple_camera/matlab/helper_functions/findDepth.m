function depth = findDepth(radius, type)

global MINIDRONE
global CREATE2
global ARDRONE
global THREEDR
global GHOST2
global MAVICPRO
global PHANTOM3
global PHANTOM4

if type == MINIDRONE
    depth = 0.5616*(radius^2) + -105.2*(radius) + 6468;
else
    depth = 0;
end

end
function [ ] = depthImageCollectionCallback( src, msg, kinectNum )
% Author: Nate Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%  
% Purpose: This function respond to a Kinect sending a depth image message.

%% Declare global variables
global kinect_number
global depthMsgs

%% Collect the message
% If kinect_number is greater than or equal to the kinectNum, then the message
% should not be copied because it is either not needed or it could
% overwrite what is already being read
if kinect_number < kinectNum
    depthMsgs(kinectNum) = msg;
end

end


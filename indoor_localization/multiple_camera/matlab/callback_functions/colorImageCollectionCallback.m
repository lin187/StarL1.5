function [ ] = colorImageCollectionCallback( src, msg, cameraNum )
% Author: Nate Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%  
% Purpose: This function respond to a Camera sending a color image message.

%% Declare global variables
global camera_number
global colorMsgs

%% Collect the message
% If camera_number is greater than or equal to the cameraNum, then the message
% should not be copied because it is either not needed or it could
% overwrite what is already being read
% disp('I am trying...')
if camera_number < cameraNum
    colorMsgs(cameraNum) = msg;
%     disp('I Read an Image!!!!!')
end

end


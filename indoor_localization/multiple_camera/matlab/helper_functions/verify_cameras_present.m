function [ found, cameraTags ] = verify_cameras_present( cameraID_list )
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose: This function parses through the ROS topics available to make 
%          sure all the listed Cameras are broadcasting.  

%% Declare global variables
global numCameras

%% Initialize variables
found = false;
numFound = 0;
cameraTags = strings(1,numCameras);

%% Search through the ROS nodes
% In order to verify each Camera listed in the input is broadcasting two
% image messages
% Acquire the list of topics
topics = rostopic('list')';

% Split the Camera list into their individual information
splitList = strsplit(cameraID_list, ',');

% For each Camera, verify that it has an associated topic
for i = 1:numCameras
    % Split the individual information into its parts: ID:X:Y 
    cameraInfo = strsplit(splitList(i),':');
    % Create a tag to look for using the ID of the form /camera#/
    cameraTags(i) = strcat('/', 'camera', cameraInfo(1),'/');
    % Search for the tag among the topics
    TF = contains(topics,cameraTags(i));
    % If there are any instances of the tag, then we can assume it is being
    % published to and count it as present
    if sum(TF) >= 1
       numFound = numFound + 1;
    else
        fprintf('Missing %s\n',cameraTags(i));
    end
end

%% Verify that all the Cameras were found
if numFound == numCameras
    found = true;
end
% If not all were found, then the function will return false

end
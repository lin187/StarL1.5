function [ found, kinectTags ] = verify_kinects_present( kinectID_list )
% Author: Nathaniel Hamilton
%  Email: nathaniel.p.hamilton@vanderbilt.edu
%
% Purpose: This function parses through the ROS topics available to make 
%          sure all the listed Kinects are broadcasting.  

%% Declare global variables
global numKinects

%% Initialize variables
found = false;
numFound = 0;
kinectTags = strings(1,numKinects);

%% Search through the ROS nodes
% In order to verify each Kinect listed in the input is broadcasting two
% image messages
% Acquire the list of topics
topics = rostopic('list')';

% Split the Kinect list into their individual information
splitList = strsplit(kinectID_list, ',');

% For each Kinect, verify that it has 2 topics associated
for i = 1:numKinects
    % Split the individual information into its parts: ID:X:Y 
    kinectInfo = strsplit(splitList(i),':');
    % Create a tag to look for using the ID of the form /kinect#/
    kinectTags(i) = strcat('/', 'kinect', kinectInfo(1),'/');
    % Search for the tag among the topics
    TF = contains(topics,kinectTags(i));
    % If there are 2 instances of the tag, then there are 2 associated
    % topics and we can count it as present
    if sum(TF) >= 2
       numFound = numFound + 1;
    else
        fprintf('Missing %s\n',kinectTags(i));
    end
end

%% Verify that all the Kinects were found
if numFound == numKinects
    found = true;
end
% If not all were found, then the function will return false

end
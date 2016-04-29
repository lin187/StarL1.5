% CHANGE THE
% FILENAME!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
% !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
global imgColorPlotted
% this script will make a video. It uses imgColorPlotted, which is created
% in plotAfterRunning, so you need to run that file first. Make sure you
% change the filename or you will overwrite your last video.
directory = 'c:\dropbox\kinect\Kinect_Videos\parrot_mini_drone_videos\';
fname = 'follow_two_drones.avi';
firstFrame = 1;
lastFrame = frameCount - offset;
frameRate = 15;
imgData = imgColorPlotted;
toSave = [directory fname];
videoMatlabToAvi(toSave, imgData, firstFrame, lastFrame, frameRate);
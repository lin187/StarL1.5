% CHANGE THE
% FILENAME!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
% !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
global imgColorPlotted
% this script will make a video. It uses imgColorPlotted, which is created
% in plotAfterRunning, so you need to run that file first. Make sure you
% change the filename or you will overwrite your last video.
directory = 'C:\Users\trandh\Desktop\Kinect_Videos\';
fname = 'follow_three_drones_no_crossing_paths.avi';
firstFrame = 1;
lastFrame = frameCount - offset;
frameRate = 15;
imgData = imgColorPlotted;
toSave = [directory fname];
videoMatlabToAvi(toSave, imgData, firstFrame, lastFrame, frameRate);
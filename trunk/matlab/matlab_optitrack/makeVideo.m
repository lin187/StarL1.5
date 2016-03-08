global imgColorPlotted
firstFrame = 1;
lastFrame = frameCount - offset;
frameRate = 15;
imgData = imgColorPlotted;
directory = 'c:\dropbox\kinect\Kinect_Videos\parrot_mini_drone_videos\';
fname = 'followApp_two_drones.avi';
toSave = [directory fname];
videoMatlabToAvi(toSave, imgData, firstFrame, lastFrame, frameRate);
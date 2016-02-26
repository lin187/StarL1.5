firstFrame = 1;
lastFrame = frameCount - offset;
frameRate = 15;
imgData = imgColorPlotted;
directory = 'c:\dropbox\kinect\Kinect_Videos\iRobotCreate2_videos\';
fname = 'flocking_translation_and_rotation.avi';
toSave = [directory fname];
videoMatlabToAvi(toSave, imgData, firstFrame, lastFrame, frameRate);
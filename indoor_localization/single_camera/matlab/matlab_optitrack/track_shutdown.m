function track_shutdown()
calllib('NPTrackingTools', 'TT_FinalCleanup');
unloadlibrary('NPTrackingTools');
disp('Complete!');
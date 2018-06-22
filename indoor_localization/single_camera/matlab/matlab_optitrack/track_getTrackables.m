function [trackCount, trackNames] = track_getTrackables()
trackCount = calllib('NPTrackingTools','TT_TrackableCount');
trackNames = cell(1,trackCount);

for i = 0:trackCount-1
    trackNames{i+1} = calllib('NPTrackingTools','TT_TrackableName',i);
end
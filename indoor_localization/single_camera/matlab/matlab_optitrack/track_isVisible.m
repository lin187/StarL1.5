function [is_vis] = isVisible(tracknum)
is_vis = calllib('NPTrackingTools','TT_IsTrackableTracked',tracknum);
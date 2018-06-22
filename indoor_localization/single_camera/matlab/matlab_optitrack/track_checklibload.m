function track_checklibload()
%Crash if the library isnt loaded
if ~libisloaded('NPTrackingTools')
	assert(false)
end

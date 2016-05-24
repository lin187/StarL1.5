% write matlab video object created with kinect to a file
% example:
%   videoMatlabToAvi('test.avi', colorFrameData)
function videoMatlabToAvi(filename, colorFrameData, firstFrame, lastFrame, frameRate)
    vw = VideoWriter(filename);
    vw.FrameRate = frameRate;
    open(vw);
    
    for i =  firstFrame:lastFrame
        frame = colorFrameData(:,:,:,i);
        writeVideo(vw, frame);
    end
    close(vw);
end
function [framed_lines color_holder] = separate_frames(lines, colors, SAFE_TRAVEL_RADIUS,SPACING, MAX_FRAMES)
framed_lines = ones([size(lines) MAX_FRAMES])*-1;
frame_marker = ones(MAX_FRAMES,1);
cur = zeros(4,1);
num_lines = size(lines,1);

color_holder = cell(size(lines,1), MAX_FRAMES);
color_holder(:,1) = colors;

framed_lines(:,:,1) = lines;

for curframe = 1:MAX_FRAMES-1
    fprintf('Checking contents of frame #%u\n',curframe);
    for i=1:num_lines
        if lineExists(framed_lines(i,:,curframe))
            % Place this line in the current frame
            cur = framed_lines(i,:,curframe);

            % Compare this line to subsequent lines
            for b = (i+1):num_lines
                if lineExists(framed_lines(b,:,curframe))
                    %fprintf('Checking between lines %u and %u - ',i,b);
                    if get_line_distance(cur,framed_lines(b,:,curframe),SPACING) < SAFE_TRAVEL_RADIUS
                        % If the subsequent line is too close, push it to the
                        % next frame
                        framed_lines(frame_marker(curframe+1),:,curframe+1) = framed_lines(b,:,curframe);
                        framed_lines(b,:,curframe) = [-1 -1 -1 -1];
                        
                        
                        color_holder(frame_marker(curframe+1),curframe+1) = color_holder(b,curframe);
                        color_holder(b,curframe) = {-1};
                        
                        frame_marker(curframe+1) = frame_marker(curframe+1) + 1;
                    end
                end
            end
        end    
    end
end

function exists = lineExists(line)
    exists = ~isequal(line,[-1 -1 -1 -1]);
        
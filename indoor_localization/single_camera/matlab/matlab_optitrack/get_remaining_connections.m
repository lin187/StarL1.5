function [i] = get_remaining_connections(streams)
i = 0;
for b = 1:length(streams)
    if streams(b) ~= []
        i = i + 1;
    end
end
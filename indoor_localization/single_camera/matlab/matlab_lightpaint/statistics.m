function [l_ghosts l_lines] = statistics(lines, ghosts)
    l_ghosts = 0;
    l_lines = 0;
    for i = 1:size(lines,1)
        line_len = p_dist(lines(i,1:2),lines(i,3:4));
        l_ghosts = l_ghosts + ghosts(i)*line_len;
        l_lines = l_lines + (~ghosts(i))*line_len;
    end
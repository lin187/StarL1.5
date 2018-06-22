function close_fig_handler()
    global fig_closed;
    disp('Exiting because you closed the figure');
    close all;
    fig_closed = 0;
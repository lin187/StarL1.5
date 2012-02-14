function plot_lines(cLines, lines, ghosts, frame_num, n_frames)
figure(1);
subplot(1,n_frames,frame_num);
hold on
gi = find(ghosts);
li = find(ghosts == 0);

line([lines(li,1)';lines(li,3)'],[lines(li,2)';lines(li,4)'],'Color','r','Marker','.');
line([lines(gi,1)';lines(gi,3)'],[lines(gi,2)';lines(gi,4)'],'Color','b');
linedata = reshape([cLines{:,:}]',[],3);
linedata_ints = find(linedata(:,3) ~= -1);
plot(linedata(linedata_ints,1),linedata(linedata_ints,2),'gs');
plot(linedata(:,1),linedata(:,2),'k.');
axis([0 3450 0 3700])
title(['Frame ' int2str(frame_num)], 'FontWeight', 'bold');
hold off;

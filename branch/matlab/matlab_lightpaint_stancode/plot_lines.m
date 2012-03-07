function plot_lines(cLines, lines, frame_num, n_frames)
gi = [];
li = [];

linedata = zeros(0,2);
linedata_ints = [];

for i=1:size(cLines,1)
   if isequal('000000', cLines{i,4})
       gi = [gi i];
   else
       li = [li i];
   end
   
   linedata = [linedata;cLines{i,1} cLines{i,2}];
   ldi = cLines{i,3};
   linedata_ints = [linedata_ints;cLines{i,3};];
end



figure(1);
subplot(1,n_frames,frame_num);
hold on
line([lines(li,1)';lines(li,3)'],[lines(li,2)';lines(li,4)'],'Color','r','Marker','.');
line([lines(gi,1)';lines(gi,3)'],[lines(gi,2)';lines(gi,4)'],'Color','b');

linedata_ints = find(linedata_ints ~= -1);
plot(linedata(linedata_ints,1),linedata(linedata_ints,2),'gs');
plot(linedata(:,1),linedata(:,2),'k.');
axis([0 3450 0 3700])
title(['Frame ' int2str(frame_num)], 'FontWeight', 'bold');
hold off;

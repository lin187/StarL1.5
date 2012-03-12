function plot_lines(cLines, lines, ghosts)
figure(1);
clf;
hold on
line([lines(:,1)';lines(:,3)'],[lines(:,2)';lines(:,4)'],'Color','r','Marker','.');
line([ghosts(:,1)';ghosts(:,3)'],[ghosts(:,2)';ghosts(:,4)'],'Color','b','Marker','.');

for i=1:size(cLines,2)
    plot(cLines(i).ptx,cLines(i).pty,'k.');
    if(cLines(i).mutex ~= -1)
        plot(cLines(i).ptx,cLines(i).pty,'gs');
    end
    if(cLines(i).start == 1)
        plot(cLines(i).ptx,cLines(i).pty,'ro');
        text(cLines(i).ptx+50,cLines(i).pty,['Start ' num2str(cLines(i).robot)]);
    end
    if(cLines(i).end == 1)
        plot(cLines(i).ptx,cLines(i).pty,'bo');
        text(cLines(i).ptx+50,cLines(i).pty,['End ' num2str(cLines(i).robot)]);
    end
end
axis([0 3450 0 3700])
hold off;

function midx = robot_has_moved(bots)
nbots = size(bots,2);
midx = [];

for i=1:nbots
   hidx = max(1,bots(i).hist_index-5);
   motion = sqrt(sum(([bots(i).X bots(i).Y]-bots(i).history(hidx,:)).^2));   
   amot = abs(bots(i).yaw-bots(i).histangle(hidx));
   if(motion > 1.42 || amot > 1)
       midx = [midx,i];     
       %fprintf('Movement %u: [%f] %f\n',i, motion, amot);
   end
end
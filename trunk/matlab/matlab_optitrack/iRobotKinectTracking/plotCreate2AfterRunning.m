showCoordinates = 1;
offset = frameCount - length(botArray(1).centers);
for i = offset + 1:frameCount - offset
    plotCreate2(imgColorAll(:,:,:,i), botArray, 2, i - offset, showCoordinates);
end
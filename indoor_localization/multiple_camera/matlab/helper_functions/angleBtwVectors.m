function angle = angleBtwVectors(A, B)
% this function returns the angle between two vectors with correct sign
AdotB = dot(A,B);
AdetB = det([A;B]);
angle = atan2(AdotB, AdetB);
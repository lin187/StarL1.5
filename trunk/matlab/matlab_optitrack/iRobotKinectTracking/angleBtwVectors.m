function angle = angleBtwVectors(A, B)
AdotB = dot(A,B);
AdetB = det([A;B]);
angle = atan2(AdotB, AdetB);
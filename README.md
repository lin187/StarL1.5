StarL1.5
========
This is the master branch for development, for stable release, please select branch "1.5"

StarL (Stabilizing Robotics Programming Language) is a programming library for developing distributed robotic algorithms.

See our research wiki:
https://wiki.cites.illinois.edu/wiki/display/MitraResearch/StarL

StarL1.5 with 5 demo applications
See getStarted.pdf for detailed instructions on how to set up StarL on both ADT and Android Studio.

How to set up StarL
1. Download and install the JDK1.7 and JRE1.7 at 
http://www.oracle.com/technetwork/java/javase/downloads/index.html

2. Download and install the latest Android Studio or ADT. 
http://developer.android.com/sdk/index.html
(We just migrated to Android Studio, please contact us if you are encountering problems)

3. Download and install git for source control

4. Get the latest version of the code from github.

5. Install the latest SDK (API21) using the SDK manager, if you are having problems, try install SDK 2.3.3 (API 10). If you are using Android Studio, set the language level to 7.0 Diamonds, ARM, multi-catch etc.

6. Import project.

7. Select the folder containing the StarL source code as the root directory. A number of
items will appear in the Projects list. Select All. The starlLib and starlSim are core of StarL; RaceApp, MazeApp, TrafficSignApp, DistributedSearchApp, LightPaintingSim are StarL simulation applications, LightPaintLib is extension to StarLib for the LightPaint application. The LightPaintApp and StarLtemplete are android applications.

8. For ADT users, if compiler errors remain, in each project Properties dialog in the Java Compiler section, ensure that the Enable project specific settings checkbox is unchecked

Now you should be ready to run StarL!

Any questions? Please contact Yixiao Lin at lin187@illinois.edu

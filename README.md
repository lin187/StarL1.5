StarL 1.5.1
========

This is the API development branch. This version of StarL uses a different build tool, Gradle, as required by several of the SDKs integrated in this branch.

Deployment directions:

1. Clone the repo and do a git checkout for the APIdev branch 
2. Uninstall the app on the tablet
3. Clean Project
4. Rebuild project 
5. Run on a device


Specific changes:

Gradle
========
https://en.wikipedia.org/wiki/Gradle

https://gradle.org/

DJI SDK 
========

![](https://media.giphy.com/media/xE8TXvuMhQrxC/giphy.gif)

stirling.h.carter@vanderbilt.edu

timothy.liang@vanderbilt.edu

Compatible with Mavic and Phantom DJI drones.
The tablet must be connected to the remote controller of the drone via USB or WiFi bridge app.

The API is implemented in the DjiController.java class and there are separate Model and MotionAutomaton classes for the Mavic and Phantom.

Ehang SDK 
========
@Christina @Anissa

3DR SDK
========
@Austin @Mark


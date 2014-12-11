StarL1.5
========

StarL1.5 with 5 demo applications

How to set up StarL
1. Download and install the JDK1.7 at 
http://www.oracle.com/technetwork/java/javase/downloads/index.html

This step is recommended to set up the java environment correctly.
It is also recommended to download and install the latest version of JRE from the same web page.

2. Download and install the latest ADT. 
http://developer.android.com/sdk/installing/bundle.html
(we are currently mirgrating to Android Studio, please use ADT for now)

This is required to run or to make changes to starL.

3. Download and install github for source control

4. Get the latest version of the code from github.

5. Open Eclipse in the ADT bundle, select a workspace. The workspace serves as a buffer between your development to your version of the StarL. The directory can be anywhere as long as you can retrieve it later.

6. In Eclipse, select File > Import. . . . In the import dialog, under the General category,
select Existing Projects into Workspace. Click Next.

7. Select the folder containing the StarL source code as the root directory. A number of
items will appear in the Projects list. Select starlLib, starlSim, RaceApp, MazeApp, TrafficSignApp, DistributedSearchApp, LightPaintingLib, LightPaintingSim.

Some error will present as now you have not install SDK for Android phones.

8. Add SDK by using the SDK Manager
Choose the appropriate version (API 10, Android2.3.3)
More steps can be found here:
http://developer.android.com/sdk/installing/adding-packages.html

9. If compiler errors remain, in each project’s Properties dialog in the Java Compiler
section, ensure that the Enable project specific settings checkbox is unchecked

Now you should be ready to run StarL!

# HAL2020-ATaing

Welcome to my GitHub!  This repository is for the models of endothelial cell growth with Heparin-MAP particles I've made at my time here at Griffin Lab.  All code on this repo uses the HAL library, and can be installed below:

http://halloworld.org/start_here.html

Getting the model working:

1) Install IntelliJ
2) Install the HAL library
2) Clone the repository (i.e. download the zip file by clicking the green "Code" button) and extract the files
3) From the HAL library, copy the file HAL and HalColorSchemes.jar and place them within the AngiogenesisModel file together with the other files in the repository
4) Open IntelliJ, ctrl + shift + a to bring up the search menu and find "Import Project from Existing Sources" then select AngiogenesisModel. (If currently in another project and not on IntelliJ "recent projects page", select File, then close project to be brought back to the project selection window.)
5) Click accept for default options. NOTE: IntelliJ should recognize the libraries "lib" and "HalColorSchemes" automatically.  If not, then they will have to be manually added as libraries afterwards.  (i.e. at the end of the tutorial, open the Project, find File, Project Structure, Libraries, then click the plus sign and find and add the file "lib" and the file "HalColorSchemes.jar" as libraries separately)
NOTE: If IntelliJ warns about overriding the .idea file, select OK.
6) If there are other errors from unprovided libraries, these will have to be dealt with on a case-by-case basis.
7) Open HeparinGrid.java in IntelliJ and click "Add Configuration" located in the top right corner between the hammer button and the play button
8) In the opened window, click the plus sign in the upper left corner to open a dropdown menu. Select application.
9) Name the application (suggested, HeparinGrid)
10) Under the "Build and Run" header, select select the main class called "VascularModel.woundGrid_2D" by clicking on the file icon at the end of the first input box.
11) Select OK. Now the program can be run by selecting the play button in the top right corner.

Enjoy!

Fall 2020

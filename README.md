# HAL2020-ATaing

Welcome to my GitHub!  This repository is for the models of endothelial cell growth with Heparin-MAP particles I've made at my time here at Griffin Lab.  All code on this repo uses the HAL library, and can be installed below:

http://halloworld.org/start_here.html

Getting the model working:

1) Install IntelliJ
2) Install the HAL library
2) Clone the repository (i.e. download the zip file by clicking the green "Code" button) and extract the files
3) From the HAL library, copy the file HAL and HalColorSchemes.jar and place them within the AngiogenesisModel file together with the other files in the repository
4) Open IntelliJ, ctrl + shift + a to bring up the search menu and find "Import Project from Existing Sources" then select AngiogenesisModel. (If currently in another project and not on IntelliJ "recent projects page", select File, then close project to be brought back to the project selection window.)
5) Click accept for default options. NOTE: If IntelliJ warns about overriding the .idea file, select OK.
6) Open any .java in the repository using IntelliJ and select "File" then "Project Structure" then "Libraries"
7) Select the minus button above the long vertical columns to delete all the current libraries. Click OK on the warnings.  Then, select the plus sign, select Java, and navigate to the ...HAL\lib file and select OK.
8) Repeat the last part of step 7 with HalColorSchemes.jar.  You may get an extra popup box.  Here you will want to select "jar Directory"
NOTE: this may seem like you just added the libraries you removed, but these steps eliminate an error with programs that use the OpenGL3DWindow class.
9) Select Apply, then OK.
10) Click "Add Configuration" located in the top right corner between the hammer button and the play button
11) In the opened window, click the plus sign in the upper left corner to open a dropdown menu. Select application.
12) Name the application (suggested, HeparinGrid)
13) Under the "Build and Run" header, select select the main class called "VascularModel.HeparinGrid" by clicking on the file icon at the end of the first input box.
14) Select OK. Now the program can be run by selecting the play button in the top right corner.

Enjoy!

Fall 2020
MINDdroidCV
===========

Controlling LEGO Mindstorms NXT using OpenCV on Android - Workaround for the OpenCV 1244 bug.

Original by Richárd Szabó. Modifications by Miguel Angel Astor and David Perez Abreu.

Modifications
=============
This fork implements a workaround that allows MINDdroidCV to run on devices affected by the OpenCV
project's 1244 bug (http://code.opencv.org/issues/1244).

The workaround has been tested to work with the Motorola Milestone smartphone. Other devices may
require fine tuning of the code. Aditionally, this fork includes code to check sensor information
for a particular type of robot proposed by Carlos Tovar and Paolo Tosiani on their grad thesis work.

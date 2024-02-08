# Augmented Reality simple shape detector using OpenCV 4
[![Android CI](https://github.com/michaeltroger/shape-detection-android/actions/workflows/android.yml/badge.svg)](https://github.com/michaeltroger/shape-detection-android/actions/workflows/android.yml)

Attention: This app was created in 2016. I was a beginner to Android development and Computer Vision back then.
So don't expect a perfect code please. Over the years I updated the dependencies and converted it to Kotlin, while the business logic remained unchanged.

Note: Originally I targeted min SDK 15 and OpenCV 3 with this project, nowadays the repo uses newer versions. If the old versions is something that you need, then you can look back in the repo's Git history (app version 1.2)

<img src="/screenshots/demo.gif" alt="Augmented Reality shape detection" width="800px"/>

### What is this repository for? ###

* Uses the camera image to recognize triangles, rectangles and circles. It can also be configured to only detect a certain color (red). If a specific shape is detected the information can be shown on top of each shape as a label describing it (multiple shapes at the same time) or as an image respresenting the shape on top of the camera (only one shape is detected). Additionally also a sound depending on the detected shape can be played.
* More computer vision projects at https://michaeltroger.com/computervision/

### How do I get set up? ###

* IDE: Android Studio (tested with 2023.3.1)
* Android SDK
* Images location: res/drawable | Sounds location: res/raw
* Mode (label/image): Flag in MainActivity (default labels)
* Mode (all colors / red only): Flag in MainActivity (default all colors)

### Test images ###
<img src="/testimages/circles.jpg" alt="" width="400px"/>

<img src="/testimages/simpleshapes.png" alt="" width="600px"/>

### Author ###
[Michael Troger](https://michaeltroger.com)

### Credits ###
* The shape detection is based on Nash's shape detection https://github.com/bsdnoobz/opencv-code/blob/master/shape-detect.cpp His version is based on OpenCV 2 for the PC and usable for static images files. He extracted the code from the OpenCV tutorials http://opencv-code.com back then - that website is no longer available though.
* The red color detection is based on Sol's OpenCV red circle detection. See https://github.com/sol-prog/OpenCV-red-circle-detection and https://solarianprogrammer.com/2015/05/08/detect-red-circles-image-using-opencv/

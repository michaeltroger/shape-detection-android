# Augmented Reality simple shape detector using OpenCV for >= Android 4.0 #
[![Android CI](https://github.com/michaeltroger/shape-detection-android/actions/workflows/android.yml/badge.svg)](https://github.com/michaeltroger/shape-detection-android/actions/workflows/android.yml)

Attention: This app was created in 2016. I was a beginner to Android development and Computer Vision back then.
So don't expect a perfect code please. In 2021 I updated the project to build with the latest Android Studio (2020.3.1), updated most dependencies and converted it to Kotlin, while the business logic remained unchanged.

<img src="/screenshots/demo.gif" alt="Augmented Reality shape detection" width="800px"/>

### What is this repository for? ###

* Uses the camera image to recognize triangles, rectangles and circles. It can also be configured to only detect a certain color (red). If a specific shape is detected the information can be shown on top of each shape as a label describing it (multiple shapes at the same time) or as an image respresenting the shape on top of the camera (only one shape is detected). Additionally also a sound depending on the detected shape can be played.

### How do I get set up? ###

* IDE: Android Studio (tested with 2020.3.1)
* Android SDK
* Dependencies: OpenCV 3 library (included) [License](/opencv-3-4-15/LICENSE)
* Images location: res/drawable | Sounds location: res/raw
* Mode (label/image): Flag in MainActivity (default labels)
* Mode (all colors / red only): Flag in MainActivity (default all colors)

### Test images ###
<img src="/testimages/circles.jpg" alt="" width="400px"/>

<img src="/testimages/simpleshapes.png" alt="" width="600px"/>

### Who do I talk to? ###
* Repo owner and developer: android@michaeltroger.com

### Credits ###
* The shape detection is based on Nash's shape detection https://github.com/bsdnoobz/opencv-code/blob/master/shape-detect.cpp His version is based on OpenCV 2 for the PC and usable for static images files.
* The red color detection is based on Sol's OpenCV red circle detection. See https://github.com/sol-prog/OpenCV-red-circle-detection and https://solarianprogrammer.com/2015/05/08/detect-red-circles-image-using-opencv/

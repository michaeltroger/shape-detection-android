package com.michaeltroger.shapedetection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.michaeltroger.shapedetection.views.OverlayView;

/**
 * the main activity - entry to the application
 */
public class MainActivity extends Activity implements CvCameraViewListener2 {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = MainActivity.class.getName();
    /**
     * the camera view
     */
    private CameraBridgeViewBase mOpenCvCameraView;
    /**
     * for displaying Toast info messages
     */
    private Toast toast;
    /**
     * responsible for displaying images on top of the camera picture
     */
    private OverlayView overlayView;
    /**
     * whether or not to log the memory usage per frame
     */
    private static final boolean LOG_MEM_USAGE = true;
    /**
     * detect only red objects
     */
    private static final boolean DETECT_RED_OBJECTS_ONLY = true;
    /**
     * the lower red HSV range (lower limit)
     */
    private static final Scalar HSV_LOW_RED1 = new Scalar(0, 100, 100);
    /**
     * the lower red HSV range (upper limit)
     */
    private static final Scalar HSV_LOW_RED2 = new Scalar(10, 255, 255);
    /**
     * the upper red HSV range (lower limit)
     */
    private static final Scalar HSV_HIGH_RED1 = new Scalar(160, 100, 100);
    /**
     * the upper red HSV range (upper limit)
     */
    private static final Scalar HSV_HIGH_RED2 = new Scalar(179, 255, 255);
    /**
     * definition of RGB red
     */
    private static final Scalar RGB_RED = new Scalar(255, 0, 0);
    /**
     * frame size width
     */
    private static final int FRAME_SIZE_WIDTH = 640;
    /**
     * frame size height
     */
    private static final int FRAME_SIZE_HEIGHT = 480;
    /**
     * whether or not to use a fixed frame size -> results usually in higher FPS
     * 640 x 480
     */
    private static final boolean FIXED_FRAME_SIZE = true;
    /**
     * whether or not to use the database to display
     * an image on top of the camera
     * when false the objects are labeled with writing
     */
    private static final boolean DISPLAY_IMAGES = false;
    /**
     * image thresholded to black and white
     */
    private Mat bw;
    /**
     * image converted to HSV
     */
    private Mat hsv;
    /**
     * the image thresholded for the lower HSV red range
     */
    private Mat lowerRedRange;
    /**
     * the image thresholded for the upper HSV red range
     */
    private Mat upperRedRange;
    /**
     * the downscaled image (for removing noise)
     */
    private Mat downscaled;
    /**
     * the upscaled image (for removing noise)
     */
    private Mat upscaled;
    /**
     * the image changed by findContours
     */
    private Mat contourImage;
    /**
     * the activity manager needed for getting the memory info
     * which is necessary for getting the memory usage
     */
    private ActivityManager activityManager;
    /**
     * responsible for getting memory information
     */
    private ActivityManager.MemoryInfo mi;
    /**
     * the found contour as hierarchy vector
     */
    private Mat hierarchyOutputVector;
    /**
     * approximated polygonal curve with specified precision
     */
    private MatOfPoint2f approxCurve;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    bw = new Mat();
                    hsv = new Mat();
                    lowerRedRange = new Mat();
                    upperRedRange = new Mat();
                    downscaled = new Mat();
                    upscaled = new Mat();
                    contourImage = new Mat();

                    hierarchyOutputVector = new Mat();
                    approxCurve = new MatOfPoint2f();

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_my);
        // get the OverlayView responsible for displaying images on top of the camera
        overlayView = (OverlayView) findViewById(R.id.overlay_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        // Michael Troger
        if (FIXED_FRAME_SIZE) {
            mOpenCvCameraView.setMaxFrameSize(FRAME_SIZE_WIDTH, FRAME_SIZE_HEIGHT);
        }
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mi = new ActivityManager.MemoryInfo();
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();


        if(toast != null)
            toast.cancel();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if (LOG_MEM_USAGE) {
            activityManager.getMemoryInfo(mi);
            long availableMegs = mi.availMem / 1048576L; // 1024 x 1024
            //Percentage can be calculated for API 16+
            //long percentAvail = mi.availMem / mi.totalMem;
            Log.d(TAG, "available mem: " + availableMegs);
        }

        // get the camera frame as gray scale image
        Mat gray = null;

        if (DETECT_RED_OBJECTS_ONLY) {
            gray = inputFrame.rgba();
        } else {
            gray = inputFrame.gray();
        }


        // the image to output on the screen in the end
        // -> get the unchanged color image
        Mat dst = inputFrame.rgba();

        // down-scale and upscale the image to filter out the noise
        Imgproc.pyrDown(gray, downscaled, new Size(gray.cols()/2, gray.rows()/2));
        Imgproc.pyrUp(downscaled, upscaled, gray.size());

        if (DETECT_RED_OBJECTS_ONLY) {
            // convert the image from RGBA to HSV
            Imgproc.cvtColor(upscaled, hsv, Imgproc.COLOR_RGB2HSV);
            // threshold the image for the lower and upper HSV red range
            Core.inRange(hsv, HSV_LOW_RED1, HSV_LOW_RED2, lowerRedRange);
            Core.inRange(hsv, HSV_HIGH_RED1, HSV_HIGH_RED2, upperRedRange);
            // put the two thresholded images together
            Core.addWeighted(lowerRedRange, 1.0, upperRedRange, 1.0, 0.0, bw);
            // apply canny to get edges only
            Imgproc.Canny(bw, bw, 0, 255);
        } else {
            // Use Canny instead of threshold to catch squares with gradient shading
            Imgproc.Canny(upscaled, bw, 0, 255);
        }


        // dilate canny output to remove potential
        // holes between edge segments
        Imgproc.dilate(bw, bw, new Mat(), new Point(-1, 1), 1);

        // find contours and store them all as a list
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        contourImage = bw.clone();
        Imgproc.findContours(
                contourImage,
                contours,
                hierarchyOutputVector,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
        );

        // loop over all found contours
        for (MatOfPoint cnt : contours) {
            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());

            // approximates a polygonal curve with the specified precision
            Imgproc.approxPolyDP(
                    curve,
                    approxCurve,
                    0.02 * Imgproc.arcLength(curve, true),
                    true
            );

            int numberVertices = (int)approxCurve.total();
            double contourArea = Imgproc.contourArea(cnt);

            Log.d(TAG, "vertices:" + numberVertices);

            // ignore to small areas
            if (Math.abs(contourArea) < 100
                   // || !Imgproc.isContourConvex(
                   ) {
                continue;
            }

            // triangle detection
            if(numberVertices == 3) {
                if (DISPLAY_IMAGES) {
                    doSomethingWithContent("triangle");
                } else {
                    setLabel(dst, "TRI", cnt);
                }


            }

            // rectangle, pentagon and hexagon detection
            if (numberVertices >= 4 && numberVertices <= 6) {

                List<Double> cos = new ArrayList<>();
                for (int j = 2; j < numberVertices + 1; j++) {
                    cos.add(
                            angle(
                                    approxCurve.toArray()[j % numberVertices],
                                    approxCurve.toArray()[j - 2],
                                    approxCurve.toArray()[j - 1]
                            )
                    );
                }
                Collections.sort(cos);

                double mincos = cos.get(0);
                double maxcos = cos.get(cos.size()-1);

                // rectangle detection
                if (numberVertices == 4
                        && mincos >= -0.1 && maxcos <= 0.3
                        ) {
                    if (DISPLAY_IMAGES) {
                        doSomethingWithContent("rectangle");
                    }
                    else {
                        setLabel(dst, "RECT", cnt);
                    }
                }
                // pentagon detection
                else if(numberVertices == 5
                        && mincos >= -0.34 && maxcos <= -0.27) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "PENTA", cnt);
                    }
                }
                // hexagon detection
                else if (numberVertices == 6
                        && mincos >= -0.55 && maxcos <= -0.45) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "HEXA", cnt);
                    }
                }
            }
            // circle detection
            else {
                Rect r = Imgproc.boundingRect(cnt);
                int radius = r.width / 2;

                if (Math.abs(
                        1 - (
                                r.width / r.height
                        )
                    ) <= 0.2 &&
                    Math.abs(
                        1 - (
                                contourArea / (Math.PI * radius * radius)
                        )
                    ) <= 0.2
                    ) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "CIR", cnt);
                    }
                }

            }


        }

        // return the matrix / image to show on the screen
        return dst;

    }

    /**
     * Helper function to find a cosine of angle between vectors
     * from pt0->pt1 and pt0->pt2
     */
    private static double angle(Point pt1, Point pt2, Point pt0)
    {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt(
                (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10
        );
    }

    /**
     * display a label in the center of the given contur (in the given image)
     * @param im the image to which the label is applied
     * @param label the label / text to display
     * @param contour the contour to which the label should apply
     */
    private void setLabel(Mat im, String label, MatOfPoint contour) {
        int fontface = Core.FONT_HERSHEY_SIMPLEX;
        double scale = 3;//0.4;
        int thickness = 3;//1;
        int[] baseline = new int[1];

        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);
        Rect r = Imgproc.boundingRect(contour);

        Point pt = new Point(
                r.x + ((r.width - text.width) / 2),
                r.y + ((r.height + text.height) /2)
        );
        /*
        Imgproc.rectangle(
                im,
                new Point(r.x + 0, r.y + baseline[0]),
                new Point(r.x + text.width, r.y -text.height),
                new Scalar(255,255,255),
                -1
                );
        */

        Imgproc.putText(im, label, pt, fontface, scale, RGB_RED, thickness);

    }

    /**
     * makes an logcat/console output with the string detected
     * displays also a TOAST message and finally sends the command to the overlay
     * @param content the content of the detected barcode
     */
    private void doSomethingWithContent(String content) {
        Log.d(TAG, "content: " + content); // for debugging in console

        final String command = content;

        Handler refresh = new Handler(Looper.getMainLooper());
        refresh.post(new Runnable() {
            public void run()
            {
                overlayView.changeCanvas(command);
            }
        });
    }
}

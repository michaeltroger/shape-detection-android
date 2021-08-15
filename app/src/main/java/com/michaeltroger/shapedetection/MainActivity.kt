package com.michaeltroger.shapedetection

import android.Manifest
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.CameraBridgeViewBase
import android.widget.Toast
import com.michaeltroger.shapedetection.views.OverlayView
import android.app.ActivityManager
import android.content.pm.PackageManager
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.view.SurfaceView
import org.opencv.android.OpenCVLoader
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.imgproc.Imgproc
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.opencv.core.*
import java.util.*
import kotlin.math.abs

/**
 * the main activity - entry to the application
 */
class MainActivity : ComponentActivity(), CvCameraViewListener2 {
    /**
     * the camera view
     */
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    /**
     * for displaying Toast info messages
     */
    private val toast: Toast? = null

    /**
     * responsible for displaying images on top of the camera picture
     */
    private var overlayView: OverlayView? = null

    /**
     * image thresholded to black and white
     */
    private var bw: Mat? = null

    /**
     * image converted to HSV
     */
    private var hsv: Mat? = null

    /**
     * the image thresholded for the lower HSV red range
     */
    private var lowerRedRange: Mat? = null

    /**
     * the image thresholded for the upper HSV red range
     */
    private var upperRedRange: Mat? = null

    /**
     * the downscaled image (for removing noise)
     */
    private var downscaled: Mat? = null

    /**
     * the upscaled image (for removing noise)
     */
    private var upscaled: Mat? = null

    /**
     * the image changed by findContours
     */
    private var contourImage: Mat? = null

    /**
     * the activity manager needed for getting the memory info
     * which is necessary for getting the memory usage
     */
    private var activityManager: ActivityManager? = null

    /**
     * responsible for getting memory information
     */
    private var mi: ActivityManager.MemoryInfo? = null

    /**
     * the found contour as hierarchy vector
     */
    private var hierarchyOutputVector: Mat? = null

    /**
     * approximated polygonal curve with specified precision
     */
    private var approxCurve: MatOfPoint2f? = null
    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    bw = Mat()
                    hsv = Mat()
                    lowerRedRange = Mat()
                    upperRedRange = Mat()
                    downscaled = Mat()
                    upscaled = Mat()
                    contourImage = Mat()
                    hierarchyOutputVector = Mat()
                    approxCurve = MatOfPoint2f()
                    mOpenCvCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    onPermissionGranted()
                } else {
                    checkPermissonAndInitialize()
                }
            }

    private fun checkPermissonAndInitialize() {
        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_my)

        // get the OverlayView responsible for displaying images on top of the camera
        overlayView = findViewById<View>(R.id.overlay_view) as OverlayView
        mOpenCvCameraView = findViewById<View>(R.id.java_camera_view) as CameraBridgeViewBase

        checkPermissonAndInitialize()
    }

    private fun onPermissionGranted() {
        if (FIXED_FRAME_SIZE) {
            mOpenCvCameraView!!.setMaxFrameSize(FRAME_SIZE_WIDTH, FRAME_SIZE_HEIGHT)
        }
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        mi = ActivityManager.MemoryInfo()
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
        toast?.cancel()
    }

    public override fun onResume() {
        super.onResume()

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        //nothing to do
    }
    override fun onCameraViewStopped() {
        //nothing to do
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        if (LOG_MEM_USAGE) {
            activityManager!!.getMemoryInfo(mi)
            val availableMegs = mi!!.availMem / 1048576L // 1024 x 1024
            //Percentage can be calculated for API 16+
            //long percentAvail = mi.availMem / mi.totalMem;
            Log.d(TAG, "available mem: $availableMegs")
        }

        // get the camera frame as gray scale image
        val gray: Mat = if (DETECT_RED_OBJECTS_ONLY) {
            inputFrame.rgba()
        } else {
            inputFrame.gray()
        }

        // the image to output on the screen in the end
        // -> get the unchanged color image
        val dst = inputFrame.rgba()

        // down-scale and upscale the image to filter out the noise
        Imgproc.pyrDown(gray, downscaled, Size((gray.cols() / 2).toDouble(), (gray.rows() / 2).toDouble()))
        Imgproc.pyrUp(downscaled, upscaled, gray.size())
        if (DETECT_RED_OBJECTS_ONLY) {
            // convert the image from RGBA to HSV
            Imgproc.cvtColor(upscaled, hsv, Imgproc.COLOR_RGB2HSV)
            // threshold the image for the lower and upper HSV red range
            Core.inRange(hsv, HSV_LOW_RED1, HSV_LOW_RED2, lowerRedRange)
            Core.inRange(hsv, HSV_HIGH_RED1, HSV_HIGH_RED2, upperRedRange)
            // put the two thresholded images together
            Core.addWeighted(lowerRedRange, 1.0, upperRedRange, 1.0, 0.0, bw)
            // apply canny to get edges only
            Imgproc.Canny(bw, bw, 0.0, 255.0)
        } else {
            // Use Canny instead of threshold to catch squares with gradient shading
            Imgproc.Canny(upscaled, bw, 0.0, 255.0)
        }


        // dilate canny output to remove potential
        // holes between edge segments
        Imgproc.dilate(bw, bw, Mat(), Point(-1.0, 1.0), 1)

        // find contours and store them all as a list
        val contours: List<MatOfPoint> = ArrayList()
        contourImage = bw!!.clone()
        Imgproc.findContours(
                contourImage,
                contours,
                hierarchyOutputVector,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
        )

        // loop over all found contours
        for (cnt in contours) {
            val curve = MatOfPoint2f(*cnt.toArray())

            // approximates a polygonal curve with the specified precision
            Imgproc.approxPolyDP(
                    curve,
                    approxCurve,
                    0.02 * Imgproc.arcLength(curve, true),
                    true
            )
            val numberVertices = approxCurve!!.total().toInt()
            val contourArea = Imgproc.contourArea(cnt)
            Log.d(TAG, "vertices:$numberVertices")

            // ignore to small areas
            if (abs(contourArea) < 100 // || !Imgproc.isContourConvex(
            ) {
                continue
            }

            // triangle detection
            if (numberVertices == 3) {
                if (DISPLAY_IMAGES) {
                    doSomethingWithContent("triangle")
                } else {
                    setLabel(dst, "TRI", cnt)
                }
            }

            // rectangle, pentagon and hexagon detection
            if (numberVertices in 4..6) {
                val cos: MutableList<Double> = ArrayList()
                for (j in 2 until numberVertices + 1) {
                    cos.add(
                            angle(
                                    approxCurve!!.toArray()[j % numberVertices],
                                    approxCurve!!.toArray()[j - 2],
                                    approxCurve!!.toArray()[j - 1]
                            )
                    )
                }
                cos.sort()
                val mincos = cos[0]
                val maxcos = cos[cos.size - 1]

                // rectangle detection
                if (numberVertices == 4 && mincos >= -0.1 && maxcos <= 0.3) {
                    if (DISPLAY_IMAGES) {
                        doSomethingWithContent("rectangle")
                    } else {
                        setLabel(dst, "RECT", cnt)
                    }
                } else if (numberVertices == 5 && mincos >= -0.34 && maxcos <= -0.27) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "PENTA", cnt)
                    }
                } else if (numberVertices == 6 && mincos >= -0.55 && maxcos <= -0.45) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "HEXA", cnt)
                    }
                }
            } else {
                val r = Imgproc.boundingRect(cnt)
                val radius = r.width / 2
                if (abs(
                                1 - r.width / r.height
                        ) <= 0.2 &&
                        abs(
                                1 - contourArea / (Math.PI * radius * radius)
                        ) <= 0.2) {
                    if (!DISPLAY_IMAGES) {
                        setLabel(dst, "CIR", cnt)
                    }
                }
            }
        }

        // return the matrix / image to show on the screen
        return dst
    }

    /**
     * display a label in the center of the given contur (in the given image)
     * @param im the image to which the label is applied
     * @param label the label / text to display
     * @param contour the contour to which the label should apply
     */
    private fun setLabel(im: Mat, label: String, contour: MatOfPoint) {
        val fontface = Core.FONT_HERSHEY_SIMPLEX
        val scale = 3.0 //0.4;
        val thickness = 3 //1;
        val baseline = IntArray(1)
        val text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline)
        val r = Imgproc.boundingRect(contour)
        val pt = Point(
                r.x + (r.width - text.width) / 2,
                r.y + (r.height + text.height) / 2
        )
        /*
        Imgproc.rectangle(
                im,
                new Point(r.x + 0, r.y + baseline[0]),
                new Point(r.x + text.width, r.y -text.height),
                new Scalar(255,255,255),
                -1
                );
        */Imgproc.putText(im, label, pt, fontface, scale, RGB_RED, thickness)
    }

    /**
     * makes an logcat/console output with the string detected
     * displays also a TOAST message and finally sends the command to the overlay
     * @param content the content of the detected barcode
     */
    private fun doSomethingWithContent(content: String) {
        Log.d(TAG, "content: $content") // for debugging in console
        val refresh = Handler(Looper.getMainLooper())
        refresh.post { overlayView!!.changeCanvas(content) }
    }

    companion object {
        /**
         * class name for debugging with logcat
         */
        private val TAG = MainActivity::class.java.name

        /**
         * whether or not to log the memory usage per frame
         */
        private const val LOG_MEM_USAGE = true

        /**
         * detect only red objects
         */
        private const val DETECT_RED_OBJECTS_ONLY = false

        /**
         * the lower red HSV range (lower limit)
         */
        private val HSV_LOW_RED1 = Scalar(0.0, 100.0, 100.0)

        /**
         * the lower red HSV range (upper limit)
         */
        private val HSV_LOW_RED2 = Scalar(10.0, 255.0, 255.0)

        /**
         * the upper red HSV range (lower limit)
         */
        private val HSV_HIGH_RED1 = Scalar(160.0, 100.0, 100.0)

        /**
         * the upper red HSV range (upper limit)
         */
        private val HSV_HIGH_RED2 = Scalar(179.0, 255.0, 255.0)

        /**
         * definition of RGB red
         */
        private val RGB_RED = Scalar(255.0, 0.0, 0.0)

        /**
         * frame size width
         */
        private const val FRAME_SIZE_WIDTH = 640

        /**
         * frame size height
         */
        private const val FRAME_SIZE_HEIGHT = 480

        /**
         * whether or not to use a fixed frame size -> results usually in higher FPS
         * 640 x 480
         */
        private const val FIXED_FRAME_SIZE = true

        /**
         * whether or not to use the database to display
         * an image on top of the camera
         * when false the objects are labeled with writing
         */
        private const val DISPLAY_IMAGES = false

        /**
         * Helper function to find a cosine of angle between vectors
         * from pt0->pt1 and pt0->pt2
         */
        private fun angle(pt1: Point, pt2: Point, pt0: Point): Double {
            val dx1 = pt1.x - pt0.x
            val dy1 = pt1.y - pt0.y
            val dx2 = pt2.x - pt0.x
            val dy2 = pt2.y - pt0.y
            return ((dx1 * dx2 + dy1 * dy2)
                    / Math.sqrt(
                    (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10
            ))
        }
    }

    init {
        Log.i(TAG, "Instantiated new " + this.javaClass)
    }
}
package be.pxl.troger.ar.tools;

import android.util.Log;

import java.util.HashMap;

/**
 * includes hardcoded the barcodes to
 * handle and their corresponding commands
 * @author Michael Troger
 */
public class ImgprocDatabase {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = ImgprocDatabase.class.getName();
    /**
     * hold the database
     * key -> is the barcode value (String)
     * value -> is the command to apply (String)
     */
    private HashMap<String, String> dataBase;

    /**
     * create an instance of the ImgprocDatabase
     */
    public ImgprocDatabase() {
        dataBase = new HashMap<>();
        fillDatabase();

        Log.d(TAG, "started :)");
    }

    /**
     * fill the database hardcoded
     */
    private void fillDatabase() {
        dataBase.put("triangle", "triangle");
        dataBase.put("rectangle", "rectangle");
    }

    /**
     * get the barcode database
     * @return returns the database as HashMap
     */
    public HashMap<String, String> getDataBase() {
        return dataBase;
    }
}
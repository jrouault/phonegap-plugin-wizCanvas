package com.impactjs.ejecta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.impactjs.ejecta.EjectaGLSurfaceView;
import android.app.Activity;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import com.impactjs.ejecta.EjectaRenderer;
import com.impactjs.ejecta.Utils;
import com.phonegap.hello_world.R;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class DemoActivity extends Activity {
	private GLSurfaceView mGLView;
    private Activity mActivity;
    private static String TAG = "ejecta";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        // Set theme for a transparent background
        setTheme(R.style.WizCanvas);
		super.onCreate(savedInstanceState);

        mActivity = this;
        setContentView(R.layout.wiz_canvas_layout);

        // Get extra layout user data
        Intent intent = getIntent();
        int width = intent.getIntExtra("EXTRA_WIDTH", -1);
        int height = intent.getIntExtra("EXTRA_HEIGHT", -1);
        int x = intent.getIntExtra("EXTRA_X", -1);
        int y = intent.getIntExtra("EXTRA_Y", -1);
        int top = intent.getIntExtra("EXTRA_TOP", -1);
        int left = intent.getIntExtra("EXTRA_LEFT", -1);
        int right = intent.getIntExtra("EXTRA_RIGHT", -1);
        int bottom = intent.getIntExtra("EXTRA_BOTTOM", -1);

        /*
        if ((width == -1) || (height == -1) ) {
            // None supplied, use user-defined variables
            width = getWindowManager().getDefaultDisplay().getWidth();
            height = getWindowManager().getDefaultDisplay().getHeight();

            // Ratio Control

            int gameWidth = width;
            int gameHeight = height;
            float scaleToFitX = (float)gameWidth / 480;
            float scaleToFitY = (float)gameHeight / 800;
            Log.d(TAG, "=========================== ");
            Log.d(TAG, "gameWidth          : " + gameWidth);
            Log.d(TAG", "gameHeight         : " + gameHeight);
            Log.d(TAG, "scaleToFitX        : " + Float.toString(scaleToFitX));
            Log.d(TAG, "scaleToFitY        : " + Float.toString(scaleToFitY));

            float currentScreenRatio = (float)gameHeight / (float)gameWidth;
            float optimalRatio = Math.min(scaleToFitX, scaleToFitY);
            Log.d(TAG, "optimalRatio       : " + optimalRatio);


            if (currentScreenRatio >= 1.77 && currentScreenRatio <= 1.79) {
                width = gameWidth;
                height = gameHeight;
            } else {
                Log.d(TAG, "modified ratio?    : yes");
                width = Math.round(480 * optimalRatio);
                height = Math.round(800 * optimalRatio);
            }
            Log.d(TAG, "width              : " + width);
            Log.d(TAG, "height             : " + height);
            NumberFormat df = DecimalFormat.getInstance();
            df.setMinimumFractionDigits(2);
            df.setMaximumFractionDigits(2);
            df.setRoundingMode(RoundingMode.UP);
            Log.d(TAG, "currentScreenRatio : " + df.format(currentScreenRatio));
            Log.d(TAG, "=========================== ");

        }
        */

        mGLView = new EjectaGLSurfaceView(this, width, height);

        ((EjectaGLSurfaceView)mGLView).setEjectaEventListener(new EjectaRenderer.EjectaEventListener() {
            @Override
            public void onCanvasCreated() {
                Log.d("ejecta", "Canvas created!");
            }
        });

        // Pass touch events to the background activity but retain events in wizCanvas
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        WindowManager.LayoutParams wmlp = getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP | Gravity.LEFT;

        /*
        Log.d(TAG, "w: " + width);
        Log.d(TAG, "h: " + height);
        Log.d(TAG, "x: " + x);
        Log.d(TAG, "y: " + y);
        Log.d(TAG, "top: " + top);
        Log.d(TAG, "left: " + left);
        Log.d(TAG, "right: " + right);
        Log.d(TAG, "bottom: " + bottom);
        */

        FrameLayout activityView = (FrameLayout) findViewById(R.id.frame).getRootView();
        RelativeLayout frame = (RelativeLayout) findViewById(R.id.frame);

        FrameLayout.LayoutParams activityLp = (FrameLayout.LayoutParams) frame.getLayoutParams();
        activityLp.height = height;
        activityLp.width = width;
        activityView.setLayoutParams(activityLp);

        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
        // Add view to layout
        container.addView(mGLView);

        // Get layout of container parent
        RelativeLayout.LayoutParams frame_layout = (RelativeLayout.LayoutParams) container.getLayoutParams();

        // Calculate CCS-like positioning
        RelativeLayout.LayoutParams new_frame_layout = setLayout(frame_layout,
                getWindowManager().getDefaultDisplay().getWidth(),
                getWindowManager().getDefaultDisplay().getHeight(),
                width,
                height,
                x,
                y,
                top,
                left,
                right,
                bottom);

        // Set user defined alignments
        container.setLayoutParams(new_frame_layout);

        IntentFilter intentFilter = new IntentFilter("android.intent.action.MESSAGE");

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Extract our message from intent
                String command = intent.getStringExtra("MESSAGE");

                // Check command
                if (command.contains("DESTROY")) {
                    mActivity.unregisterReceiver(this);
                    mActivity.finish();
                    return;
                }
                if (command.contains("LAYOUT")) {
                    mActivity.unregisterReceiver(this);
                    mActivity.finish();
                    return;
                }
                if (command.contains("LOAD")) {
                    String source = intent.getStringExtra("SOURCE");
                    load(source);
                    return;
                }
            }
        };

        // Registering our receiver
        this.registerReceiver(mReceiver, intentFilter);
    }

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		((EjectaGLSurfaceView)mGLView).onDestroy();
		super.onDestroy();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
	
	static {
    	//System.loadLibrary("corefoundation");
    	System.loadLibrary("JavaScriptCore");
        System.loadLibrary("ejecta");
    }

    public void load(String source) {
        // Check remote or local source
        try {
            URL u = new URL(source);    // Check for the protocol
            u.toURI();                  // Extra checking required for validation of URI

            // If we did not fall out here then source is a valid URI
            Log.d(TAG, "load URL: " + source);

            // Download URL source to data/data/<package_name>/filename then call load()
            // again with local path
            new asyncDownload(u, this).execute();

        } catch (MalformedURLException ex1) {
            // Missing protocol

            // Copies the file to data/data/<package_name>/filename
            // Allows the file to be readable from Ejecta-X
            String mainBundle = "/data/data/" + this.getPackageName();
            Utils.copyDatFile(this, mainBundle + "/files/build/", source);

            File f = new File("file:///android_asset/" + source);

            ((EjectaGLSurfaceView)mGLView).loadJavaScriptFile(f.getName());

            f = null;

        } catch (URISyntaxException ex2) {

            // Copies the file to data/data/<package_name>/filename
            // Allows the file to be readable from Ejecta-X
            String mainBundle = "/data/data/" + this.getPackageName();
            Utils.copyDatFile(this, mainBundle + "/files/build/", source);

            File f = new File("file:///android_asset/" + source);

            ((EjectaGLSurfaceView)mGLView).loadJavaScriptFile(f.getName());

            f = null;
        }


    }

    private RelativeLayout.LayoutParams setLayout(RelativeLayout.LayoutParams rl, int parentHeight, int parentWidth, int width, int height, int x, int y, int top, int left, int right, int bottom) {
        Log.d(TAG, "Setting up layout...");

        // Size
        int _parentHeight = parentHeight;
        int _parentWidth = parentWidth;
        int _height = _parentHeight;
        int _width = _parentWidth;

        // Margins
        int _x = 0;
        int _y = 0;
        int _top = 0;
        int _bottom = 0;
        int _right = 0;
        int _left = 0;

        if (height != -1) {
            _height = height;
        }

        if (width != -1) {
            _width = width;
        }

        if (x != -1) {
            _x = x;
            _left = _x;
            _width = _width + _x;
        }

        if (y != -1) {
            _y = y;
            _top = _y;
            _height = _height + _y;
        }

        if (left != -1) {
            _left = _left + left;
            _width -= _left;
        } else {
            // default
            if (_x != 0) {
                _left = _x;
            }
        }

        if (right != -1) {
            _right = right;
            _width = _width - _right;
        }

        if (top != -1) {
            _top = _top + top;
        } else {
            // default
            if (_y != 0) {
                _top = _y;
            }
        }

        if (bottom != -1) {
            _top = _top + _parentHeight - _height - bottom;
            _bottom = - bottom;
        }

        rl.setMargins(_left, _top, _right, _bottom);
        rl.height = _height;
        rl.width = _width;

        return rl;
    }

    private class asyncDownload extends AsyncTask<File, String , String> {

        private String dirName;
        private URL url;
        private Activity activity;

        // Constructor
        public asyncDownload(URL url, Activity activity) {
            // Assign class vars
            this.url = url;
            this.activity = activity;
        }

        @Override
        protected String doInBackground(File... params) {
            // Run async download task
            String filename;
            try {
                filename = new File(this.url.toURI().getPath()).getName();

            } catch (URISyntaxException e) {
                Log.e("ejecta", "URL ERROR");
                return "";
            }

            File file = new File("/data/data/" + this.activity.getPackageName() + "/files/build/" + filename);
            if (!file.exists()) {
                // Create the directory if not existing
                file.mkdirs();
            }

            Log.d(TAG, "[downloadUrl] *********** /data/data/" + this.activity.getPackageName() + "/files/build/" + filename + " > " + file.getAbsolutePath());

            try {
                URL url = this.url;
                HttpGet httpRequest = null;
                httpRequest = new HttpGet(url.toURI());

                HttpClient httpclient = new DefaultHttpClient();

                // Credential check
                String credentials = url.getUserInfo();
                if (credentials != null) {
                    // Add Basic Authentication header
                    httpRequest.setHeader("Authorization", "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP));
                }

                HttpResponse response = httpclient.execute(httpRequest);
                HttpEntity entity = response.getEntity();

                InputStream is;

                Header contentHeader = entity.getContentEncoding();
                if (contentHeader != null) {
                    if (contentHeader.getValue().contains("gzip")) {
                        Log.d(TAG, "GGGGGGGGGZIIIIIPPPPPED!");
                        is = new GZIPInputStream(entity.getContent());
                    } else {
                        BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
                        is = bufHttpEntity.getContent();
                    }
                } else {
                    BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
                    is = bufHttpEntity.getContent();
                }
                byte[] buffer = new byte[1024];

                int len1 = 0;

                FileOutputStream fos = new FileOutputStream(file);

                while ( (len1 = is.read(buffer)) > 0 ) {
                    fos.write(buffer,0, len1);
                }

                fos.close();
                is.close();

                // Tell Ejecta-X to load file from downloaded path
                load(filename);

            } catch (MalformedURLException e) {
                Log.e("ejecta", "Bad url : ", e);
            } catch (Exception e) {
                Log.e("ejecta", "Error : " + e);
            }
            return null;
        }
    }
}
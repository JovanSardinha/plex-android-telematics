package ai.plex.poc.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import ai.plex.poc.android.Constants;
import ai.plex.poc.android.R;
import ai.plex.poc.android.services.PredictiveMotionDataService;

public class WebAppActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_app);
        WebView mWebView = (WebView) findViewById(R.id.web_app);

        //Expose the Android javascipt api to the hosted application in the web view
        mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
        mWebView.getSettings().setJavaScriptEnabled(true);

        //Set the url for the mobile application
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient());
        //mWebView.loadUrl("http://40.122.215.160/");
        mWebView.loadUrl("http://40.122.215.160:2910/");

        // Start the background service
        Intent mServiceIntent = new Intent(this, PredictiveMotionDataService.class);

        if (!PredictiveMotionDataService.isRunning()) {
            mServiceIntent.setAction(Constants.ACTIONS.START_PREDICTIVE_MOTION_SERVICE_IN_FOREGROUND);
            startService(mServiceIntent);
        }
    }



    /**
     * An interface that will be passed to the web application
     */
    public class WebAppInterface{
        Context mContext;

        WebAppInterface(Context context){
            mContext = context;
        }

        /**
         * An exposed function that allows the hosted webview to intract with the application
         * @param userId
         */
        @JavascriptInterface
        public void setUserId(String userId){
            mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putString("userId", userId).commit();
            Toast.makeText(mContext, "User id: " + mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).getString("userId","Default_user"), Toast.LENGTH_SHORT).show();
        }

        /**
         * An exposed function that allows the web application to check if the app is already
         * associated with a user and skip requiring to authenticate
         * Todo: Replace this implementation with a more secure implementation
         * @param userId
         * @return
         */
        @JavascriptInterface
        public String getUserId(String userId){
            return PreferenceManager.getDefaultSharedPreferences(mContext).getString("userId", "");
        }


    }
}

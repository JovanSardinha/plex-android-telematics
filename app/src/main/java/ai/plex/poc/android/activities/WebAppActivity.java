package ai.plex.poc.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import ai.plex.poc.android.Constants;
import ai.plex.poc.android.R;
import ai.plex.poc.android.services.PredictiveMotionDataService;

public class WebAppActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_app);
        WebView mWebView = (WebView) findViewById(R.id.web_app);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl("http://40.122.215.160:2910/mobile/overview");

        // Start the background service
        Intent mServiceIntent = new Intent(this, PredictiveMotionDataService.class);

        if (!PredictiveMotionDataService.isRunning()) {
            mServiceIntent.setAction(Constants.ACTIONS.START_PREDICTIVE_MOTION_SERVICE_IN_FOREGROUND);
            startService(mServiceIntent);
        }
    }
}

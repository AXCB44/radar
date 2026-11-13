package pl.radar.gta;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity implements LocationListener {

    private WebView web;
    private LocationManager lm;

    public static class Bridge {
        @JavascriptInterface
        public boolean ok() {
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowUniversalAccessFromFileURLs(true);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                startUpdates();
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/radar.html");

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && !hasPerm()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private boolean hasPerm() {
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startUpdates() {
        if (lm == null || !hasPerm()) return;
        try {
            String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
            for (String p : providers) {
                if (lm.isProviderEnabled(p)) {
                    lm.requestLocationUpdates(p, 2000, 5, this);
                    Location last = lm.getLastKnownLocation(p);
                    if (last != null) push(last);
                }
            }
        } catch (SecurityException e) {
            // permission not granted
        }
    }

    private void push(Location l) {
        final String js = "window.nativePos&&window.nativePos(" + l.getLatitude() + "," + l.getLongitude() + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript(js, null);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (lm != null) {
            try {
                lm.removeUpdates(this);
            } catch (SecurityException e) {
                // ignore
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        push(location);
    }

    @Override
    public void onProviderEnabled(String provider) {
        startUpdates();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
    }
}

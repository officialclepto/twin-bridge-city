package com.twinbridgecity.streetlegends;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

/** Full-screen landscape window that runs the game from the files packed inside the app. */
public class MainActivity extends Activity {
    private WebView web;
    private TtsBridge tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        web = new WebView(this);
        web.setBackgroundColor(0xFF000000);
        setContentView(web);
        hideSystemBars();

        // serve app/src/main/assets/ at https://appassets.androidplatform.net/assets/
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                 // saves settings and arcade crowns
        s.setMediaPlaybackRequiresUserGesture(false); // music and sound effects
        s.setAllowFileAccess(false);

        tts = new TtsBridge(this);
        web.addJavascriptInterface(tts, "AndroidTTS");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.evaluateJavascript("try{AC&&AC.suspend()}catch(e){}", null);
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.evaluateJavascript("try{AC&&AC.resume()}catch(e){}", null);
        hideSystemBars();
    }

    /** Phone back button: steps back through the game's menus, and closes the app from the title screen. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript(
                "(function(){try{if(scene==='title'&&!titleMenu)return 'exit';"
                        + "if(scene==='title'){titleMenu=false;return 'ok';}"
                        + "onKeyMenu('Escape');}catch(e){}return 'ok';})()",
                value -> {
                    if (value != null && value.contains("exit")) finish();
                });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}

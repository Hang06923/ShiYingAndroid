package com.hang.shiying;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hang.shiying.bridge.NativeBridge;
import com.hang.shiying.data.DownloadRepository;

public final class MainActivity extends Activity {
    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int insetTop = -1;
    private int insetBottom = -1;
    private int insetIme = -1;
    private boolean pageReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(244, 244, 242));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                pushInsets();
            }
        });
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setScrollbarFadingEnabled(true);
        webView.addJavascriptInterface(
                new NativeBridge(this, this::dispatchToWeb),
                "ShiYingNative");

        setContentView(webView);
        goImmersive();
        webView.setOnApplyWindowInsetsListener((view, insets) -> {
            readInsets(insets);
            return insets;
        });
        new DownloadRepository(this).markActiveTasksInterrupted();
        requestOptionalPermissions();
        webView.post(() -> {
            WindowInsets current = webView.getRootWindowInsets();
            if (current != null) {
                readInsets(current);
            }
            webView.loadUrl("file:///android_asset/www/index.html");
        });
    }

    private void goImmersive() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }

        boolean lightSystemBars = !isNightMode();
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(
                        lightSystemBars ? appearance : 0,
                        appearance);
            }
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (lightSystemBars) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private boolean isNightMode() {
        return (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private void readInsets(WindowInsets insets) {
        int top;
        int bottom;
        int ime;
        if (Build.VERSION.SDK_INT >= 30) {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                            | WindowInsets.Type.displayCutout());
            top = bars.top;
            bottom = bars.bottom;
            ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
        } else {
            top = insets.getSystemWindowInsetTop();
            bottom = insets.getSystemWindowInsetBottom();
            ime = 0;
        }

        float density = getResources().getDisplayMetrics().density;
        if (density <= 0f) {
            density = 1f;
        }
        int nextTop = Math.round(top / density);
        int nextBottom = Math.round(bottom / density);
        int nextIme = Math.round(ime / density);
        if (nextTop == insetTop
                && nextBottom == insetBottom
                && nextIme == insetIme) {
            return;
        }
        insetTop = nextTop;
        insetBottom = nextBottom;
        insetIme = nextIme;
        pushInsets();
    }

    private void pushInsets() {
        if (webView == null || !pageReady) {
            return;
        }
        String javascript =
                "document.documentElement.style.setProperty('--safe-top','"
                        + Math.max(0, insetTop) + "px');"
                        + "document.documentElement.style.setProperty('--safe-bottom','"
                        + Math.max(0, insetBottom) + "px');"
                        + "document.documentElement.style.setProperty('--ime','"
                        + Math.max(0, insetIme) + "px');";
        webView.evaluateJavascript(javascript, null);
    }

    private void dispatchToWeb(String javascript) {
        mainHandler.post(() -> {
            if (webView != null) {
                webView.evaluateJavascript(javascript, null);
            }
        });
    }

    private void requestOptionalPermissions() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    2001);
        }
        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    2002);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            goImmersive();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

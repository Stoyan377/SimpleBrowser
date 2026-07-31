package stoyanov.stoyan.simplebrowser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomappbar.BottomAppBar;

public class MainActivity extends AppCompatActivity {

    WebView brow;
    EditText urledit;
    ImageButton go;
    ImageButton adblockBtn;
    ProgressBar progressBar;
    BottomAppBar bottomAppBar;
    FrameLayout fullscreenContainer;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private ourViewClient webViewClient;
    private boolean adBlockEnabled = true;
    private boolean isServiceRunning = false;
    private boolean isUrlBarFullySelected = false;

    // Handler for periodic background tick (ad skipping + force resume)
    private final Handler bgHandler = new Handler(Looper.getMainLooper());
    private Runnable bgTickRunnable;

    private static final String DEFAULT_HOME = "https://www.google.com";
    private static final String SEARCH_URL = "https://www.google.com/search?q=";
    private static final String PREFS_NAME = "SimpleBrowserPrefs";
    private static final String PREF_ADBLOCK = "adblock_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ad blocker with comprehensive hosts list
        AdBlocker.init(this);

        // Load ad-block preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        adBlockEnabled = prefs.getBoolean(PREF_ADBLOCK, true);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        brow = (WebView) findViewById(R.id.wv_brow);
        urledit = (EditText) findViewById(R.id.et_url);
        go = (ImageButton) findViewById(R.id.btn_go);
        adblockBtn = (ImageButton) findViewById(R.id.btn_adblock);
        bottomAppBar = findViewById(R.id.bottom_app_bar);
        fullscreenContainer = (FrameLayout) findViewById(R.id.fullscreen_container);

        setSupportActionBar(bottomAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set up WebViewClient with ad-blocking and background playback visibility patch
        webViewClient = new ourViewClient();
        webViewClient.setAdBlockEnabled(adBlockEnabled);
        brow.setWebViewClient(webViewClient);

        brow.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);

                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    // Update URL bar with the current page URL
                    if (view.getUrl() != null) {
                        urledit.setText(view.getUrl());
                    }
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;

                fullscreenContainer.addView(customView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                fullscreenContainer.setVisibility(View.VISIBLE);

                brow.setVisibility(View.GONE);
                findViewById(R.id.ll_urlgo).setVisibility(View.GONE);
                bottomAppBar.setVisibility(View.GONE);
                findViewById(R.id.bottom_divider).setVisibility(View.GONE);

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });

        WebSettings webSettings = brow.getSettings();
        webSettings.setUserAgentString(null); // Force default Android Mobile User-Agent
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setUseWideViewPort(false); // Disable desktop viewport
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Keep WebView renderer alive at high priority in background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            brow.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        }

        brow.loadUrl(DEFAULT_HOME);

        // Go button — smart URL detection
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrlFromInput();
            }
        });

        // Ad-block toggle button
        updateAdBlockButton();
        adblockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adBlockEnabled = !adBlockEnabled;
                webViewClient.setAdBlockEnabled(adBlockEnabled);
                updateAdBlockButton();

                // Persist preference
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(PREF_ADBLOCK, adBlockEnabled).apply();

                Toast.makeText(MainActivity.this,
                        adBlockEnabled ? "Ad-block enabled" : "Ad-block disabled",
                        Toast.LENGTH_SHORT).show();

                // Reload page to apply/remove ad blocking
                brow.reload();
            }
        });

        // Handle Enter/Go key on keyboard
        urledit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                                && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    loadUrlFromInput();
                    return true;
                }
                return false;
            }
        });

        // Address bar touch logic: 1st tap selects all, 2nd tap places cursor at clicked position
        urledit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (!urledit.hasFocus()) {
                        urledit.requestFocus();
                        urledit.post(new Runnable() {
                            @Override
                            public void run() {
                                urledit.selectAll();
                                isUrlBarFullySelected = true;
                            }
                        });
                        return true; // Consume touch event so initial tap doesn't place cursor
                    } else if (isUrlBarFullySelected) {
                        // 2nd tap while selected: clear selection and allow cursor placement at tapped character
                        isUrlBarFullySelected = false;
                        return false; 
                    }
                }
                return false;
            }
        });

        urledit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    urledit.post(new Runnable() {
                        @Override
                        public void run() {
                            urledit.selectAll();
                            isUrlBarFullySelected = true;
                        }
                    });
                } else {
                    isUrlBarFullySelected = false;
                }
            }
        });
    }

    private void hideCustomView() {
        if (customView == null) return;

        fullscreenContainer.removeView(customView);
        fullscreenContainer.setVisibility(View.GONE);

        brow.setVisibility(View.VISIBLE);
        findViewById(R.id.ll_urlgo).setVisibility(View.VISIBLE);
        bottomAppBar.setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_divider).setVisibility(View.VISIBLE);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
        }
        customView = null;
        customViewCallback = null;
    }

    /**
     * Updates the ad-block button icon and background based on current state.
     */
    private void updateAdBlockButton() {
        if (adBlockEnabled) {
            adblockBtn.setImageResource(R.drawable.ic_shield);
            adblockBtn.setBackgroundResource(R.drawable.adblock_button_on);
        } else {
            adblockBtn.setImageResource(R.drawable.ic_shield_off);
            adblockBtn.setBackgroundResource(R.drawable.adblock_button_off);
        }
    }

    /**
     * Smart URL loading: detects whether input is a URL or a search query.
     */
    private void loadUrlFromInput() {
        String input = urledit.getText().toString().trim();
        if (input.isEmpty()) return;

        String url;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            url = SEARCH_URL + input;
        }

        brow.loadUrl(url);
        hideKeyboardAndClearFocus();
    }

    private void hideKeyboardAndClearFocus() {
        urledit.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(urledit.getWindowToken(), 0);
        }
    }

    // --- Background Playback Lifecycle ---

    @Override
    protected void onPause() {
        super.onPause();
        startBackgroundPlayback();
        if (brow != null) {
            brow.onResume(); // Keep WebView JS/media execution alive

            // Tell JS we're going to background — pause override will block background pauses
            brow.evaluateJavascript("window.__sbIsBackground = true;", null);

            // Start repeating background tick: ad-skip + force-resume every 500ms
            startBgTick();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        startBackgroundPlayback();
        if (brow != null) {
            brow.onResume();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Stop background tick
        stopBgTick();

        if (brow != null) {
            brow.onResume();
            // Tell JS we're back in foreground — allow normal pause behavior
            brow.evaluateJavascript("window.__sbIsBackground = false;", null);
        }
    }

    @Override
    protected void onDestroy() {
        stopBgTick();
        stopBackgroundPlayback();
        if (brow != null) {
            brow.onPause();
            brow.destroy();
        }
        super.onDestroy();
    }

    /**
     * Start a repeating Handler callback that runs the background tick script
     * every 500ms. This ensures ad-skipping and force-resume work even if
     * JS setInterval is throttled by Android WebView in the background.
     */
    private void startBgTick() {
        stopBgTick(); // Clear any existing
        bgTickRunnable = new Runnable() {
            @Override
            public void run() {
                if (brow != null) {
                    brow.evaluateJavascript(AdBlocker.getBgTickScript(), null);
                }
                bgHandler.postDelayed(this, 500);
            }
        };
        bgHandler.postDelayed(bgTickRunnable, 300);
    }

    private void stopBgTick() {
        if (bgTickRunnable != null) {
            bgHandler.removeCallbacks(bgTickRunnable);
            bgTickRunnable = null;
        }
    }

    private void startBackgroundPlayback() {
        if (!isServiceRunning) {
            Intent serviceIntent = new Intent(this, BackgroundPlaybackService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            isServiceRunning = true;
        }
    }

    private void stopBackgroundPlayback() {
        if (isServiceRunning) {
            Intent serviceIntent = new Intent(this, BackgroundPlaybackService.class);
            stopService(serviceIntent);
            isServiceRunning = false;
        }
    }

    // --- Menu ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bottom_nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_back) {
            if (brow.canGoBack())
                brow.goBack();
            return true;
        } else if (itemId == R.id.action_forward) {
            if (brow.canGoForward())
                brow.goForward();
            return true;
        } else if (itemId == R.id.action_reload) {
            brow.reload();
            return true;
        } else if (itemId == R.id.action_stop) {
            brow.stopLoading();
            return true;
        } else if (itemId == R.id.action_settings) {
            brow.clearCache(true);
            brow.clearHistory();
            brow.clearMatches();
            brow.clearFormData();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (customView != null) {
                hideCustomView();
                return true;
            }
            if (brow.canGoBack())
                brow.goBack();
            else this.moveTaskToBack(true);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}

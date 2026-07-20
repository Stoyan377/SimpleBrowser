package stoyanov.stoyan.simplebrowser;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomappbar.BottomAppBar;

public class MainActivity extends AppCompatActivity {

    WebView brow;
    EditText urledit;
    ImageButton go;
    ImageButton search;
    ProgressBar progressBar;
    BottomAppBar bottomAppBar;

    private static final String DEFAULT_HOME = "https://www.google.com";
    private static final String SEARCH_URL = "https://www.google.com/search?q=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        brow = (WebView)findViewById(R.id.wv_brow);
        urledit = (EditText)findViewById(R.id.et_url);
        go = (ImageButton)findViewById(R.id.btn_go);
        search = (ImageButton)findViewById(R.id.btn_go2);
        bottomAppBar = findViewById(R.id.bottom_app_bar);

        setSupportActionBar(bottomAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        brow.setWebViewClient(new ourViewClient());
        brow.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);

                if(newProgress == 100){
                    progressBar.setVisibility(View.GONE);
                    // Update URL bar with the current page URL
                    if (view.getUrl() != null) {
                        urledit.setText(view.getUrl());
                    }
                } else{
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        WebSettings webSettings = brow.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        brow.loadUrl(DEFAULT_HOME);

        // Go button — smart URL detection
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrlFromInput();
            }
        });

        // Search button — always Google Search
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = urledit.getText().toString().trim();
                if (!query.isEmpty()) {
                    brow.loadUrl(SEARCH_URL + query);
                }
                hideKeyboardAndClearFocus();
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

        // Select all text when URL bar is tapped for easy editing
        urledit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    urledit.selectAll();
                }
            }
        });
    }

    /**
     * Smart URL loading: detects whether input is a URL or a search query.
     * - If it starts with http:// or https://, load directly
     * - If it contains a dot and no spaces (e.g. "google.com"), treat as URL with https://
     * - Otherwise, treat as a Google search query
     */
    private void loadUrlFromInput() {
        String input = urledit.getText().toString().trim();
        if (input.isEmpty()) return;

        String url;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            // Already has a scheme
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            // Looks like a URL (e.g. "google.com", "en.wikipedia.org")
            url = "https://" + input;
        } else {
            // Treat as a search query
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bottom_nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_back) {
            if(brow.canGoBack())
                brow.goBack();
            return true;
        } else if (itemId == R.id.action_forward) {
            if(brow.canGoForward())
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
    public boolean onKeyDown( int keyCode, KeyEvent event )  {
        if ( keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if(brow.canGoBack())
                brow.goBack();
            else this.moveTaskToBack(true);
            return true;
        }

        return super.onKeyDown( keyCode, event );
    }

}

package stoyanov.stoyan.simplebrowser;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        brow= (WebView)findViewById(R.id.wv_brow);
        urledit = (EditText)findViewById(R.id.et_url);
        go = (ImageButton)findViewById(R.id.btn_go);
        search = (ImageButton)findViewById(R.id.btn_go2);
        bottomAppBar = findViewById(R.id.bottom_app_bar);
        
        setSupportActionBar(bottomAppBar);

        brow.setWebViewClient(new ourViewClient());
        brow.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);

                if(newProgress == 100){
                    progressBar.setVisibility(View.GONE);
                } else{
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        WebSettings webSettings = brow.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        //brow.setWebChromeClient(new WebChromeClient());
        //brow.getSettings().setAllowFileAccess(true);
        //brow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        brow.loadUrl("http://www.google.com");

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String editextvalue = urledit.getText().toString();

                if (!editextvalue.startsWith("http://"))
                    editextvalue = "http://" + editextvalue;
                String url = editextvalue;
                urledit.clearFocus();
                brow.loadUrl(url);

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(urledit.getWindowToken(), 0);

            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String editextvalue2 = urledit.getText().toString();
                editextvalue2 = "https://www.google.com/search?q=" + editextvalue2;
                String url2 = editextvalue2;
                urledit.clearFocus();
                brow.loadUrl(url2);

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(urledit.getWindowToken(), 0);


            }
        });


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


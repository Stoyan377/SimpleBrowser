package stoyanov.stoyan.simplebrowser;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    WebView brow;
    EditText urledit;
    ImageButton go;
    ImageButton search;
    Button forward;
    Button back;
    Button reload;
    Button stop;
    Button stngs;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        brow= (WebView)findViewById(R.id.wv_brow);
        urledit = (EditText)findViewById(R.id.et_url);
        go = (ImageButton)findViewById(R.id.btn_go);
        search = (ImageButton)findViewById(R.id.btn_go2);
        back = (Button)findViewById(R.id.bck);
        forward = (Button)findViewById(R.id.fwd);
        reload = (Button)findViewById(R.id.btn_reload);
        stop = (Button)findViewById(R.id.btn_stop);
        stngs = (Button)findViewById(R.id.btn_settings);

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

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(brow.canGoBack())
                    brow.goBack();
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(brow.canGoForward())
                    brow.goForward();
            }
        });

        reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                brow.reload();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                brow.stopLoading();
            }
        });


        stngs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                brow.clearCache(true);
                brow.clearHistory();
                brow.clearMatches();
                brow.clearFormData();
            }
        });

        urledit.setOnEditorActionListener(new TextView.OnEditorActionListener(){


            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO){
                    String editextvalue = urledit.getText().toString();
                    if(!editextvalue.startsWith("http://"))
                        editextvalue = "http://" + editextvalue;
                    String url = editextvalue;
                    urledit.clearFocus();
                    brow.loadUrl(url);

                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(urledit.getWindowToken(),0);
                    handled = true;
                }
                return handled;
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


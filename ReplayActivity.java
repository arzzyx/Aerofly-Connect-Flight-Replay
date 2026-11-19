package com.aeroflyconnect.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** Portable replay browser. Uses the same JSON replay format as the PC build. */
public class ReplayActivity extends Activity {
    private static final int PICK_REPLAY = 731;
    private WebView web;
    private String pendingJson;
    private LinearLayout list;
    private int pad;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); pad = Ui.dp(this, 18); showList();
    }
    private TextView label(String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(Ui.TXT); if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); return t;
    }
    private void showList() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(pad,pad,pad,pad);
        root.addView(label("FLIGHT REPLAY",24,true)); root.addView(label("3D Aircraft · 2D Scenery · 3D Terrain · 3D Flight Path",12,false));
        Button importBtn = Ui.button(this,"Import PC Replay (.json)",Ui.PRIMARY); importBtn.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/json"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_REPLAY); }); root.addView(importBtn);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(Ui.withBackground(this,scroll,R.drawable.bg_start)); refresh();
    }
    private void refresh() {
        list.removeAllViews(); File dir=new File(getFilesDir(),"replays"); File[] files=dir.listFiles((d,n)->n.endsWith(".json"));
        if(files==null||files.length==0){list.addView(label("No recordings yet. Start a flight in Pilot; it will be recorded automatically.",14,false));return;}
        for(File f:files){ Button b=Ui.button(this,f.getName(),Ui.SECONDARY); b.setOnClickListener(v->{try{open(read(f));}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}); list.addView(b); }
    }
    private String read(File f) throws Exception { FileInputStream in=new FileInputStream(f); String s=readStream(in); in.close(); return s; }
    private String readStream(InputStream in) throws Exception { ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n; while((n=in.read(b))>0)out.write(b,0,n); return out.toString("UTF-8"); }
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK_REPLAY&&res==RESULT_OK&&data!=null){try{InputStream in=getContentResolver().openInputStream(data.getData());String json=readStream(in);in.close();JSONObject j=new JSONObject(json);if(!"aerofly-connect-replay".equals(j.optString("schema")))throw new Exception("Unsupported replay file");File dir=new File(getFilesDir(),"replays");dir.mkdirs();File f=new File(dir,"import-"+System.currentTimeMillis()+".json");java.io.FileOutputStream o=new java.io.FileOutputStream(f);o.write(json.getBytes("UTF-8"));o.close();open(json);}catch(Exception e){Toast.makeText(this,"Could not open replay: "+e.getMessage(),Toast.LENGTH_LONG).show();}}}
    private void open(String json) {
        pendingJson=json; web=new WebView(this); web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true); web.getSettings().setAllowFileAccess(true); web.getSettings().setAllowFileAccessFromFileURLs(true); web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String url){String safe=JSONObject.quote(pendingJson);v.evaluateJavascript("window.__REPLAY_DATA=JSON.parse("+safe+");",null);}});
        setContentView(web); web.loadUrl("file:///android_asset/replay/index.html");
    }
    @Override public void onBackPressed(){if(web!=null){web=null;showList();}else super.onBackPressed();}
}

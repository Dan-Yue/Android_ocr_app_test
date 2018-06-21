package com.example.xueyudlut.ocrtest3;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements View.OnClickListener {

    Button btn_openCamera;
    Button btn_findText;
    Button btn_getTxt;
    OcrView ocrView;
    TextView txtget;
    Bitmap bmp = null;
    TessBaseAPI mTess;

    String sdPath;
    String picPath;
    String TXT_get = null;
    ThreadPoolExecutor threadPoolExecutor;//线程池
    String filename;
    int ScreenHeight;
    int ScreenWidth;

    static {
        System.loadLibrary("native-lib");
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            txtget.setText(OCRUtil.GetTextFromRect(mTess, bmp, ScreenWidth, ocrView));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_openCamera = (Button) findViewById(R.id.btn_openCamera);
        btn_findText = (Button) findViewById(R.id.btn_findtext);
        btn_getTxt = (Button) findViewById(R.id.btn_gettext);
        txtget = (TextView) findViewById(R.id.txt_get);
        btn_openCamera.setOnClickListener(this);
        btn_findText.setOnClickListener(this);
        btn_getTxt.setOnClickListener(this);
        initVariable();
        OCRUtil.init(getApplicationContext(), sdPath, filename);
        addContentView(ocrView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mTess = OCRUtil.initTessBaseData(sdPath, mTess);
            }
        });
    }

    private void initVariable() {
        WindowManager wm = this.getWindowManager();
        ScreenHeight = wm.getDefaultDisplay().getHeight();
        ScreenWidth = wm.getDefaultDisplay().getWidth();
        threadPoolExecutor = new ThreadPoolExecutor(3, 6, 2, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(128));
        sdPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Android/data/" + getPackageName() + "/files";
        picPath = sdPath + "/" + "temp.png";
        filename = sdPath + "/test/tessdata/amt.traineddata";
        ocrView = new OcrView(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            System.out.print(requestCode + "--------------" + resultCode);
            Bundle bdl = data.getExtras();
            bmp = (Bitmap) bdl.get("data");
            ImageView im_camera = (ImageView) findViewById(R.id.img_camera);
            im_camera.setImageBitmap(bmp);
            mTess.clear();
            mTess.setImage(bmp);
            String result = mTess.getUTF8Text();
            TextView txtget = (TextView) findViewById(R.id.txt_get);
            txtget.setText(result);
        } else if (resultCode == Activity.RESULT_OK && requestCode == OCRUtil.REQUST_ORIGINAL) {
            FileInputStream fis = null;
            try {
                Log.e("sdpath2", picPath);
                fis = new FileInputStream(picPath);
                bmp = null;
                bmp = OCRUtil.FixImageOrientation(picPath);
                ImageView im_camera = (ImageView) findViewById(R.id.img_camera);
                im_camera.setImageBitmap(bmp);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "没有拍到照片", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_openCamera:
                OCRUtil.getImageFromCamera(MainActivity.this, picPath);
                break;
            case R.id.btn_findtext:
                ocrView.SelectRect();
                break;
            case R.id.btn_gettext:
                txtget.setText("识别中，请稍等。");
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        TXT_get = OCRUtil.GetTextFromRect(mTess, bmp, ScreenWidth, ocrView);
                        handler.sendEmptyMessage(0);
                    }
                });
                break;
        }
    }
}

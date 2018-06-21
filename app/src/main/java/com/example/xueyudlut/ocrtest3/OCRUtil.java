package com.example.xueyudlut.ocrtest3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copyright 2013-2018 duolabao.com All right reserved. This software is the
 * confidential and proprietary information of duolabao.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with duolabao.com.
 * <p>
 * Created by DanYue on 2018/6/21 15:58.
 */
public class OCRUtil {
    public static int REQUST_ORIGINAL = 101;//原图标志

    //初始化Tess
    static public TessBaseAPI initTessBaseData(String sdPath, TessBaseAPI mTess) {
        mTess = new TessBaseAPI();
        String datapath = sdPath + "/test/";
        String language = "amt";
        File dir = new File(datapath + "tessdata/");
        if (!dir.exists())
            dir.mkdirs();
        mTess.init(datapath, language);
        return mTess;
    }

    //首次运行程序，载入训练文件到sd卡
    static public void copyBigDataToSD(Context context, String sdPath, String strFileName) throws IOException {
        InputStream myInput;
        context.getExternalFilesDir(null).getAbsolutePath();
        File dir = new File(sdPath + File.separator + "test/tessdata/");
        dir.mkdirs();
        File filea = new File(sdPath + File.separator + "test/tessdata/amt.traineddata");
        filea.createNewFile();
        OutputStream myOutput = new FileOutputStream(strFileName);
        myInput = context.getAssets().open("amt.traineddata");
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
    }

    //得到高质量照片
    static public void getImageFromCamera(Activity activity, String picPath) {
        Intent getImageByCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            File g = new File(picPath);//测试错误
            try {
                g.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            uri = FileProvider.getUriForFile(activity, "xueyu404", g);
        } else {
            uri = Uri.fromFile(new File(picPath));
        }
        getImageByCamera.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(getImageByCamera, REQUST_ORIGINAL);
    }

    static public String GetTextFromRect(TessBaseAPI mTess, Bitmap bmp, int ScreenWidth, OcrView ocrView) {
        double scale;
        mTess.clear();
        if (bmp == null) return null;
        mTess.setImage(bmp);
        scale = (double) bmp.getWidth() / (double) ScreenWidth;
        mTess.setRectangle((int) (scale * ocrView.rect.left), (int) (scale * ocrView.rect.top), (int) (scale * ocrView.rect.width()), (int) (scale * ocrView.rect.height()));
        String result = mTess.getUTF8Text();
        return result;
    }

    static public Bitmap FixImageOrientation(String imagePath) throws IOException {
        //检验图片地址是否正确
        if (imagePath == null || imagePath.equals(""))
            return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        //图片旋转角度
        int rotate = 0;

        ExifInterface exif = new ExifInterface(imagePath);
        //先获取当前图像的方向，判断是否需要旋转
        int imageOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (imageOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            default:
                break;
        }
        // 获取当前图片的宽和高
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix mtx = new Matrix();
        // 使用Matrix对图片进行处理
        mtx.preRotate(rotate);
        // 旋转图片
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        return bitmap;
    }

    static public void init(Context context, String sdPath, String filename) {
        //手机中不存在训练文件，则在sd卡中写入对应的文件
        //应用首次运行，将训练文件拷贝到sd卡中
        SharedPreferences sp = context.getSharedPreferences("ocr_test", Context.MODE_PRIVATE);
        int int_runtimes = sp.getInt("run", 0);
        if (int_runtimes == 0) {
            try {
                OCRUtil.copyBigDataToSD(context, sdPath, filename);
                sp.edit().putInt("run", 1).commit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

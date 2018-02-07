package com.android.systemtakeandselectphotodemo;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUESTCODE_PICK = 0;// 相册选图标记
    private static final int REQUESTCODE_TAKE = 1;// 相机拍照标记
    private static final int REQUESTCODE_CUTE = 2;// 裁剪图片标记

    private Button btnSelect;
    private Button btnTake;
    private TextView tvContent;

    private String mPhotoPath;
    private File mPhotoFile;
    private File mCutePhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPhotoPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM" + File.separator;
        final File file = new File(mPhotoPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        btnSelect = (Button) findViewById(R.id.btn_select);
        btnTake = (Button) findViewById(R.id.btn_take);
        tvContent = (TextView) findViewById(R.id.tv_content);

        btnSelect.setOnClickListener(mClick);
        btnTake.setOnClickListener(mClick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUESTCODE_TAKE == requestCode) {
            final Uri uri = getImageContentUri(this, mPhotoFile);
            cutPicture(uri);
        } else if (REQUESTCODE_PICK == requestCode) {
            if (data != null) {
                cutPicture(data.getData());
            }
        } else if (REQUESTCODE_CUTE == requestCode) {
            final String content = "\nfile_path: " + mCutePhotoFile.getAbsolutePath()
                    + "\nexists: " + mCutePhotoFile.exists() + "\ntips: " + (mCutePhotoFile.exists() ? "" : "请检查存储权限");
            tvContent.setText(content);

            refreshAlbum(mCutePhotoFile);
        }
    }

    private void doSelectPhoto() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(pickIntent, REQUESTCODE_PICK);
    }

    private void doTakePhoto() {
        Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takeIntent.resolveActivity(getPackageManager()) != null) {
            mPhotoFile = new File(mPhotoPath, System.currentTimeMillis() + ".jpg");

            final Uri uri = getUriFromFile(mPhotoFile);

            takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(takeIntent, REQUESTCODE_TAKE);
        } else {
            Toast.makeText(this, "打开相机失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void cutPicture(Uri uri) {
        mCutePhotoFile = new File(mPhotoPath, "cute_" + System.currentTimeMillis() + ".jpg");

        final Uri outputUri = Uri.fromFile(mCutePhotoFile);

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, REQUESTCODE_CUTE);
    }

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    private Uri getUriFromFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(this, "com.android.systemtakeandselectphotodemo.myprovider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    private void refreshAlbum(File file) {
        if (file != null && file.exists()) {
            final Uri uri = Uri.fromFile(file);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }
    }

    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_select:
                    doSelectPhoto();
                    break;
                case R.id.btn_take:
                    doTakePhoto();
                    break;
            }
        }
    };
}

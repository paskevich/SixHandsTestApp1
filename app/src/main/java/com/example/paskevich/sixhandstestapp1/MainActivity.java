package com.example.paskevich.sixhandstestapp1;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private ImageView imageView;

    private Button btnOpen;
    private Button btnSave;
    private Button btnMode;

    private static int REQUEST_IMAGE = 42;

    private Bitmap bitmapImage;
    private MyRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!supportES2()) {
            Toast.makeText(this, "OpenGL ES2 is not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        //imageView = (ImageView)findViewById(R.id.imageViewID);

        btnOpen = (Button)findViewById(R.id.openButton);
        btnSave = (Button)findViewById(R.id.saveButton);
        btnMode = (Button)findViewById(R.id.modeButton);

        renderer = new MyRenderer(this);

        glSurfaceView = (GLSurfaceView)findViewById(R.id.glSurfaceViewID);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        OnClickListener oclBtnOpen = new OnClickListener() {
            @Override
            public void onClick(View view) {
                requestReadPermissions();
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        };

        btnOpen.setOnClickListener(oclBtnOpen);

        glSurfaceView.requestRender();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri imageUri = data.getData();
                    try {
                        bitmapImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        //imageView.setImageBitmap(bitmapImage);
                        renderer.setBitmapImage(bitmapImage);
                    } catch (IOException e) {
                        Toast.makeText(this, "Invalid image", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(this, "NullDataError", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(this, "Sorry, can't load image :c", Toast.LENGTH_LONG).show();
            }
        }
        else {
            //smth for another request
        }

        //glSurfaceView.invalidate();
    }

    private boolean supportES2() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null : "ActivityManger is null";
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    @TargetApi(23)
    public void requestReadPermissions(){
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
}

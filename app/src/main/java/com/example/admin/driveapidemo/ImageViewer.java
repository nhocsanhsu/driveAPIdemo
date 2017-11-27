package com.example.admin.driveapidemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import java.net.URL;

/**
 * Created by Admin on 11/27/2017.
 */

public class ImageViewer extends Activity {
    private ImageView imgViewer;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_viewer);
        imgViewer = (ImageView)findViewById(R.id.imgViewer);
        Intent intent = getIntent();
        //Bitmap image = intent.getExtras().get
        String title = intent.getStringExtra(MainActivity.extra_title);
        try {
            //URL url = new URL(link);
            //Bitmap img = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            //imgViewer.setImageBitmap(img);
        }
        catch (Exception e)
        {

        }

    }
}

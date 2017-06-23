package test.com.ocrtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;



public class MainActivity extends AppCompatActivity {
    CameraView mCameraView;
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = (CameraView) findViewById(R.id.main_camera);
        mImageView = (ImageView) findViewById(R.id.main_image);
        mCameraView.setTag(mImageView);

    }



}

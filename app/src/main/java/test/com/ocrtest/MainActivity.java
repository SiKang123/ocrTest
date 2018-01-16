package test.com.ocrtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;


public class MainActivity extends AppCompatActivity {
    CameraView mCameraView;
    RecycledImageView mImageView;
    private static Demo demo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (demo == null)
            demo = new Demo();
        setContentView(R.layout.activity_main);
        mCameraView = (CameraView) findViewById(R.id.main_camera);
        mImageView = (RecycledImageView) findViewById(R.id.main_image);
        mCameraView.setTag(mImageView);
    }

    class Demo {

    }


}

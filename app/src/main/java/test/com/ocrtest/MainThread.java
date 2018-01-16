package test.com.ocrtest;

import android.os.Handler;
import android.os.Looper;


public class MainThread {
    private static MainThread mPlatform;
    private Handler handler;

    private MainThread() {
        handler = new Handler(Looper.getMainLooper());
    }

    public static MainThread getInstance() {
        if (mPlatform == null)
            synchronized (MainThread.class) {
                if (mPlatform == null)
                    mPlatform = new MainThread();
            }
        return mPlatform;
    }


    public void execute(Runnable runnable) {
        handler.post(runnable);
    }

}

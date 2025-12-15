package org.tensorflow.lite.examples.objectdetection;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.os.Build;
import android.util.Rational;

public class PipController {

    private final Activity activity;

    public PipController(Activity activity) {
        this.activity = activity;
    }

    public void enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams params =
                new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(3, 4)) // CameraX のアスペクト比に合わせる
                    .build();
            activity.enterPictureInPictureMode(params);
        }
    }
}

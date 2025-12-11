package org.tensorflow.lite.examples.objectdetection.fragments;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import org.tensorflow.lite.examples.objectdetection.R;

public class PermissionsFragmentDirections {
  private PermissionsFragmentDirections() {
  }

  @CheckResult
  @NonNull
  public static NavDirections actionPermissionsToCamera() {
    return new ActionOnlyNavDirections(R.id.action_permissions_to_camera);
  }
}

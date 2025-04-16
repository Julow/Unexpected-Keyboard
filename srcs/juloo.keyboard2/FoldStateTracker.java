package juloo.keyboard2;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;
import androidx.window.layout.DisplayFeature;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowInfoTracker;
import androidx.window.layout.WindowLayoutInfo;

import androidx.core.util.Consumer;


public class FoldStateTracker {
    private final Consumer<WindowLayoutInfo> _innerListener;
    private final WindowInfoTrackerCallbackAdapter _windowInfoTracker;
    private FoldingFeature _foldingFeature = null;
    private Runnable _changedCallback = null;
    public FoldStateTracker(Context context) {
        _windowInfoTracker =
                new WindowInfoTrackerCallbackAdapter(WindowInfoTracker.getOrCreate(context));
        _innerListener = new LayoutStateChangeCallback();
        _windowInfoTracker.addWindowLayoutInfoListener(context, Runnable::run, _innerListener);
    }

    public static boolean isFoldableDevice(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE);
    }

    public boolean isUnfolded() {
        // FoldableFeature is only present when the device is unfolded. Otherwise, it's removed.
        // A weird decision from Google, but that's how it works:
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:window/window/src/main/java/androidx/window/layout/adapter/sidecar/SidecarAdapter.kt;l=187?q=SidecarAdapter

        return _foldingFeature != null;
    }

    public void close() {
        _windowInfoTracker.removeWindowLayoutInfoListener(_innerListener);
    }

    public void setChangedCallback(Runnable _changedCallback) {
        this._changedCallback = _changedCallback;
    }

    class LayoutStateChangeCallback implements Consumer<WindowLayoutInfo> {
        @Override
        public void accept(WindowLayoutInfo newLayoutInfo) {
            FoldingFeature old = _foldingFeature;
            _foldingFeature = null;
            for (DisplayFeature feature: newLayoutInfo.getDisplayFeatures()) {
                if (feature instanceof FoldingFeature) {
                    _foldingFeature = (FoldingFeature) feature;
                }
            }

            if (old != _foldingFeature && _changedCallback != null) {
                _changedCallback.run();
            }
        }
    }
}

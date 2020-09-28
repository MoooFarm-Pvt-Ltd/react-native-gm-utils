package com.reactlibrary;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.IllegalViewOperationException;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

public class GmUtilsModule extends ReactContextBaseJavaModule {

    private static final String TAG = "GmUtils";
    private final ReactApplicationContext reactContext;

    public GmUtilsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "GmUtils";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    private Context getAppContext() {
        return this.reactContext.getApplicationContext();
    }

    //-- ASK FOR DRAW OVER OTHER APPS PERMISSION.

    @ReactMethod
    public void askForOverlayPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this.getAppContext())){
            Context context = getAppContext();
            String packageName = context.getApplicationContext().getPackageName();

            Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            this.getAppContext().startActivity(myIntent);
        }
    }

    //-- LAUNCH APP FROM BACKGROUND

    @ReactMethod
    public void backToForeground() {
        Context context = getAppContext();
        String packageName = context.getApplicationContext().getPackageName();
        Intent focusIntent = context.getPackageManager().getLaunchIntentForPackage(packageName).cloneFilter();
        Activity activity = getCurrentActivity();
        boolean isOpened = activity != null;
        Log.d(TAG, "backToForeground, PackageName" + packageName);
        Log.d(TAG, "backToForeground, app isOpened ?" + (isOpened ? "true" : "false"));

        if (isOpened) {
            focusIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(focusIntent);
            Log.d(TAG, "backToForeground, Start from background");
        } else {
            focusIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK +
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED +
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD +
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON +
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON +
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            getReactApplicationContext().startActivity(focusIntent);

            Log.d(TAG, "backToForeground, Start from Killed");
        }
    }

    //-- UNLOCK SCREEN LOCK

    @ReactMethod
    public void unlock() {
        Log.d(TAG, "manualTurnScreenOn()");
        runOnUiThread(new Runnable() {
            public void run() {
                Activity mCurrentActivity = getCurrentActivity();
                if (mCurrentActivity == null) {
                    Log.d(TAG, "ReactContext doesn't hava any Activity attached.");
                    return;
                }
                KeyguardManager keyguardManager = (KeyguardManager) reactContext.getSystemService(Context.KEYGUARD_SERVICE);
                KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock(Context.KEYGUARD_SERVICE);
                keyguardLock.disableKeyguard();

                PowerManager powerManager = (PowerManager) reactContext.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK
                                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                                | PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                | PowerManager.ON_AFTER_RELEASE, "RNUnlockDeviceModule");

                wakeLock.acquire();

                Window window = mCurrentActivity.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        });
    }

    /**
     * Returns true if the device is locked or screen turned off (in case password not set)
     */
    @ReactMethod
    public boolean isDeviceLocked() {

        Context context = this.getAppContext();

        boolean isLocked = false;

        // First we check the locked state
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean inKeyguardRestrictedInputMode = keyguardManager.inKeyguardRestrictedInputMode();

        if (inKeyguardRestrictedInputMode) {
            isLocked = true;

        } else {
            // If password is not set in the settings, the inKeyguardRestrictedInputMode() returns false,
            // so we need to check if screen on for this case

            PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                isLocked = !powerManager.isInteractive();
            } else {
                //noinspection deprecation
                isLocked = !powerManager.isScreenOn();
            }
        }

        //Log.d(String.format("Now device is %s.", isLocked ? "locked" : "unlocked"));
        return isLocked;
    }

    //-- BOTTOM BAR COLOR METHODS

    private static final String ERROR_NO_ACTIVITY = "E_NO_ACTIVITY";
    private static final String ERROR_NO_ACTIVITY_MESSAGE = "Tried to change the navigation bar while not attached to an Activity";
    private static final String ERROR_API_LEVEL = "API_LEVEl";
    private static final String ERROR_API_LEVEL_MESSAGE = "Only Android Oreo and above is supported";
    private static final int UI_FLAG_HIDE_NAV_BAR = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    @ReactMethod
    public void changeNavigationBarColor(final String color, final Boolean light, final Boolean animated, final Promise promise) {
        final WritableMap map = Arguments.createMap();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getCurrentActivity() != null) {
                try {
                    final Window window = getCurrentActivity().getWindow();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (color.equals("transparent") || color.equals("translucent")) {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                                if (color.equals("transparent")) {
                                    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                                } else {
                                    window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                                }
                                setNavigationBarTheme(getCurrentActivity(), light);
                                map.putBoolean("success", true);
                                promise.resolve(map);
                                return;
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                            }
                            if (animated) {
                                Integer colorFrom = window.getNavigationBarColor();
                                Integer colorTo = Color.parseColor(String.valueOf(color));
                                //window.setNavigationBarColor(colorTo);
                                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animator) {
                                        window.setNavigationBarColor((Integer) animator.getAnimatedValue());
                                    }
                                });
                                colorAnimation.start();
                            } else {
                                window.setNavigationBarColor(Color.parseColor(String.valueOf(color)));
                            }
                            setNavigationBarTheme(getCurrentActivity(), light);
                            WritableMap map = Arguments.createMap();
                            map.putBoolean("success", true);
                            promise.resolve(map);
                        }
                    });
                } catch (IllegalViewOperationException e) {
                    map.putBoolean("success", false);
                    promise.reject("error", e);
                }

            } else {
                promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));

            }
        } else {
            promise.reject(ERROR_API_LEVEL, new Throwable(ERROR_API_LEVEL_MESSAGE));
        }
    }

    public void setNavigationBarTheme(Activity activity, Boolean light) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Window window = activity.getWindow();
            int flags = window.getDecorView().getSystemUiVisibility();
            if (light) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }
}

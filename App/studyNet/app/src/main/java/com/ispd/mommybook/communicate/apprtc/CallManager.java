/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.ispd.mommybook.communicate.apprtc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.ispd.mommybook.R;
import com.ispd.mommybook.communicate.CommunicateView;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CustomVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;

import static com.ispd.mommybook.MainHandlerMessages.COMMUNICATION_STOP;
import static org.webrtc.EglBase.configBuilder;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallManager implements AppRTCClient.SignalingEvents,
                                                      PeerConnectionClient.PeerConnectionEvents,
                                                      CallFragment.OnCallEvents {
  private static final String TAG = "CallRTCClient";

  public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
  public static final String EXTRA_URLPARAMETERS = "org.appspot.apprtc.URLPARAMETERS";
  public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
  public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
  public static final String EXTRA_SCREENCAPTURE = "org.appspot.apprtc.SCREENCAPTURE";
  public static final String EXTRA_CAMERA2 = "org.appspot.apprtc.CAMERA2";
  public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
  public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
  public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
  public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
      "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
  public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
  public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
  public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
  public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "org.appspot.apprtc.CAPTURETOTEXTURE";
  public static final String EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC";
  public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
  public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
  public static final String EXTRA_NOAUDIOPROCESSING_ENABLED =
      "org.appspot.apprtc.NOAUDIOPROCESSING";
  public static final String EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP";
  public static final String EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED =
      "org.appspot.apprtc.SAVE_INPUT_AUDIO_TO_FILE";
  public static final String EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES";
  public static final String EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC";
  public static final String EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC";
  public static final String EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS";
  public static final String EXTRA_DISABLE_WEBRTC_AGC_AND_HPF =
      "org.appspot.apprtc.DISABLE_WEBRTC_GAIN_CONTROL";
  public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
  public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
  public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
  public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
  public static final String EXTRA_VIDEO_FILE_AS_CAMERA = "org.appspot.apprtc.VIDEO_FILE_AS_CAMERA";
  public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE =
      "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE";
  public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH =
      "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH";
  public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT =
      "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT";
  public static final String EXTRA_USE_VALUES_FROM_INTENT =
      "org.appspot.apprtc.USE_VALUES_FROM_INTENT";
  public static final String EXTRA_DATA_CHANNEL_ENABLED = "org.appspot.apprtc.DATA_CHANNEL_ENABLED";
  public static final String EXTRA_ORDERED = "org.appspot.apprtc.ORDERED";
  public static final String EXTRA_MAX_RETRANSMITS_MS = "org.appspot.apprtc.MAX_RETRANSMITS_MS";
  public static final String EXTRA_MAX_RETRANSMITS = "org.appspot.apprtc.MAX_RETRANSMITS";
  public static final String EXTRA_PROTOCOL = "org.appspot.apprtc.PROTOCOL";
  public static final String EXTRA_NEGOTIATED = "org.appspot.apprtc.NEGOTIATED";
  public static final String EXTRA_ID = "org.appspot.apprtc.ID";
  public static final String EXTRA_ENABLE_RTCEVENTLOG = "org.appspot.apprtc.ENABLE_RTCEVENTLOG";

  private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

  // List of mandatory application permissions.
  private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
      "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

  // Peer connection statistics callback period in ms.
  private static final int STAT_CALLBACK_PERIOD = 1000;

  private static class ProxyVideoSink implements VideoSink {
    private VideoSink target;

    @Override
    synchronized public void onFrame(VideoFrame frame) {
      if (target == null) {
        Logging.d(TAG, "Dropping frame in proxy because target is null.");
        return;
      }

      target.onFrame(frame);
    }

    synchronized public void setTarget(VideoSink target) {
      this.target = target;
    }
  }

  private Context mContext;
  private Handler mMainHandler;

  private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();
  private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
  @Nullable
  private PeerConnectionClient peerConnectionClient;
  @Nullable
  private AppRTCClient appRtcClient;
  @Nullable
  private AppRTCClient.SignalingParameters signalingParameters;
  @Nullable private AppRTCAudioManager audioManager;
  @Nullable
  private SurfaceViewRenderer pipRenderer;
  @Nullable
  private SurfaceViewRenderer fullscreenRenderer;
  @Nullable
  private VideoFileRenderer videoFileRenderer;
  private final List<VideoSink> remoteSinks = new ArrayList<>();
  private Toast logToast;
  private boolean commandLineRun;
  private boolean activityRunning;
  private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
  @Nullable
  private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
  private boolean connected;
  private boolean isError;
  private boolean callControlFragmentVisible = true;
  private long callStartedTimeMs;
  private boolean micEnabled = true;
  private boolean screencaptureEnabled;
  private static Intent mediaProjectionPermissionResultData;
  private static int mediaProjectionPermissionResultCode;
  // True if local view is in the fullscreen renderer.
  private boolean isSwappedFeeds;

  // Controls
  private CallFragment callFragment;
  private HudFragment hudFragment;
  private CpuMonitor cpuMonitor;

  //private glEngine mGlEngine;
  private EglBase mEGLBase;
  private int mTextureID = -1;
  private boolean mUseOES = false;

  private CommunicateView.RemoteCoordinateCallback mRemoteCoordinateCallback = null;

  // TODO(bugs.webrtc.org/8580): LayoutParams.FLAG_TURN_SCREEN_ON and
  // LayoutParams.FLAG_SHOW_WHEN_LOCKED are deprecated.
  @SuppressWarnings("deprecation")
  public CallManager(Context context, Handler handler, EGLContext egl_context, int textureID, String inputRoomID) {
    mContext = context;
    mMainHandler = handler;
    Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler((Activity)mContext));

    connected = false;
    signalingParameters = null;

    // Create UI controls.
    pipRenderer = ((Activity)mContext).findViewById(R.id.pip_video_view);
//    fullscreenRenderer = ((Activity)mContext).findViewById(R.id.fullscreen_video_view);
    callFragment = new CallFragment();
    hudFragment = new HudFragment();

    // Show/hide call control fragment on view click.
    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        toggleCallControlFragmentVisibility();
      }
    };

    // Swap feeds on pip view click.
    pipRenderer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setSwappedFeeds(!isSwappedFeeds);
      }
    });

//    fullscreenRenderer.setOnClickListener(listener);
    remoteSinks.add(remoteProxyRenderer);

    mTextureID = textureID;

    int[] CONFIG_PLAIN = configBuilder().createConfigAttributes();
    final EglBase eglBase = EglBase.createEgl14(egl_context, CONFIG_PLAIN);
    mEGLBase = eglBase;

    // Create video renderers.
    pipRenderer.init(eglBase.getEglBaseContext(), null);
    pipRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);

    //20210319 change here...
    //String saveRemoteVideoToFile = intent.getStringExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
    String saveRemoteVideoToFile = null;

    // When saveRemoteVideoToFile is set we save the video from the remote to a file.
    if (saveRemoteVideoToFile != null) {
//      int videoOutWidth = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
//      int videoOutHeight = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
//      try {
//        videoFileRenderer = new VideoFileRenderer(
//            saveRemoteVideoToFile, videoOutWidth, videoOutHeight, eglBase.getEglBaseContext());
//        remoteSinks.add(videoFileRenderer);
//      } catch (IOException e) {
//        throw new RuntimeException(
//            "Failed to open video file for output: " + saveRemoteVideoToFile, e);
//      }
    }
//    fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
//    fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL);

    pipRenderer.setZOrderMediaOverlay(true);
    pipRenderer.setEnableHardwareScaler(true /* enabled */);
    pipRenderer.setMirror(true);

//    fullscreenRenderer.setEnableHardwareScaler(true /* enabled */);
    // Start with local feed in fullscreen and swap it to the pip when the call is connected.
    setSwappedFeeds(true /* isSwappedFeeds */);

    // Check for mandatory permissions.
//    for (String permission : MANDATORY_PERMISSIONS) {
//      if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
//        logAndToast("Permission " + permission + " is not granted");
//        setResult(RESULT_CANCELED);
//        finish();
//        return;
//      }
//    }

    //20210319 change here...
//    Uri roomUri = intent.getData();
//    Uri roomUri = Uri.parse("https://appr.tc");
    Uri roomUri = Uri.parse("http://localhost:4000/");
//    if (roomUri == null) {
//      logAndToast(getString(R.string.missing_url));
//      Log.e(TAG, "Didn't get any URL in intent!");
//      setResult(RESULT_CANCELED);
//      finish();
//      return;
//    }

    String roomId = inputRoomID;

    // Get Intent parameters.
    //20210319 change here...
//    String roomId = intent.getStringExtra(EXTRA_ROOMID);
    //String roomId = "ispd0191";
    Log.d(TAG, "Room ID: " + roomId);
//    if (roomId == null || roomId.length() == 0) {
//      logAndToast(getString(R.string.missing_url));
//      Log.e(TAG, "Incorrect room ID in intent!");
//      setResult(RESULT_CANCELED);
//      finish();
//      return;
//    }

    //20210319 change here...
//    boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
//    boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);
//
//    int videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0);
//    int videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0);

    boolean loopback = false;
    boolean tracing = false;

    int videoWidth = 0;
    int videoHeight = 0;

//    screencaptureEnabled = intent.getBooleanExtra(EXTRA_SCREENCAPTURE, false);
    screencaptureEnabled = false;
    // If capturing format is not specified for screencapture, use screen resolution.
    if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
      DisplayMetrics displayMetrics = getDisplayMetrics();
      videoWidth = displayMetrics.widthPixels;
      videoHeight = displayMetrics.heightPixels;
    }

    //20210319 change here...
    PeerConnectionClient.DataChannelParameters dataChannelParameters = null;
//    if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
    if ( true ) {
//      dataChannelParameters = new PeerConnectionClient.DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
//          intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
//          intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
//          intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));

      dataChannelParameters = new PeerConnectionClient.DataChannelParameters(true,
              -1,
              -1, "",
              false, -1);
    }
    peerConnectionParameters =
//        new PeerConnectionClient.PeerConnectionParameters(intent.getBooleanExtra(EXTRA_VIDEO_CALL, true), loopback,
//            tracing, videoWidth, videoHeight, intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
//            intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0), intent.getStringExtra(EXTRA_VIDEOCODEC),
//            intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
//            intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false),
//            intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0), intent.getStringExtra(EXTRA_AUDIOCODEC),
//            intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
//            intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
//            intent.getBooleanExtra(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false),
//            intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
//            intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
//            intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
//            intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
//            intent.getBooleanExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
//            intent.getBooleanExtra(EXTRA_ENABLE_RTCEVENTLOG, false), dataChannelParameters);
//    commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
//    int runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);

//    Log.d(TAG, "VIDEO_FILE: '" + intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA) + "'");

            new PeerConnectionClient.PeerConnectionParameters(true, loopback,
                    tracing, videoWidth, videoHeight, 0,
                    //0, "VP8",
                    0, "H264 High",//VP8//H264 High
                    true,//khkim true
                    false,
                   0, "OPUS",
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false, dataChannelParameters);
    commandLineRun = false;
    int runTimeMs = 0;

    // Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
    // standard WebSocketRTCClient.
    if (loopback || !DirectRTCClient.IP_PATTERN.matcher(roomId).matches()) {
      appRtcClient = new WebSocketRTCClient(this);
    } else {
      Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
      appRtcClient = new DirectRTCClient(this);
    }
    // Create connection parameters.
//    String urlParameters = intent.getStringExtra(EXTRA_URLPARAMETERS);
    String urlParameters = null;
    roomConnectionParameters =
        new AppRTCClient.RoomConnectionParameters(roomUri.toString(), roomId, loopback, urlParameters);

    // Create CPU monitor
    if (CpuMonitor.isSupported()) {
      cpuMonitor = new CpuMonitor(mContext);
      hudFragment.setCpuMonitor(cpuMonitor);
    }

    Bundle bundle = new Bundle();

    bundle.putBoolean("org.appspot.apprtc.CAMERA2", true);
    bundle.putBoolean("org.appspot.apprtc.DISPLAY_HUD", false);
    bundle.putInt("org.appspot.apprtc.VIDEO_BITRATE", 0);
    bundle.putBoolean("org.appspot.apprtc.CMDLINE", false);
    bundle.putInt("org.appspot.apprtc.VIDEO_HEIGHT", 0);
    bundle.putString("org.appspot.apprtc.AUDIOCODEC", "OPUS");
    bundle.putInt("org.appspot.apprtc.VIDEO_WIDTH", 0);
    bundle.putBoolean("org.appspot.apprtc.HWCODEC", true);
    bundle.putInt("org.appspot.apprtc.VIDEO_FPS", 0);
    bundle.putBoolean("org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER", false);
    bundle.putBoolean("org.appspot.apprtc.NOAUDIOPROCESSING", false);
    bundle.putInt("org.appspot.apprtc.MAX_RETRANSMITS_MS", -1);
    bundle.putInt("org.appspot.apprtc.RUNTIME", 0);
    bundle.putBoolean("org.appspot.apprtc.LOOPBACK", false);
    bundle.putString("org.appspot.apprtc.VIDEOCODEC", "VP8");
    bundle.putBoolean("org.appspot.apprtc.VIDEO_CALL", true);
    bundle.putBoolean("org.appspot.apprtc.SAVE_INPUT_AUDIO_TO_FILE", false);
    bundle.putInt("org.appspot.apprtc.MAX_RETRANSMITS", -1);
    bundle.putBoolean("org.appspot.apprtc.DISABLE_BUILT_IN_AEC", false);
    bundle.putBoolean("org.appspot.apprtc.DISABLE_BUILT_IN_AGC", false);
    bundle.putBoolean("org.appspot.apprtc.DATA_CHANNEL_ENABLED", true);
    bundle.putBoolean("org.appspot.apprtc.CAPTURETOTEXTURE", true);
    bundle.putBoolean("org.appspot.apprtc.ORDERED", true);
    bundle.putString("org.appspot.apprtc.PROTOCOL", "");
    bundle.putBoolean("org.appspot.apprtc.AECDUMP", false);
    bundle.putBoolean("org.appspot.apprtc.TRACING", false);
    bundle.putInt("org.appspot.apprtc.AUDIO_BITRATE", 0);
    bundle.putBoolean("org.appspot.apprtc.ENABLE_RTCEVENTLOG", false);
    bundle.putBoolean("org.appspot.apprtc.SCREENCAPTURE", false);
    bundle.putBoolean("org.appspot.apprtc.FLEXFEC", false);
    bundle.putBoolean("org.appspot.apprtc.OPENSLES", false);
    bundle.putBoolean("org.appspot.apprtc.NEGOTIATED", false);
    bundle.putBoolean("org.appspot.apprtc.DISABLE_WEBRTC_GAIN_CONTROL", false);
    bundle.putInt("org.appspot.apprtc.ID", -1);
    bundle.putBoolean("org.appspot.apprtc.DISABLE_BUILT_IN_NS", false);
    bundle.putString("org.appspot.apprtc.ROOMID", "ispd1234");

    //20210319 what just UI...
    // Send intent arguments to fragments.
//    callFragment.setArguments(intent.getExtras());
//    hudFragment.setArguments(intent.getExtras());
    callFragment.setArguments(bundle);
    hudFragment.setArguments(bundle);

    callFragment.attachEvent(this);

    // Activate call and HUD fragments and start the call.
//    FragmentTransaction ft = ((Activity)mContext).getFragmentManager().beginTransaction();
//    ft.add(R.id.call_fragment_container, callFragment);
//    ft.add(R.id.hud_fragment_container, hudFragment);
//    ft.commit();

    // For command line execution run connection for <runTimeMs> and exit.
    if (commandLineRun && runTimeMs > 0) {
      (new Handler()).postDelayed(new Runnable() {
        @Override
        public void run() {
          disconnect();
        }
      }, runTimeMs);
    }

    // Create peer connection client.
    peerConnectionClient = new PeerConnectionClient(
        mContext, eglBase, peerConnectionParameters, CallManager.this);
    PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
    if (loopback) {
      options.networkIgnoreMask = 0;
    }
    peerConnectionClient.createPeerConnectionFactory(options);
    if (screencaptureEnabled) {
      startScreenCapture();
    } else {
      startCall();
    }
  }

  //khkim added...
  public void showUISetting(boolean visible)
  {
    if( pipRenderer != null ) {
      if (visible == true) {
        pipRenderer.setVisibility(View.VISIBLE);
      } else {
        pipRenderer.setVisibility(View.GONE);
      }
    }
  }

  @TargetApi(17)
  private DisplayMetrics getDisplayMetrics() {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    WindowManager windowManager =
        (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
    return displayMetrics;
  }

  @TargetApi(19)
  private static int getSystemUiVisibility() {
    int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }
    return flags;
  }

  @TargetApi(21)
  private void startScreenCapture() {
    MediaProjectionManager mediaProjectionManager =
        (MediaProjectionManager) mContext.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE);
//    startActivityForResult(
//        mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
  }

//  @Override
//  public void onActivityResult(int requestCode, int resultCode, Intent data) {
//    if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
//      return;
//    mediaProjectionPermissionResultCode = resultCode;
//    mediaProjectionPermissionResultData = data;
//    startCall();
//  }

  private boolean useCamera2() {
    //return Camera2Enumerator.isSupported(this) && getIntent().getBooleanExtra(EXTRA_CAMERA2, true);
    return true;
  }

  private boolean captureToTexture() {
//    return getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);

    Log.d(TAG, "captureToTexture");

    return true;
  }

  private @Nullable VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
    final String[] deviceNames = enumerator.getDeviceNames();

    // First, try to find front facing camera
    Logging.d(TAG, "Looking for front facing cameras.");
    for (String deviceName : deviceNames) {
      if (enumerator.isFrontFacing(deviceName)) {
        Logging.d(TAG, "Creating front facing camera capturer.");
        VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

        if (videoCapturer != null) {
          return videoCapturer;
        }
      }
    }

    // Front facing camera not found, try something else
    Logging.d(TAG, "Looking for other cameras.");
    for (String deviceName : deviceNames) {
      if (!enumerator.isFrontFacing(deviceName)) {
        Logging.d(TAG, "Creating other camera capturer.");
        VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

        if (videoCapturer != null) {
          return videoCapturer;
        }
      }
    }

    return null;
  }

  @TargetApi(21)
  private @Nullable VideoCapturer createScreenCapturer() {
    if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
      reportError("User didn't give permission to capture the screen.");
      return null;
    }
    return new ScreenCapturerAndroid(
        mediaProjectionPermissionResultData, new MediaProjection.Callback() {
      @Override
      public void onStop() {
        reportError("User revoked permission to capture the screen.");
      }
    });
  }

  //khkim call here...
  // Activity interfaces
  //@Override
  public void onStop() {
    //super.onStop();
    activityRunning = false;
    // Don't stop the video when using screencapture to allow user to show other apps to the remote
    // end.
    if (peerConnectionClient != null && !screencaptureEnabled) {
      peerConnectionClient.stopVideoSource();
    }
    if (cpuMonitor != null) {
      cpuMonitor.pause();
    }
  }

  //@Override
  public void onStart() {

    Log.d(TAG, "onStart");

    //super.onStart();
    activityRunning = true;
    // Video is not paused for screencapture. See onPause.
    if (peerConnectionClient != null && !screencaptureEnabled) {
      peerConnectionClient.startVideoSource();
    }
    if (cpuMonitor != null) {
      cpuMonitor.resume();
    }
  }

  //@Override
  protected void onDestroy() {
    Thread.setDefaultUncaughtExceptionHandler(null);
    disconnect();
    if (logToast != null) {
      logToast.cancel();
    }
    activityRunning = false;
    //super.onDestroy();
  }

  // CallFragment.OnCallEvents interface implementation.
  @Override
  public void onCallHangUp() {
    disconnect();
  }

  @Override
  public void onCameraSwitch() {
    if (peerConnectionClient != null) {
      peerConnectionClient.switchCamera();
    }
  }

  @Override
  public void onVideoScalingSwitch(ScalingType scalingType) {
//    fullscreenRenderer.setScalingType(scalingType);
  }

  @Override
  public void onCaptureFormatChange(int width, int height, int framerate) {

    Log.d(TAG, "onCaptureFormatChange");

    if (peerConnectionClient != null) {
      peerConnectionClient.changeCaptureFormat(width, height, framerate);
    }
  }

  @Override
  public boolean onToggleMic() {
    if (peerConnectionClient != null) {
      micEnabled = !micEnabled;
      peerConnectionClient.setAudioEnabled(micEnabled);
    }
    return micEnabled;
  }

  //related message khkim...
  //drawData
  public void onTouchDataSend(String text) {
    if (peerConnectionClient != null) {
      peerConnectionClient.sendText(text,"text");
    }
  }

  public void onAIScoreDataSend(String text) {
    if (peerConnectionClient != null) {
      peerConnectionClient.sendTextForAIScore(text,"text");
    }
  }

  //fingerData
  public void onFingerDataSend(float datas[])
  {
    if (peerConnectionClient != null) {
      peerConnectionClient.sendText2_1(datas,"binary");
    }
  }

  //fingerData
  public void onFingerDataSend2(float datas[])
  {
    if (peerConnectionClient != null) {
      peerConnectionClient.sendText2_2(datas,"binary");
    }
  }

  //cropData
  public void onExpandingDataSend(float datas[])
  {
    if (peerConnectionClient != null) {
      peerConnectionClient.sendText3(datas,"binary");
    }
  }

  public boolean getConnectInfo()
  {
    return connected;
  }

  // Helper functions.
  private void toggleCallControlFragmentVisibility() {
    if (!connected || !callFragment.isAdded()) {
      return;
    }
    // Show/hide call control fragment
    callControlFragmentVisible = !callControlFragmentVisible;
    FragmentTransaction ft = ((Activity)mContext).getFragmentManager().beginTransaction();
    if (callControlFragmentVisible) {
      ft.show(callFragment);
      ft.show(hudFragment);
    } else {
      ft.hide(callFragment);
      ft.hide(hudFragment);
    }
    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    ft.commit();
  }

  private void startCall() {

    Log.d(TAG, "startCall");

    if (appRtcClient == null) {
      Log.e(TAG, "AppRTC client is not allocated for a call.");
      return;
    }
    callStartedTimeMs = System.currentTimeMillis();

    // Start room connection.
    logAndToast(mContext.getString(R.string.connecting_to, roomConnectionParameters.roomUrl));
    appRtcClient.connectToRoom(roomConnectionParameters);

    // Create and audio manager that will take care of audio routing,
    // audio modes, audio device enumeration etc.
    audioManager = AppRTCAudioManager.create(mContext);
    // Store existing audio settings and change audio mode to
    // MODE_IN_COMMUNICATION for best possible VoIP performance.
    Log.d(TAG, "Starting the audio manager...");
    audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
      // This method will be called each time the number of available audio
      // devices has changed.
      @Override
      public void onAudioDeviceChanged(
              AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
        onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
      }
    });
  }

  // Should be called from UI thread
  private void callConnected() {

    Log.d(TAG, "callConnected");

    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    Log.i(TAG, "Call connected: delay=" + delta + "ms");
    if (peerConnectionClient == null || isError) {
      Log.w(TAG, "Call is connected in closed or error state");
      return;
    }
    // Enable statistics callback.
    peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    setSwappedFeeds(false /* isSwappedFeeds */);
  }

  // This method is called when the audio manager reports audio device change,
  // e.g. from wired headset to speakerphone.
  private void onAudioManagerDevicesChanged(
          final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
    Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
            + "selected: " + device);
    // TODO(henrika): add callback handler.
  }

  // Disconnect from remote resources, dispose of local resources, and exit.
  private void disconnect() {

    Log.d(TAG, "[disconnect-debug] disconnect");

    mMainHandler.sendEmptyMessage(COMMUNICATION_STOP);

    activityRunning = false;
    remoteProxyRenderer.setTarget(null);
    localProxyVideoSink.setTarget(null);
    if (appRtcClient != null) {
      appRtcClient.disconnectFromRoom();
      appRtcClient = null;
    }
    if (pipRenderer != null) {
      pipRenderer.release();
      pipRenderer = null;
    }
    if (videoFileRenderer != null) {
      videoFileRenderer.release();
      videoFileRenderer = null;
    }
    if (fullscreenRenderer != null) {
      fullscreenRenderer.release();
      fullscreenRenderer = null;
    }
    if (peerConnectionClient != null) {
      peerConnectionClient.close();
      peerConnectionClient = null;
    }
    if (audioManager != null) {
      audioManager.stop();
      audioManager = null;
    }
    if (connected && !isError) {
      ((Activity)mContext).setResult(((Activity)mContext).RESULT_OK);
    } else {
      ((Activity)mContext).setResult(((Activity)mContext).RESULT_CANCELED);
    }
    //((Activity)mContext).finish();
    connected = false;
  }

  private void disconnectWithErrorMessage(final String errorMessage) {
    if (commandLineRun || !activityRunning) {
      Log.e(TAG, "Critical error: " + errorMessage);
      disconnect();
    } else {
      new AlertDialog.Builder(mContext)
          .setTitle(((Activity)mContext).getText(R.string.channel_error_title))
          .setMessage(errorMessage)
          .setCancelable(false)
          .setNeutralButton(R.string.ok,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                  disconnect();
                }
              })
          .create()
          .show();
    }
  }

  // Log |msg| and Toast about it.
  private void logAndToast(String msg) {
    Log.d(TAG, msg);
    if (logToast != null) {
      logToast.cancel();
    }
    logToast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
    logToast.show();
  }

  private void reportError(final String description) {
    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!isError) {
          isError = true;
          disconnectWithErrorMessage(description);
        }
      }
    });
  }

  public void switchCameraView(int texID, boolean useOES)
  {
    peerConnectionClient.setVideoSwitch(texID, useOES);
  }

  //khkim 20210316
  private @Nullable VideoCapturer createVideoCapturer() {
    final VideoCapturer videoCapturer;
//    String videoFileAsCamera = getIntent().getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
    String videoFileAsCamera = "";

    //videoFileAsCamera = "/sdcard/test.mp4";
    if( true )
    {
      try {
        //videoCapturer = new CustomVideoCapturer("CustomThread", mGlEngine.getTextureID(), mEGLBase.getEglBaseContext());
        videoCapturer = new CustomVideoCapturer("CustomThread", mEGLBase.getEglBaseContext(), mTextureID, mUseOES/*true*/);
      } catch (IOException e) {
        reportError("Failed to open custom video file for emulated camera");
        return null;
      }
    }
    else if (videoFileAsCamera != null) {
      try {
        videoCapturer = new FileVideoCapturer(videoFileAsCamera);
      } catch (IOException e) {
        reportError("Failed to open video file for emulated camera");
        return null;
      }
    } else if (screencaptureEnabled) {
      return createScreenCapturer();
    } else if (useCamera2()) {
      if (!captureToTexture()) {
        reportError(((Activity)mContext).getString(R.string.camera2_texture_only_error));
        return null;
      }

      Logging.d(TAG, "Creating capturer using camera2 API.");
      videoCapturer = createCameraCapturer(new Camera2Enumerator(mContext));
    } else {
      Logging.d(TAG, "Creating capturer using camera1 API.");
      videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
    }
    if (videoCapturer == null) {
      reportError("Failed to open camera");
      return null;
    }
    return videoCapturer;
  }

  private void setSwappedFeeds(boolean isSwappedFeeds) {

    Log.d(TAG, "setSwappedFeeds");

    Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
    this.isSwappedFeeds = isSwappedFeeds;
//    localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
//    remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);

//    localProxyVideoSink.setTarget(fullscreenRenderer);
    remoteProxyRenderer.setTarget(pipRenderer);

//    fullscreenRenderer.setMirror(isSwappedFeeds);
//    pipRenderer.setMirror(!isSwappedFeeds);
  }

  // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
  // All callbacks are invoked from websocket signaling looper thread and
  // are routed to UI thread.
  private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {

    Log.d(TAG, "onConnectedToRoomInternal");

    final long delta = System.currentTimeMillis() - callStartedTimeMs;

    signalingParameters = params;
    logAndToast("Creating peer connection, delay=" + delta + "ms");
    VideoCapturer videoCapturer = null;
    if (peerConnectionParameters.videoCallEnabled) {
      videoCapturer = createVideoCapturer();
    }
    peerConnectionClient.createPeerConnection(
        localProxyVideoSink, remoteSinks, videoCapturer, signalingParameters);

    if (signalingParameters.initiator) {
      logAndToast("Creating OFFER...");
      // Create offer. Offer SDP will be sent to answering client in
      // PeerConnectionEvents.onLocalDescription event.
      peerConnectionClient.createOffer();
    } else {
      if (params.offerSdp != null) {
        peerConnectionClient.setRemoteDescription(params.offerSdp);
        logAndToast("Creating ANSWER...");
        // Create answer. Answer SDP will be sent to offering client in
        // PeerConnectionEvents.onLocalDescription event.
        peerConnectionClient.createAnswer();
      }
      if (params.iceCandidates != null) {
        // Add remote ICE candidates from room.
        for (IceCandidate iceCandidate : params.iceCandidates) {
          peerConnectionClient.addRemoteIceCandidate(iceCandidate);
        }
      }
    }
  }

  @Override
  public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {

    Log.d(TAG, "onConnectedToRoom");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        onConnectedToRoomInternal(params);
      }
    });
  }

  @Override
  public void onRemoteDescription(final SessionDescription desc) {

    Log.d(TAG, "onRemoteDescription");

    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (peerConnectionClient == null) {
          Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
          return;
        }
        logAndToast("Received remote " + desc.type + ", delay=" + delta + "ms");
        peerConnectionClient.setRemoteDescription(desc);
        if (!signalingParameters.initiator) {
          logAndToast("Creating ANSWER...");
          // Create answer. Answer SDP will be sent to offering client in
          // PeerConnectionEvents.onLocalDescription event.
          peerConnectionClient.createAnswer();
        }
      }
    });
  }

  @Override
  public void onRemoteIceCandidate(final IceCandidate candidate) {

    Log.d(TAG, "onRemoteIceCandidate");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (peerConnectionClient == null) {
          Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
          return;
        }
        peerConnectionClient.addRemoteIceCandidate(candidate);
      }
    });
  }

  @Override
  public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {

    Log.d(TAG, "onRemoteIceCandidatesRemoved");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (peerConnectionClient == null) {
          Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
          return;
        }
        peerConnectionClient.removeRemoteIceCandidates(candidates);
      }
    });
  }

  @Override
  public void onChannelClose() {

    Log.d(TAG, "onChannelClose");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        logAndToast("Remote end hung up; dropping PeerConnection");
        disconnect();
      }
    });
  }

  @Override
  public void onChannelError(final String description) {

    Log.d(TAG, "onChannelError");

    reportError(description);
  }

  // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
  // Send local peer connection SDP and ICE candidates to remote party.
  // All callbacks are invoked from peer connection client looper thread and
  // are routed to UI thread.
  @Override
  public void onLocalDescription(final SessionDescription desc) {

    Log.d(TAG, "onLocalDescription");

    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (appRtcClient != null) {
          logAndToast("Sending " + desc.type + ", delay=" + delta + "ms");
          if (signalingParameters.initiator) {
            appRtcClient.sendOfferSdp(desc);
          } else {
            appRtcClient.sendAnswerSdp(desc);
          }
        }

        if (peerConnectionParameters.videoMaxBitrate > 0) {
          Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
          peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
        }
      }
    });
  }

  @Override
  public void onIceCandidate(final IceCandidate candidate) {

    Log.d(TAG, "onIceCandidate");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (appRtcClient != null) {
          appRtcClient.sendLocalIceCandidate(candidate);
        }
      }
    });
  }

  @Override
  public void onIceCandidatesRemoved(final IceCandidate[] candidates) {

    Log.d(TAG, "onIceCandidatesRemoved");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (appRtcClient != null) {
          appRtcClient.sendLocalIceCandidateRemovals(candidates);
        }
      }
    });
  }

  @Override
  public void onIceConnected() {

    Log.d(TAG, "onIceConnected");

    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        logAndToast("ICE connected, delay=" + delta + "ms");
      }
    });
  }

  @Override
  public void onIceDisconnected() {

    Log.d(TAG, "[disconnect-debug] onIceDisconnected");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        logAndToast("ICE disconnected");
      }
    });
  }

  @Override
  public void onConnected() {

    Log.d(TAG, "onConnected2");

    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        logAndToast("DTLS connected, delay=" + delta + "ms");
        connected = true;
        callConnected();
      }
    });
  }

  @Override
  public void onDisconnected() {

    Log.d(TAG, "[disconnect-debug] onDisconnected");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        logAndToast("DTLS disconnected");
        connected = false;
        disconnect();
      }
    });
  }

  @Override
  public void onPeerConnectionClosed() {}

  @Override
  public void onPeerConnectionStatsReady(final StatsReport[] reports) {

    Log.d(TAG, "onPeerConnectionStatsReady");

    ((Activity)mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
//        if (!isError && connected) {
//          hudFragment.updateEncoderStatistics(reports);
//        }
      }
    });
  }

  @Override
  public void onPeerConnectionError(final String description) {
    reportError(description);
  }

  //related message khkim...
  @Override
  public void onPeerConnectionGotDataMsg(final String msg) {
    ((Activity)mContext).runOnUiThread(()->mRemoteCoordinateCallback.drawTouchInfo(msg));
  }

  public void setDrawTouchCallBack(CommunicateView.RemoteCoordinateCallback callback)
  {
    mRemoteCoordinateCallback = callback;
  }

  public void DrawPreviewForWebRtc() {
    if( peerConnectionClient != null ) {
      peerConnectionClient.DrawPreviewForWebRtc();
    }
  }
}

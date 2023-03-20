package com.example.droidusbsource;

import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.os.Message;




import android.widget.TextView;
import android.content.pm.ActivityInfo;

import com.xindawn.droidusbsource.MediaControlBrocastFactory;
import com.xindawn.droidusbsource.PhoneSourceService;
import com.xindawn.droidusbsource.Logger;
import android.text.method.ScrollingMovementMethod;
import android.text.TextPaint;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.widget.Toast;



public class MainActivity extends Activity {
    final String TAG = getClass().getSimpleName();
    private PhoneSourceService mPhoneSourceService = null;
    private static ServiceHandler mHandler = null;
    private  TextView mTextView;
    private TextView mLogTextView;
    private Logger mLogger =  new TextLogger();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);
        mLogTextView  = findViewById(R.id.logTextView);
        mLogTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        mHandler = new ServiceHandler();
        Intent intent = new Intent(this, PhoneSourceService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);


        String str = " 　都玩投屏　 ";
        str += "\n";
        str += "让手机更好玩";
        mTextView.setTextColor(Color.RED);
        mTextView.setText(str);
        TextPaint tp = mTextView.getPaint();
        tp.setFakeBoldText(true);
        mTextView.setTextSize(30);

    }



    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        if(mPhoneSourceService != null) {
            if(!mPhoneSourceService.isConnected())
                mPhoneSourceService.start(mLogger);
        }
       // bindService(new Intent(this, PhoneSourceActivity.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        onServiceDisconnecting();

        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
            Log.d(TAG, "@@MainActivity destroy PhoneSourceActivity");
        }

        super.onDestroy();
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPhoneSourceService = ((PhoneSourceService.LocalBinder) service).getService();
            MainActivity.this.onServiceConnected();
            Log.d(TAG, "local PhoneSourceActivity onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "local PhoneSourceActivity onServiceDisconnected");
            MainActivity.this.onServiceDisconnecting();

        }
    };


    public void onServiceConnected() {
        if(mPhoneSourceService != null) {
            mPhoneSourceService.sigPhoneServiceNotify.connect(this, "slotPhoneServiceNotify");
            if(!mPhoneSourceService.isConnected())
                mPhoneSourceService.start(mLogger);
        }
    }

    public void onServiceDisconnecting() {

        if(mPhoneSourceService != null) {
            mPhoneSourceService.sigPhoneServiceNotify.disconnectReceiver(this);
            mPhoneSourceService = null;
        }
    }


    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MediaControlBrocastFactory.EVENT_ID_DEVICE_START: {
                    if(hasVoicePermission())
                        startCaptureService();
                    else
                        requestAudioPermissions();
                    break;
                }
                case  MediaControlBrocastFactory.EVENT_ID_DEVICE_STOP: {
                    stopService(new Intent(MainActivity.this, PhoneSourceService.class));
                    finish();
                    break;
                }
            }
        }
    }

    public void slotPhoneServiceNotify(int event) {
        if(event == MediaControlBrocastFactory.EVENT_ID_DEVICE_START)
            mHandler.obtainMessage(MediaControlBrocastFactory.EVENT_ID_DEVICE_START, (Object)MediaControlBrocastFactory.EVENT_ID_DEVICE_START).sendToTarget();
        else if(event == MediaControlBrocastFactory.EVENT_ID_DEVICE_STOP)
            mHandler.obtainMessage(MediaControlBrocastFactory.EVENT_ID_DEVICE_STOP, (Object)MediaControlBrocastFactory.EVENT_ID_DEVICE_STOP).sendToTarget();
    }



    private static final int RECORD_REQUEST_CODE  = 101;
    private static final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            //init();
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCaptureService();
                } else {
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    private void startCaptureService()
    {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                        Intent captureIntent = projectionManager.createScreenCaptureIntent();
                        startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                    }
                } catch (Exception e){

                }
            }
        }, 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == RECORD_REQUEST_CODE) {
            Log.d(TAG, "onActivityResult: startService resultCode:" + resultCode);
            if(resultCode == RESULT_OK){
                mPhoneSourceService.onActivityResult(requestCode,resultCode,data);
            } else {
            }
        }
    }


    class TextLogger extends Logger {
        @Override
        public void log(final String message) {
            Log.d(TAG, message);
           mLogTextView.post(new Runnable() {
                @Override
                public void run() {
                    mLogTextView.append(message);
                    mLogTextView.append("\n");
                }
            });
        }
    }

    private boolean hasVoicePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(this,android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

}

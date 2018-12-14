package com.example.asus.lab5_candle;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import android.os.Handler;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity {

    private File sdcardfile = null;
    private ImageView imageView;
    private DevicePolicyManager policyManager;
    private ComponentName adminReceiver;
    private int flag;
    boolean admin;
    private Handler mHandler = new Handler();
    //private AudioRecordDemo audioRecordDemo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        init();
        getSDCardFile();
        //audioRecordDemo=new AudioRecordDemo();
        //audioRecordDemo.getNoiseLevel();
        getNoiseLevel();
        imageView=(ImageView)findViewById(R.id.image_view);

    }

    public void init(){
        adminReceiver = new ComponentName(this, ScreenOffAdminReceiver.class);
        policyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        admin = policyManager.isAdminActive(adminReceiver);
        //⑧申请录制音频的动态权限
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{
                    android.Manifest.permission.RECORD_AUDIO},1);
        }else {
        }

        checkAndTurnOnDeviceManager();
        isOpen();

    }

    private void isOpen() {
        if (policyManager.isAdminActive(adminReceiver)) {//判断超级管理员是否激活

            Toast.makeText(this, "设备已被激活", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, "设备没有被激活", Toast.LENGTH_SHORT).show();


        }
    }
    /**
     * ②获取内存卡中文件的方法
     */
    private void getSDCardFile() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {//内存卡存在
            sdcardfile=Environment.getExternalStorageDirectory();//获取目录文件
        }else {
            Toast.makeText(this,"未找到内存卡",Toast.LENGTH_SHORT).show();
        }
    }
    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    Object mLock;

    public void getNoiseLevel() {
        if (isGetVoiceRun) {
            Log.e(TAG, "还在录着呢");
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

        if (mAudioRecord == null) {
            Log.e("sound", "mAudioRecord初始化失败");
        }
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isGetVoiceRun) {
                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // 将 buffer 内容取出，进行平方和运算
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // 平方和除以数据总长度，得到音量大小。
                    double mean = v / (double) r;
                    double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "分贝值:" + volume + "  " + flag);

                    if (volume >= 60) {
                        flag++;
                        if(flag>=5) {
                            imageView.setImageResource(R.drawable.p2);
                            SystemClock.sleep(1000);
                            if (admin) {

                                mHandler.postDelayed(new Runnable(){

                                    @Override
                                    public void run() {

                                        policyManager.lockNow();
                                    }
                                }, 2000);
                                imageView.setImageResource(R.drawable.p1);
                                finish();
                                break;
                            } else {
                                Log.d("abc", "没有设备管理权限");
                            }
                        }

                    } else {
                        flag = 0;
                    }
                    // 大概一秒十次

                    SystemClock.sleep(100);
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }).start();
    }


    public void checkAndTurnOnDeviceManager() {
        Intent intent = new Intent();
        intent.setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
        startActivity(intent);
        admin = policyManager.isAdminActive(adminReceiver);
    }
}

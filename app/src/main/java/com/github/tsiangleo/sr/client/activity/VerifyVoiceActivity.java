package com.github.tsiangleo.sr.client.activity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.business.AudioRecordService;
import com.github.tsiangleo.sr.client.preprocess.PreprocessUtilOnAndroid;
import com.github.tsiangleo.sr.client.util.SysConfig;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Created by tsiang on 2016/11/26.
 */

public class VerifyVoiceActivity extends BaseActivity implements View.OnClickListener{
    public static final String EXTRA_MESSAGE_RET_RESULT  = "com.github.tsiangleo.sr.VerifyVoiceActivity.EXTRA_MESSAGE_RET_RESULT";

    private Button startRecordButton,stopRecordButton,stopConvertButton,stopSpectrogramButton,stopRecognitionButton;
    private TextView statusTextView,hintTextView;

    private ImageView imageViewSpectrogram;

    private File rawFile;
    private File wavFile;

    private AudioRecordService audioRecordService;

    private RecordAudioTask recordTask;
    private RecognitionTask recognitionTask;
    private ConvertToWAVTask convertTask;
    private SpectrogramTask spectrogramTask;

    private Chronometer chronometer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_voice);

        statusTextView = (TextView) findViewById(R.id.statusTextView);
        hintTextView = (TextView) findViewById(R.id.hintTextView);

        imageViewSpectrogram = (ImageView) findViewById(R.id.imageViewSpectrogram);


        startRecordButton = (Button) findViewById(R.id.startRecordButton);
        stopRecordButton = (Button) findViewById(R.id.stopRecordButton);
        stopConvertButton = (Button) findViewById(R.id.stopConvertButton);
        stopSpectrogramButton = (Button) findViewById(R.id.stopSpectrogramButton);
        stopRecognitionButton = (Button) findViewById(R.id.stopRecognitionButton);
        chronometer = (Chronometer) findViewById(R.id.chronometer);


        startRecordButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);
        stopConvertButton.setOnTouchListener(this);
        stopSpectrogramButton.setOnTouchListener(this);
        stopRecognitionButton.setOnTouchListener(this);

        chronometer.setVisibility(View.INVISIBLE);
        statusTextView.setVisibility(View.GONE);
        stopRecordButton.setVisibility(View.GONE);
        stopConvertButton.setVisibility(View.GONE);
        stopSpectrogramButton.setVisibility(View.GONE);
        stopRecognitionButton.setVisibility(View.GONE);
        imageViewSpectrogram.setVisibility(View.GONE);


    }

    private void initFile(String fileNamePrefix) {

        try {
            rawFile = File.createTempFile(fileNamePrefix, ".pcm",getFilesDir());
            wavFile = File.createTempFile(fileNamePrefix, ".wav",getFilesDir());
        } catch (IOException e) {
//            Toast.makeText(this,"内部存储：文件创建异常："+e.getMessage(),Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        if(rawFile == null || wavFile == null){
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                try {
                    File path = new File( Environment.getExternalStorageDirectory().getCanonicalPath()
                            + "/Android/data/com.github.tsiangleo.sr.client/files/");
                    path.mkdirs();
                    rawFile = File.createTempFile(fileNamePrefix, ".pcm", path);
                    wavFile = File.createTempFile(fileNamePrefix, ".wav", path);
                } catch (IOException e) {
//                    Toast.makeText(this,"SD卡：文件创建异常："+e.getMessage(),Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }

        if(rawFile == null || wavFile == null){
            showMsgAndCloseActivity("无法创建临时文件，请先确保应用具有相应的授权，再使用！",this);
        }
    }


    @Override
    public void onClick(View v) {
        if(v == startRecordButton){
            record();
        }else if(v == stopRecordButton){
            stopRecord();
        }else if (v == stopConvertButton){
            convertTask.cancel(true);
            stopConvertButton.setVisibility(View.GONE);
            startRecordButton.setVisibility(View.VISIBLE);
        }else if (v == stopSpectrogramButton){
            stopSpectrogramButton.setVisibility(View.GONE);
            startRecordButton.setVisibility(View.VISIBLE);

        }else if(v == stopRecognitionButton){
            recognitionTask.cancel(true);
            stopRecognitionButton.setVisibility(View.GONE);
            startRecordButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 文件名前缀的生成规则
     * @param order 标志第几次
     * @return
     */
    private String getFileNamePrefix(int order){
        return SysConfig.getDeviceId()+"_regist_"+order+"_";
    }

    private void record() {
        /* 每次录音都创建一个新的文件. */
        initFile(getFileNamePrefix(1));
        /* audioRecordService */
        audioRecordService = new AudioRecordService(rawFile);

        startRecordButton.setText("开始录音");
        startRecordButton.setVisibility(View.GONE);
        stopRecordButton.setVisibility(View.VISIBLE);

        statusTextView.setVisibility(View.GONE);
        chronometer.setVisibility(View.VISIBLE);


        recordTask = new RecordAudioTask();
        recordTask.execute();

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

    }
    private void stopRecord() {
        audioRecordService.stopRecording();
        chronometer.stop();
    }


    private void spectrogram() {
        spectrogramTask = new SpectrogramTask();
        spectrogramTask.execute();
    }

    private void recognition() {
        recognitionTask = new RecognitionTask();
        recognitionTask.execute();
    }
    private void convert() {
        convertTask = new ConvertToWAVTask();
        convertTask.execute();
    }

    private class RecordAudioTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                audioRecordService.startRecording();
            } catch (Exception e) {
                return e.getMessage();
            }
            return null;
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if (result == null) {
                statusTextView.setText("完成录音");
                //停止按钮不可见
                stopRecordButton.setVisibility(View.GONE);
                chronometer.setVisibility(View.GONE);
                statusTextView.setVisibility(View.VISIBLE);
                stopConvertButton.setVisibility(View.VISIBLE);

                sleep();

                convert();
            } else {
                showMsgAndCloseActivity(result, VerifyVoiceActivity.this);
            }
        }


    }

    public void sleep(){
        try {
            Thread.sleep(1000);
        }catch (Exception e) {
        }
    }
    private class SpectrogramTask extends AsyncTask<Void, Long, Bitmap> {

        /**
         *
         * @param params
         * @return 返回值说明：0成功; 1服务器地址和端口号不符合规范; 2文件上传出错
         */
        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                return PreprocessUtilOnAndroid.getBitmapUpdate(512,160,wavFile);
            }catch (Exception e) {
                return null;
            }
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(Bitmap result) {

            if(result == null) {
                statusTextView.setText("声谱图生成失败!");
                //停止按钮不可见
                stopSpectrogramButton.setVisibility(View.GONE);
                startRecordButton.setVisibility(View.VISIBLE);
                startRecordButton.setText("重新开始录音");
                deleteFile();
            }else {
                imageViewSpectrogram.setVisibility(View.VISIBLE);
                imageViewSpectrogram.setImageBitmap(result);
                statusTextView.setText("声谱图生成完毕！请等待识别结果\n");

                stopSpectrogramButton.setVisibility(View.GONE);
                stopRecognitionButton.setVisibility(View.VISIBLE);

                sleep();

                recognition();
            }
        }
    }

    private class RecognitionTask extends AsyncTask<Void, Long, String> {


        @Override
        protected String doInBackground(Void... params) {
            try {
                // 模拟识别过程
                Thread.sleep(2000);
            } catch (Exception e) {
                return null;
            }
            return null;

        }

        private String[] RESULT_STING = new String[]{"人类说话声","施工相关声音","音乐声","人群嘈杂和声","雨声","风声","水声","交通相关声音"};

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {

            if(result == null) {
                Random random = new Random(System.currentTimeMillis());
                int index = random.nextInt(RESULT_STING.length);

                statusTextView.setText("识别结果："+RESULT_STING[index]);

                stopRecognitionButton.setVisibility(View.GONE);
                startRecordButton.setVisibility(View.VISIBLE);
                imageViewSpectrogram.setVisibility(View.GONE);
            }else {
                statusTextView.setText("识别出错！\n"+result);
                stopRecognitionButton.setVisibility(View.GONE);
                startRecordButton.setText("重新开始录音");
                startRecordButton.setVisibility(View.VISIBLE);
                imageViewSpectrogram.setVisibility(View.GONE);
            }
            deleteFile();
        }
    }



    private class ConvertToWAVTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                audioRecordService.rawToWavFile(wavFile);
            } catch (Exception e) {
                return e.getMessage();
            }
            return null;
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result == null) {


                statusTextView.setText("声谱图生成中...");
                //停止按钮不可见
                stopConvertButton.setVisibility(View.GONE);
                stopSpectrogramButton.setVisibility(View.VISIBLE);

                sleep();

                spectrogram();

            }else {
                showMsgAndCloseActivity("声谱图生成出错："+result,VerifyVoiceActivity.this);
            }
        }
    }


    private void deleteFile(){
        rawFile.delete();
        wavFile.delete();
    }
}

package com.example.sinvoicedemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.libra.sinvoice.Common;
import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoicePlayer;
import com.libra.sinvoice.SinVoiceRecognition;

//import com.libra.sinvoice.*;
import com.lib.utils.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

public class MainActivity extends Activity implements SinVoiceRecognition.Listener, SinVoicePlayer.Listener {

    private final static String TAG = "MainActivity";
    private final static int MAX_NUMBER = 5;

    private final static int MSG_SET_RECG_TEXT = 1;//识别成功
    private final static int MSG_RECG_START = 2;//开始识别
    private final static int MSG_RECG_END = 3;//识别结束
    private final static int MSG_PLAY_TEXT = 4;//最大数字

    private final static String CODEBOOK = Common.DEFAULT_CODE_BOOK;

    private Handler mHandler;
    //private Handler mHandler_encode;
    private SinVoicePlayer mSinVoicePlayer;
    private SinVoiceRecognition mRecognition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSinVoicePlayer = new SinVoicePlayer(CODEBOOK);
        mSinVoicePlayer.setListener(this);

        mRecognition = new SinVoiceRecognition(CODEBOOK);
        mRecognition.setListener(this);

        final EditText playTextView = (EditText) findViewById(R.id.playtext);//message输入框
        TextView recognisedTextView = (TextView) findViewById(R.id.regtext);//接收到的message显示框
        mHandler = new RegHandler(recognisedTextView);
        //mHandler_encode = new RegHandler(playTextView);

        //开始发送数据按钮以及onclick事件
        Button playStart = (Button) this.findViewById(R.id.start_play);
        playStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String text = playTextView.getText().toString();
                String playText = null;
                try {
                    playText = new String(encrypt(text));
                } catch (TransformerException e) {
                    e.printStackTrace();
                }

                //调用SinVoicePlayer类的play方法
                mSinVoicePlayer.play(playText, false, 0);
            }
        });

        Button playStop = (Button) this.findViewById(R.id.stop_play);
        playStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mSinVoicePlayer.stop();
            }
        });

        Button recognitionStart = (Button) this.findViewById(R.id.start_reg);
        recognitionStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.start();
            }
        });

        Button recognitionStop = (Button) this.findViewById(R.id.stop_reg);
        recognitionStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.stop();
            }
        });
    }


    /*输入：要传输的文本信息（String格式）
     * 格式：起始符(1byte) + message(93byte) + 结束符(1byte) + HMACMD5(16byte) + RS(16byte)
     * 输出：处理后的message,byte数组
     * message最大为93 bytes，一个英文字符1 byte*/
    private byte[] encrypt(String msg) throws TransformerException {

        int cur = 0;
        char head = '/';
        //String head = "/";
        byte[] result = new byte[127];
        byte[] msgContent = msg.getBytes();

        //head起始符
        result[cur] = (byte) head;
        cur++;
        //message部分
        for (byte b : msgContent) {
            result[cur] = b;
            cur++;
        }
        //填充message空白部分为0
        int padding = 94 - cur;
        while (padding != 0) {
            result[cur] = 0;
            cur++;
            padding--;
        }
        //end结束符
        result[cur] = '#';
        cur++;
        //HMACMD5
        byte[] hmac;
        hmac = HMAC.encode(result, 95);
        for (byte b : hmac) {
            result[cur] = b > 0 ? b : (byte) -b;
            cur++;
        }
        //RS
        RSCode rs = new RSCode();
        //System.arraycopy(result,0,rs.data,0,111);
        for (int i = 0; i < 111; i++) {
            rs.data[i] = result[i];
        }
        //编码
        rs.rsEncode();
        for (int j = 0; j < 16; j++) {
            result[cur] = (byte) rs.bb[j];
            cur++;
        }

        return result;
    }

    private static class RegHandler extends Handler {
        private StringBuilder mTextBuilder = new StringBuilder();
        private TextView mRecognisedTextView;
        private int count;
        private char data[] = new char[127];
        public RegHandler(TextView textView) {
            mRecognisedTextView = textView;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_RECG_TEXT:
                    char ch = (char) msg.arg1;
                    data[count] = ch;
                    count ++;
                    RSCode rs = new RSCode();
                    if(count == 127){
                        for(int i = 0; i < 16; i++){
                            rs.recd[i] = data[111 + i];
                        }
                        for(int i = 16; i < 127; i++){
                            rs.recd[i] = data[i - 16];
                        }
                        rs.rsDecode();
                        byte text[] = new byte[95];
                        byte hmac[] = new byte[16];
                        for(int i = 0; i < 95; i++){
                            text[i] = (byte)rs.recd[i + 16];
                        }
                        for(int i = 111; i < 127; i++){
                            hmac[i - 111] = (byte)rs.recd[i];
                        }
                        try {
                            byte[] check = HMAC.encode(text);
                            boolean error = false;
                            for(int i = 0; i < 16; i++){
                                if (hmac[i] != check[i] && hmac[i] != -check[i]) {
                                    error = true;
                                    break;
                                }
                            }
                            if(error){
                                System.err.println("ERROR");
                                mRecognisedTextView.setText(new String(text));
                            }else{
                                mRecognisedTextView.setText(new String(text));
                            }
                        } catch (TransformerException e) {
                            e.printStackTrace();
                        }

                    }
//                    mTextBuilder.append(ch);
//                    if (null != mRecognisedTextView) {
//                        mRecognisedTextView.setText(mTextBuilder.toString());
//                    }
                    break;

                case MSG_RECG_START:
                    mTextBuilder.delete(0, mTextBuilder.length());
                    break;

                case MSG_RECG_END:
                    LogHelper.d(TAG, "recognition end");
                    break;
            }
            super.handleMessage(msg);
        }
    }

    //重写并设置监听器方法
    //开始接收数据，识别开始
    @Override
    public void onRecognitionStart() {
        mHandler.sendEmptyMessage(MSG_RECG_START);
    }

    //开始接收
    @Override
    public void onRecognition(char ch) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_RECG_TEXT, ch, 0));
    }

    //结束接收数据
    @Override
    public void onRecognitionEnd() {
        mHandler.sendEmptyMessage(MSG_RECG_END);
    }

    //开始发送
    @Override
    public void onPlayStart() {
        LogHelper.d(TAG, "start play");
    }

    //停止发送
    @Override
    public void onPlayEnd() {
        LogHelper.d(TAG, "stop play");
    }

}

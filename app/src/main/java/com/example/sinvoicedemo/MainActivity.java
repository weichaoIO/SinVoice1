package com.example.sinvoicedemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lib.utils.HMAC;
import com.lib.utils.RSCode;
import com.libra.sinvoice.Common;
import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoicePlayer;
import com.libra.sinvoice.SinVoiceRecognition;

import javax.xml.transform.TransformerException;

public class MainActivity extends Activity implements SinVoiceRecognition.Listener, SinVoicePlayer.Listener {
    private final static String TAG = "MainActivity";

    private final static int MSG_SET_RECG_TEXT = 1; // 识别成功
    private final static int MSG_RECG_START = 2;    // 开始识别
    private final static int MSG_RECG_END = 3;      // 识别结束
    private final static int MSG_RESET = 4;         // 重置

    private final static String CODE_BOOK = Common.DEFAULT_CODE_BOOK;
    private final static int TOKEN_START = Common.DEFAULT_TOKEN_START;
    private final static int TOKEN_STOP = Common.DEFAULT_TOKEN_STOP;
    private final static int TOKEN_START_LENGTH = Common.DEFAULT_TOKEN_START_LENGTH;
    private final static int MESSAGE_LENGTH = Common.DEFAULT_MESSAGE_LENGTH;
    private final static int TOKEN_END_LENGTH = Common.DEFAULT_TOKEN_END_LENGTH;
    private final static int HMAC_LENGTH = Common.DEFAULT_HMAC_LENGTH;
    private final static int RS_LENGTH = Common.DEFAULT_RS_LENGTH;
    private final static boolean REPEAT = Common.DEFAULT_REPEAT;
    private final static int GEN_DURATION = Common.DEFAULT_GEN_DURATION;
    private final static int MUTE_INTERVAL = Common.DEFAULT_MUTE_INTERVAL;

    private Handler mHandler;
    private SinVoicePlayer mSinVoicePlayer;
    private SinVoiceRecognition mRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText playTextView = findViewById(R.id.playtext);
        final TextView recognisedTextView = findViewById(R.id.regtext);
        findViewById(R.id.start_play).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mHandler.sendEmptyMessage(MSG_RESET);
                final String text = playTextView.getText().toString();
                String playText = null;
                try {
                    playText = new String(encrypt(text));
                    LogHelper.d(TAG, "playText:" + playText);
                } catch (TransformerException e) {
                    e.printStackTrace();
                }
                mSinVoicePlayer.play(playText, REPEAT, GEN_DURATION, MUTE_INTERVAL);
            }
        });
        findViewById(R.id.stop_play).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mSinVoicePlayer.stop();
            }
        });
        findViewById(R.id.start_reg).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mHandler.sendEmptyMessage(MSG_RESET);
                mRecognition.start();
            }
        });
        findViewById(R.id.stop_reg).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.stop();
            }
        });

        mHandler = new RegHandler(recognisedTextView);

        mSinVoicePlayer = new SinVoicePlayer(CODE_BOOK);
        mSinVoicePlayer.setListener(this);

        mRecognition = new SinVoiceRecognition();
        mRecognition.setListener(this);
    }

    /* 输入：要传输的文本信息（String格式）
     * 格式：起始符(1byte) + message(93byte) + 结束符(1byte) + HMACMD5(16byte) + RS(16byte)
     * 输出：处理后的message,byte数组
     * message最大为93byte，一个英文字符1byte*/
    private byte[] encrypt(String msg) throws TransformerException {// !\"#$0123ABCDabcd
        final byte[] result = new byte[TOKEN_START_LENGTH + MESSAGE_LENGTH + TOKEN_END_LENGTH + HMAC_LENGTH + RS_LENGTH];

        final byte[] msgBytes = msg.getBytes();
        int cur = 0;

        // head起始符
        result[cur] = (byte) TOKEN_START;
        cur++;

        LogHelper.d(TAG, "encrypt result--start:" + result[0]);// 2
        // head起始符

        // message部分
        for (byte b : msgBytes) {
            result[cur] = b;
            cur++;
        }
        // 填充message空白部分为0
        int padding = TOKEN_START_LENGTH + MESSAGE_LENGTH - cur;
        while (padding != 0) {
            result[cur] = 0;
            cur++;
            padding--;
        }

        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i < cur; i++) {
            stringBuilder.append(result[i]).append(" ");
        }

        LogHelper.d(TAG, "encrypt result--message:" + stringBuilder.toString());// 33 34 35 36 48 49 50 51 65 66 67 68 97 98 99 100 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        // message部分

        // end结束符
        result[cur] = TOKEN_STOP;
        cur++;

        LogHelper.d(TAG, "encrypt result--end:" + result[cur - 1]);// 3
        // end结束符

        // HMACMD5
        final byte[] hmac = HMAC.encode(result, TOKEN_START_LENGTH + MESSAGE_LENGTH + TOKEN_END_LENGTH);
        for (byte b : hmac) {
            result[cur] = b > 0 ? b : (byte) -b;
            cur++;
        }

        final StringBuilder stringBuilder1 = new StringBuilder();
        for (int i = cur - HMAC_LENGTH; i < cur; i++) {
            stringBuilder1.append(result[i]).append(" ");
        }
        LogHelper.d(TAG, "encrypt result--hmac:" + stringBuilder1.toString());// 62 102 105 66 16 5 58 67 101 73 105 49 95 107 28 74
        // HMACMD5

        // RS
        final RSCode rs = new RSCode();
        final int rsCount = TOKEN_START_LENGTH + MESSAGE_LENGTH + TOKEN_END_LENGTH + HMAC_LENGTH;
        for (int i = 0; i < rsCount; i++) {
            rs.data[i] = result[i];
        }
        rs.rsEncode();
        for (int j = 0; j < RS_LENGTH; j++) {
            result[cur] = (byte) rs.bb[j];
            cur++;
        }

        final StringBuilder stringBuilder2 = new StringBuilder();
        for (int i = 0; i < RS_LENGTH; i++) {
            stringBuilder2.append((byte) rs.bb[i]).append(" ");
        }
        LogHelper.d(TAG, "encrypt result--rs:" + stringBuilder2.toString());// 73 55 78 70 110 65 59 1 104 98 100 84 96 105 109 10
        // RS

        return result;
    }

    private static class RegHandler extends Handler {
        private TextView mRecognisedTextView;

        private StringBuilder mTextBuilder = new StringBuilder();
        private char[] data;
        private int count;

        public RegHandler(TextView textView) {
            mRecognisedTextView = textView;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_RECG_TEXT:
                    final char ch = (char) msg.arg1;
                    try {
                        data[count] = ch;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                        return;
                    }
                    count++;
                    final int msgSrcLenth = TOKEN_START_LENGTH + MESSAGE_LENGTH + TOKEN_END_LENGTH;
                    final int msgLengthWithoutRs = msgSrcLenth + HMAC_LENGTH;
                    final int msgLength = msgLengthWithoutRs + RS_LENGTH;
                    if (count == msgLength) {
                        final RSCode rs = new RSCode();
                        // recd的前16位是data的后16位
                        for (int i = 0; i < RS_LENGTH; i++) {
                            rs.recd[i] = data[msgLengthWithoutRs + i];
                        }
                        // recd的后111位是data的前111位
                        for (int i = RS_LENGTH; i < msgLength; i++) {
                            rs.recd[i] = data[i - RS_LENGTH];
                        }
                        // 解码
                        rs.rsDecode();

                        final byte msgSrc[] = new byte[msgSrcLenth];
                        // msgSrc是recd的中间95位
                        for (int i = 0; i < msgSrcLenth; i++) {
                            msgSrc[i] = (byte) rs.recd[i + RS_LENGTH];
                        }
                        // hmac是recd的后16位
                        final byte hmac[] = new byte[HMAC_LENGTH];
                        for (int i = msgLengthWithoutRs; i < msgLength; i++) {
                            hmac[i - msgLengthWithoutRs] = (byte) rs.recd[i];
                        }
                        try {
                            // 计算hmac
                            final byte[] check = HMAC.encode(msgSrc);
                            boolean error = false;
                            // 将计算得出的hmac与msg传递的hmac比较
                            for (int i = 0; i < HMAC_LENGTH; i++) {
                                if (hmac[i] != check[i] && hmac[i] != -check[i]) {
                                    error = true;
                                    break;
                                }
                            }
                            if (error) {
                                LogHelper.d(TAG, "recognition failed");
                                mRecognisedTextView.setText("HMAC不匹配");
                            } else {
                                LogHelper.d(TAG, "recognition successed");
                                final String msgSrcStr = new String(msgSrc);
                                mRecognisedTextView.setText(msgSrcStr.substring(1, msgSrcStr.length() - 2));
                            }
                        } catch (TransformerException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MSG_RECG_START:
                    LogHelper.d(TAG, "recognition start");
                    mTextBuilder.delete(0, mTextBuilder.length());
                    break;
                case MSG_RECG_END:
                    LogHelper.d(TAG, "recognition end");
                    break;
                case MSG_RESET:
                    LogHelper.d(TAG, "reset");
                    data = new char[TOKEN_START_LENGTH + MESSAGE_LENGTH + TOKEN_END_LENGTH + HMAC_LENGTH + RS_LENGTH];
                    count = 0;
                    mRecognisedTextView.setText("");
                    break;
            }
        }
    }

    // 开始接收数据，识别开始
    @Override
    public void onRecognitionStart() {
        LogHelper.d(TAG, "onRecognitionStart()");
        mHandler.sendEmptyMessage(MSG_RECG_START);
    }

    // 开始接收
    @Override
    public void onRecognition(char ch) {
        LogHelper.d(TAG, "onRecognition(" + ch + ")");
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_RECG_TEXT, ch, 0));
    }

    // 结束接收数据
    @Override
    public void onRecognitionEnd() {
        LogHelper.d(TAG, "onRecognitionEnd()");
        mHandler.sendEmptyMessage(MSG_RECG_END);
    }

    // 开始发送
    @Override
    public void onPlayStart() {
        LogHelper.d(TAG, "onPlayStart()");
    }

    // 停止发送
    @Override
    public void onPlayEnd() {
        LogHelper.d(TAG, "onPlayEnd()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "停止发送", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
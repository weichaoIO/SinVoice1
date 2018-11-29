/*
 * Copyright (C) 2013 gujicheng
 *
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 *
 * If you have any question, please contact me.
 *
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 */
package com.libra.sinvoice;

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;

import com.libra.sinvoice.Buffer.BufferData;

public class SinVoicePlayer implements Encoder.Listener, Encoder.Callback, PcmPlayer.Listener, PcmPlayer.Callback {
    private final static String TAG = "SinVoicePlayer";

    private final static int STATE_START = 1;   // 启动状态
    private final static int STATE_STOP = 2;    // 结束状态
    private final static int STATE_PENDING = 3; // 挂起、等待状态

    private final static String CODE_BOOK = Common.DEFAULT_CODE_BOOK;
    private final static int SAMPLE_RATE = Common.DEFAULT_SAMPLE_RATE;
    private final static int BUFFER_SIZE = Common.DEFAULT_BUFFER_SIZE;
    private final static int BUFFER_COUNT = Common.DEFAULT_BUFFER_COUNT;
    private final static boolean REPEAT = Common.DEFAULT_REPEAT;
    private final static int GEN_DURATION = Common.DEFAULT_GEN_DURATION;
    private final static int MUTE_INTERVAL = Common.DEFAULT_MUTE_INTERVAL;

    private String mCodeBook = CODE_BOOK;
    private List<Integer> mCodes = new ArrayList<>();// 存放编码的arraylist

    private Encoder mEncoder;
    private PcmPlayer mPlayer;
    private Buffer mBuffer;

    private int mState;
    private Listener mListener;
    private Thread mPlayThread;
    private Thread mEncodeThread;

    public SinVoicePlayer() {
        this(CODE_BOOK);
    }

    public SinVoicePlayer(String codeBook) {
        this(codeBook, SAMPLE_RATE, BUFFER_SIZE, BUFFER_COUNT);
    }

    public SinVoicePlayer(String codeBook, int sampleRate, int bufferSize, int buffCount) {
        mState = STATE_STOP;
        mBuffer = new Buffer(buffCount, bufferSize);
        mEncoder = new Encoder(this, sampleRate, Common.BITS_16, bufferSize);
        mEncoder.setListener(this);
        mPlayer = new PcmPlayer(this, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mPlayer.setListener(this);
        setCodeBook(codeBook);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setCodeBook(String codeBook) {
        mCodeBook = codeBook;
    }

    public void play(final String text) {
        play(text, REPEAT, GEN_DURATION, MUTE_INTERVAL);
    }

    public void play(final String text, final boolean repeat, final int genDuration, final int muteInterval) {
        if (STATE_STOP == mState && null != mCodeBook) {
            mState = STATE_PENDING;

            mCodes = convertTextToCodes(text);

            mPlayThread = new Thread() {
                @Override
                public void run() {
                    LogHelper.d(TAG, "play start");
                    mPlayer.start();
                    LogHelper.d(TAG, "play end");
                }
            };
            mPlayThread.start();

            mEncodeThread = new Thread() {
                @Override
                public void run() {
                    do {
                        LogHelper.d(TAG, "encode start");
                        mEncoder.encode(mCodes, genDuration, muteInterval);
                        LogHelper.d(TAG, "encode end");

                        mEncoder.stop();
                    } while (repeat && STATE_PENDING != mState);
                    stopPlayer();
                }
            };
            mEncodeThread.start();

            mState = STATE_START;
        }
    }

    /**
     * 将要传递的文本信息转换成ASCII编码
     */
    private List<Integer> convertTextToCodes(String text) {
        LogHelper.d(TAG, "convertTextToCodes(" + text + ")");
        final List<Integer> list = new ArrayList<>();

        byte tmp = -1;
        final byte[] bytes = text.getBytes();
        for (byte b : bytes) {
            if (tmp == b) {
                // 连续相同的两个字符，第二个置为128
                list.add(Common.DEFAULT_TOKEN_SAME_AS_BEFORE);
                tmp = -1;
            } else {
                list.add((int) b);
                tmp = b;
            }
        }

        LogHelper.d(TAG, "encrypt result convert:" + list.toString());// 2, 33, 34, 35, 36, 48, 49, 50, 51, 65, 66, 67, 68, 97, 98, 99, 100, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 128, 0, 3, 62, 102, 105, 66, 16, 5, 58, 67, 101, 73, 105, 49, 95, 107, 28, 74, 73, 55, 78, 70, 110, 65, 59, 1, 104, 98, 100, 84, 96, 105, 109, 10

        return list;
    }

    public void stop() {
        LogHelper.d(TAG, "stop()");
        if (STATE_START == mState) {
            mState = STATE_PENDING;

            LogHelper.d(TAG, "force stop start");
            mEncoder.stop();
            if (null != mEncodeThread) {
                try {
                    mEncodeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mEncodeThread = null;
                }
            }
            LogHelper.d(TAG, "force stop end");
        }
    }

    private void stopPlayer() {
        LogHelper.d(TAG, "stopPlayer()");
        if (mEncoder.isStoped()) {
            mPlayer.stop();
        }

        mBuffer.putFull(BufferData.getEmptyBuffer());

        if (null != mPlayThread) {
            try {
                mPlayThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mPlayThread = null;
            }
        }

        mBuffer.reset();
        mState = STATE_STOP;
    }

    @Override
    public void onStartEncode() {
        LogHelper.d(TAG, "onStartEncode()");
    }

    @Override
    public void freeEncodeBuffer(BufferData buffer) {
        LogHelper.d(TAG, "freeEncodeBuffer()");
        if (null != buffer) {
            mBuffer.putFull(buffer);
        }
    }

    @Override
    public BufferData getEncodeBuffer() {
        LogHelper.d(TAG, "getEncodeBuffer()");
        return mBuffer.getEmpty();
    }

    @Override
    public void onEndEncode() {
        LogHelper.d(TAG, "onEndEncode()");
    }

    @Override
    public BufferData getPlayBuffer() {
        LogHelper.d(TAG, "getPlayBuffer()");
        return mBuffer.getFull();
    }

    @Override
    public void freePlayData(BufferData data) {
        LogHelper.d(TAG, "freePlayData()");
        mBuffer.putEmpty(data);
    }

    @Override
    public void onPlayStart() {
        LogHelper.d(TAG, "onPlayStart()");
        if (null != mListener) {
            mListener.onPlayStart();
        }
    }

    @Override
    public void onPlayStop() {
        LogHelper.d(TAG, "onPlayStop()");
        if (null != mListener) {
            mListener.onPlayEnd();
        }
    }

    public interface Listener {
        void onPlayStart();

        void onPlayEnd();
    }
}
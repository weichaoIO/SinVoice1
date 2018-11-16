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
import android.text.TextUtils;

import com.libra.sinvoice.Buffer.BufferData;

public class SinVoicePlayer implements Encoder.Listener, Encoder.Callback, PcmPlayer.Listener, PcmPlayer.Callback {
    private final static String TAG = "SinVoicePlayer";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_PENDING = 3;

    private final static int DEFAULT_GEN_DURATION = 100;

    private String mCodeBook;
    private List<Integer> mCodes = new ArrayList<Integer>();//存放编码的arraylist

    private Encoder mEncoder;
    private PcmPlayer mPlayer;
    private Buffer mBuffer;

    private int mState;
    private Listener mListener;
    private Thread mPlayThread;
    private Thread mEncodeThread;

    //监听器接口
    public static interface Listener {
        void onPlayStart();

        void onPlayEnd();
    }

    //默认编码本
    public SinVoicePlayer() {
        this(Common.DEFAULT_CODE_BOOK);
    }

    //自定义编码本，传入默认的采样率、缓存大小等
    public SinVoicePlayer(String codeBook) {
        this(codeBook, Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_SIZE, Common.DEFAULT_BUFFER_COUNT);
    }

    //自定义编码本、采样率、缓存大小等
    public SinVoicePlayer(String codeBook, int sampleRate, int bufferSize, int buffCount) {
        mState = STATE_STOP;
        mBuffer = new Buffer(buffCount, bufferSize);

        mEncoder = new Encoder(this, sampleRate, SinGenerator.BITS_16, bufferSize);//十六位采样
        mEncoder.setListener(this);
        mPlayer = new PcmPlayer(this, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mPlayer.setListener(this);

        setCodeBook(codeBook);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setCodeBook(String codeBook) {
//        if (!TextUtils.isEmpty(codeBook) && codeBook.length() < Encoder.getMaxCodeCount() - 1) {
            mCodeBook = codeBook;
//        }
    }

    //将要传递的文本信息转换成ASCII编码
    private boolean convertTextToCodes(String text) {
        byte tmp = -1;

        for(byte b: text.getBytes()){
            if(tmp == b){
                mCodes.add(128);
                tmp = -1;
            } else {
                mCodes.add((int)b);
                tmp = b;
            }

        }
       return true;
 }

    public void play(final String text) {
        play(text, false, 0);
    }

    public void play(final String text, final boolean repeat, final int muteInterval) {
        if (STATE_STOP == mState && null != mCodeBook && convertTextToCodes(text)) {
            mState = STATE_PENDING;//挂起、等待状态

            mPlayThread = new Thread() {
                @Override
                public void run() {
                    mPlayer.start();
                }//run方法在new的时候并未执行，只有在start犯法执行时调用run方法的时候方才执行
            };
            //play的线程启动
            if (null != mPlayThread) {
                mPlayThread.start();
            }

            mEncodeThread = new Thread() {
                @Override
                public void run() {
                    do {
                        LogHelper.d(TAG, "encode start");
                        mEncoder.encode(mCodes, DEFAULT_GEN_DURATION, muteInterval);
                        LogHelper.d(TAG, "encode end");

                        mEncoder.stop();
                    } while (repeat && STATE_PENDING != mState);
                    stopPlayer();
                }
            };
            //编码的enocde线程启动
            if (null != mEncodeThread) {
                mEncodeThread.start();
            }

            LogHelper.d(TAG, "play");
            mState = STATE_START;//启动状态
        }
    }

    public void stop() {
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
        if (mEncoder.isStoped()) {
            mPlayer.stop();
        }

        // put end buffer
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
        LogHelper.d(TAG, "onStartGen");
    }

    @Override
    public void freeEncodeBuffer(BufferData buffer) {
        if (null != buffer) {
            mBuffer.putFull(buffer);
        }
    }

    @Override
    public BufferData getEncodeBuffer() {
        return mBuffer.getEmpty();
    }

    @Override
    public void onEndEncode() {
    }

    @Override
    public BufferData getPlayBuffer() {
        return mBuffer.getFull();
    }

    @Override
    public void freePlayData(BufferData data) {
        mBuffer.putEmpty(data);
    }

    @Override
    public void onPlayStart() {
        if (null != mListener) {
            mListener.onPlayStart();
        }
    }

    @Override
    public void onPlayStop() {
        if (null != mListener) {
            mListener.onPlayEnd();
        }
    }

}

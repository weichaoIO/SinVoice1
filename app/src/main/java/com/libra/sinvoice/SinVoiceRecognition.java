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

import android.media.AudioFormat;

import com.lib.utils.FFTRecognition;
import com.libra.sinvoice.Buffer.BufferData;

public class SinVoiceRecognition implements Record.Listener, Record.Callback, FFTRecognition.Callback, FFTRecognition.Listener {
    private final static String TAG = "SinVoiceRecognition";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_PENDING = 3;

    private final static int SAMPLE_RATE = Common.DEFAULT_SAMPLE_RATE;
    private final static int BUFFER_SIZE = Common.DEFAULT_BUFFER_SIZE;
    private final static int BUFFER_COUNT = Common.DEFAULT_BUFFER_COUNT;

    private Buffer mBuffer;
    private Record mRecord;
    private FFTRecognition mRecognition;

    private Thread mRecordThread;
    private Thread mRecognitionThread;
    private int mState;
    private Listener mListener;

    private int lastRecognition = -1;

    public SinVoiceRecognition() {
        this(SAMPLE_RATE, BUFFER_SIZE, BUFFER_COUNT);
    }

    public SinVoiceRecognition(int sampleRate, int bufferSize, int bufferCount) {
        mState = STATE_STOP;

        mBuffer = new Buffer(bufferCount, bufferSize);
        mRecord = new Record(this, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mRecord.setListener(this);
        mRecognition = new FFTRecognition(this);
        mRecognition.setListener(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        LogHelper.d(TAG, "start()");
        if (STATE_STOP == mState) {
            mState = STATE_PENDING;

            mRecognitionThread = new Thread() {
                @Override
                public void run() {
                    mRecognition.start();
                }
            };
            mRecognitionThread.start();

            mRecordThread = new Thread() {
                @Override
                public void run() {
                    mRecord.start();
                    stopRecognition();
                }
            };
            mRecordThread.start();

            mState = STATE_START;
        }
    }

    private void stopRecognition() {
        LogHelper.d(TAG, "stopRecognition()");
        mRecognition.stop();

        final BufferData data = new BufferData(0);
        mBuffer.putFull(data);

        if (null != mRecognitionThread) {
            try {
                mRecognitionThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mRecognitionThread = null;
            }
        }

        mBuffer.reset();
    }

    public void stop() {
        LogHelper.d(TAG, "stop()");
        if (STATE_START == mState) {
            mState = STATE_PENDING;

            LogHelper.d(TAG, "force stop start");
            mRecord.stop();
            if (null != mRecordThread) {
                try {
                    mRecordThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mRecordThread = null;
                }
            }

            mState = STATE_STOP;
            LogHelper.d(TAG, "force stop end");
        }
    }

    @Override
    public void onStartRecord() {
        LogHelper.d(TAG, "onStartRecord()");
    }

    @Override
    public void onStopRecord() {
        LogHelper.d(TAG, "onStopRecord()");
    }

    @Override
    public BufferData getRecordBuffer() {
        LogHelper.d(TAG, "getRecordBuffer()");
        final BufferData buffer = mBuffer.getEmpty();
        if (null == buffer) {
            LogHelper.e(TAG, "get null empty buffer");
        }
        return buffer;
    }

    @Override
    public void freeRecordBuffer(BufferData buffer) {
        LogHelper.d(TAG, "freeRecordBuffer()");
        if (null != buffer) {
            if (!mBuffer.putFull(buffer)) {
                LogHelper.e(TAG, "put full buffer failed");
            }
        }
    }

    @Override
    public BufferData getRecognitionBuffer() {
        LogHelper.d(TAG, "getRecognitionBuffer()");
        final BufferData buffer = mBuffer.getFull();
        if (null == buffer) {
            LogHelper.e(TAG, "get null full buffer");
        }
        return buffer;
    }

    @Override
    public void freeRecognitionBuffer(BufferData buffer) {
        LogHelper.d(TAG, "freeRecognitionBuffer()");
        if (null != buffer) {
            if (!mBuffer.putEmpty(buffer)) {
                LogHelper.e(TAG, "put empty buffer failed");
            }
        }
    }

    @Override
    public void onStartRecognition() {
        LogHelper.d(TAG, "onStartRecognition()");
    }

    @Override
    public void onRecognition(int ascii) {
        LogHelper.d(TAG, "onRecognition(" + ascii + ")");
        if (ascii != lastRecognition) {
            if (ascii == Common.DEFAULT_TOKEN_SAME_AS_BEFORE) {
                mListener.onRecognition((char) lastRecognition);
                LogHelper.d(TAG, "recognition:" + lastRecognition);
            } else {
                mListener.onRecognition((char) ascii);
                LogHelper.d(TAG, "recognition:" + ascii);
            }
            lastRecognition = ascii;
        }
    }

    @Override
    public void onStopRecognition() {
        LogHelper.d(TAG, "onStopRecognition()");
    }

    public interface Listener {
        void onRecognitionStart();

        void onRecognition(char ch);

        void onRecognitionEnd();
    }
}
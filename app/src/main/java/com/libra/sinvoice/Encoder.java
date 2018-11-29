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

import java.util.List;

import com.libra.sinvoice.Buffer.BufferData;

public class Encoder implements SinGenerator.Listener, SinGenerator.Callback {
    private final static String TAG = "Encoder";

    private final static int STATE_ENCODING = 1;
    private final static int STATE_STOPED = 2;

    private int mState;

    private SinGenerator mSinGenerator;
    private Listener mListener;
    private Callback mCallback;

    public Encoder(Callback callback, int sampleRate, int bits, int bufferSize) {
        mState = STATE_STOPED;
        mCallback = callback;
        mSinGenerator = new SinGenerator(this, sampleRate, bits, bufferSize);
        mSinGenerator.setListener(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public final static int getMaxCodeCount() {
        LogHelper.d(TAG, "getMaxCodeCount()");
        return (int) ((20000.0 - Common.BASE_FREQUENCY) / 44100.0 * 2048.0);
    }

    /**
     * 获取字符对应的频率
     */
    public static int getCodeFrequency(int index) {
        return (int) (Common.BASE_FREQUENCY + (double) index * 44100 / 2048);
    }

    public final boolean isStoped() {
        LogHelper.d(TAG, "isStoped()");
        return (STATE_STOPED == mState);
    }

    public void encode(List<Integer> codes, int duration, int muteInterval) {
        if (STATE_STOPED == mState) {
            mState = STATE_ENCODING;

            if (null != mListener) {
                mListener.onStartEncode();
            }

            mSinGenerator.start();

            final int maxCodeCount = getMaxCodeCount();
            LogHelper.d(TAG, "maxCodeCount:" + maxCodeCount);
            final StringBuilder stringBuilder = new StringBuilder();
            for (int ascii : codes) {
                if (STATE_ENCODING == mState) {
                    if (ascii >= 0 && ascii < maxCodeCount) {
                        final int codeFrequency = getCodeFrequency(ascii);
                        stringBuilder.append(codeFrequency).append(" ");
                        mSinGenerator.gen(codeFrequency, duration);
                    } else {
                        LogHelper.e(TAG, "code index error");
                    }
                } else {
                    LogHelper.d(TAG, "encode force stop");
                    break;
                }
            }
            LogHelper.d(TAG, "codeFrequency:" + stringBuilder.toString());
            if (STATE_ENCODING == mState) {
                mSinGenerator.gen(0, muteInterval);
            } else {
                LogHelper.d(TAG, "encode force stop");
            }
            stop();

            if (null != mListener) {
                mListener.onEndEncode();
            }
        }
    }

    public void stop() {
        LogHelper.d(TAG, "stop()");
        if (STATE_ENCODING == mState) {
            mState = STATE_STOPED;

            mSinGenerator.stop();
        }
    }

    @Override
    public void onStartGen() {
        LogHelper.d(TAG, "onStartGen()");
    }

    @Override
    public void onStopGen() {
        LogHelper.d(TAG, "onStopGen()");
    }

    @Override
    public BufferData getGenBuffer() {
        LogHelper.d(TAG, "getGenBuffer()");
        if (null != mCallback) {
            return mCallback.getEncodeBuffer();
        }

        return null;
    }

    @Override
    public void freeGenBuffer(BufferData buffer) {
        LogHelper.d(TAG, "freeGenBuffer()");
        if (null != mCallback) {
            mCallback.freeEncodeBuffer(buffer);
        }
    }

    public interface Listener {
        void onStartEncode();

        void onEndEncode();
    }

    public interface Callback {
        void freeEncodeBuffer(BufferData buffer);

        BufferData getEncodeBuffer();
    }
}
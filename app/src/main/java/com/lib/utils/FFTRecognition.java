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
package com.lib.utils;

import com.libra.sinvoice.Buffer.BufferData;
import com.libra.sinvoice.Common;
import com.libra.sinvoice.LogHelper;

public class FFTRecognition {
    private final static String TAG = "FFTRecognition";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    private final static int SAMPLE_RATE = Common.DEFAULT_SAMPLE_RATE;
    private final static int BASE_INDEX = Common.BASE_INDEX;

    private int mState;
    private Listener mListener;
    private Callback mCallback;

    private int mRegIndex;

    public FFTRecognition(Callback callback) {
        mState = STATE_STOP;

        mCallback = callback;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        LogHelper.d(TAG, "start()");
        if (STATE_STOP == mState) {
            if (null != mCallback) {
                mState = STATE_START;

                if (null != mListener) {
                    mListener.onStartRecognition();
                }

                while (STATE_START == mState) {
                    final BufferData data = mCallback.getRecognitionBuffer();
                    if (null != data) {
                        if (null != data.mData) {
                            process(data);
                            mCallback.freeRecognitionBuffer(data);
                        } else {
                            LogHelper.d(TAG, "end input buffer, so stop");
                            break;
                        }
                    } else {
                        LogHelper.e(TAG, "get null recognition buffer");
                        break;
                    }
                }

                mState = STATE_STOP;
                if (null != mListener) {
                    mListener.onStopRecognition();
                }
            }
        }
    }

    public void stop() {
        LogHelper.d(TAG, "stop()");
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    // 处理的是16位采样数据
    private void process(BufferData data) {
        LogHelper.d(TAG, "process()");
        final int size = data.getFilledSize(); // size=4096 大约46ms 一个字符100ms

        // 16位采样的处理
        final short[] temp1 = new short[size / 2];
        for (int i = 0; i < size; i++) {
            short sh1 = data.mData[i];// 低8位
            sh1 &= 0xff;
            short sh2 = data.mData[++i];// 高8位
            sh2 <<= 8;
            temp1[((i - 1) / 2)] = (short) ((sh1) | (sh2));
        }
        handler(temp1);
        if (mRegIndex >= 0) {
            if (null != mListener) {
                mListener.onRecognition(mRegIndex);
                mRegIndex = -1;
            }
        }
    }

    private double handler(short[] temp1) {
        LogHelper.d(TAG, "handler()");
        final Complex result[] = FFT.fft(temp1);
        final int size = temp1.length;
        double max = Double.MIN_VALUE;
        int maxIndex = -1;
        for (int i = 0; i < size / 2; i++) {
            if (result[i].abs() > max) {
                max = result[i].abs();
                maxIndex = i;
            }
        }

        final double trueHz = (double) maxIndex * SAMPLE_RATE / size;
        mRegIndex = maxIndex - BASE_INDEX;
        return trueHz;
    }

    public interface Listener {
        void onStartRecognition();

        void onRecognition(int index);

        void onStopRecognition();
    }

    public interface Callback {
        BufferData getRecognitionBuffer();

        void freeRecognitionBuffer(BufferData buffer);
    }
}
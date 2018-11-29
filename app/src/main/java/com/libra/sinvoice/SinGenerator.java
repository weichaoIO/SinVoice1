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

import com.libra.sinvoice.Buffer.BufferData;

public class SinGenerator {
    private static final String TAG = "SinGenerator";

    private static final int STATE_START = 1;
    private static final int STATE_STOP = 2;

    private static final int SAMPLE_RATE = Common.DEFAULT_SAMPLE_RATE;
    private static final int BITS = Common.DEFAULT_BITS;
    private static final int BUFFER_SIZE = Common.DEFAULT_BUFFER_SIZE;

    private int mState;
    private int mSampleRate;
    private int mBits;
    private int mDuration;
    private int mGenRate;

    private int mFilledSize;
    private int mBufferSize;
    private Listener mListener;
    private Callback mCallback;

    public SinGenerator(Callback callback) {
        this(callback, SAMPLE_RATE, BITS, BUFFER_SIZE);
    }

    public SinGenerator(Callback callback, int sampleRate, int bits, int bufferSize) {
        mCallback = callback;

        mSampleRate = sampleRate;
        mBits = bits;
        mBufferSize = bufferSize;
        mDuration = 0;

        mFilledSize = 0;
        mState = STATE_STOP;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        LogHelper.d(TAG, "start()");
        if (STATE_STOP == mState) {
            mState = STATE_START;
        }
    }

    public void stop() {
        LogHelper.d(TAG, "stop()");
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    public void gen(int genRate, int duration) {
        LogHelper.d(TAG, "gen(" + genRate + ", " + duration + ")");
        if (STATE_START == mState) {
            mGenRate = genRate;
            mDuration = duration;

            if (null != mListener) {
                mListener.onStartGen();
            }

            if (null != mCallback) {
                mFilledSize = 0;
                // 获取要编码的数据
                BufferData buffer = mCallback.getGenBuffer();
                if (null != buffer) {
                    final int n = mBits / 2;
                    final int totalCount = (mDuration * mSampleRate) / 1000;
                    LogHelper.d(TAG, "sin gen totalCount:" + totalCount);
                    final double per = (mGenRate / (double) mSampleRate) * 2 * Math.PI;
                    double d = 0;
//                    if (genRate == 17054) {
//                        final StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < totalCount; ++i) {
                        if (STATE_START == mState) {
                            // 算出不同点的正弦值
                            final int out = (int) (Math.sin(d) * n);
//                                stringBuilder.append(out).append("\r\n");

                            // 如果填充数量超过了缓冲区的大小，就重置mFilledSize，释放bufferData
                            if (mFilledSize >= mBufferSize - 1) {
                                buffer.setFilledSize(mFilledSize);
                                mCallback.freeGenBuffer(buffer);

                                mFilledSize = 0;
                                buffer = mCallback.getGenBuffer();
                                if (null == buffer) {
                                    LogHelper.e(TAG, "get null buffer");
                                    break;
                                }
                            }

                            // 转码为byte类型并保存，& 0xff是为了防止负数转换出现异常
                            buffer.mData[mFilledSize++] = (byte) (out & 0xff);// 低8位
//                                stringBuilder.append((byte) (out & 0xff)).append("\r\n");
                            if (BITS == mBits) {
                                buffer.mData[mFilledSize++] = (byte) ((out >> 8) & 0xff);// 高8位
//                                    stringBuilder.append((byte) ((out >> 8) & 0xff)).append("\r\n");
                            }

                            d += per;
                        } else {
                            LogHelper.d(TAG, "sin gen force stop");
                            break;
                        }
                    }
//                        FileUtil.writeLog(Common.LOG_PATH, stringBuilder.toString());
//                    }
                } else {
                    LogHelper.e(TAG, "get null buffer");
                }

                if (null != buffer) {
                    buffer.setFilledSize(mFilledSize);
                    mCallback.freeGenBuffer(buffer);
                }
                mFilledSize = 0;

                if (null != mListener) {
                    mListener.onStopGen();
                }
            }
        }
    }

    public interface Listener {
        void onStartGen();

        void onStopGen();
    }

    public interface Callback {
        BufferData getGenBuffer();

        void freeGenBuffer(BufferData buffer);
    }
}
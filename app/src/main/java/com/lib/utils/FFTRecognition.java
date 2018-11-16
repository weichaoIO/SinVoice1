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
import com.libra.sinvoice.LogHelper;

public class FFTRecognition {
    private final static String TAG = "Recognition";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;


    private final static int MAX_SAMPLING_POINT_COUNT = 31;
    private final static int MIN_REG_CIRCLE_COUNT = 10;

    private int mState;
    private Listener mListener;
    private Callback mCallback;

    private int mSamplingPointCount = 0;

    private int mSampleRate;
    private int mChannel;
    private int mBits;

    private boolean mIsStartCounting = false;
    private int mStep;
    private boolean mIsBeginning = false;
    private boolean mStartingDet = false;
    private int mStartingDetCount;

    private int mRegValue;
    private int mRegIndex;
    private int mRegCount;
    private int mPreRegCircle;
    private boolean mIsRegStart = false;

    public static interface Listener {
        void onStartRecognition();

        void onRecognition(int index);

        void onStopRecognition();
    }

    public static interface Callback {
        BufferData getRecognitionBuffer();

        void freeRecognitionBuffer(BufferData buffer);
    }

    public FFTRecognition(Callback callback, int SampleRate, int channel, int bits) {
        mState = STATE_STOP;

        mCallback = callback;
        mSampleRate = SampleRate;
        mChannel = channel;
        mBits = bits;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        if (STATE_STOP == mState) {

            if (null != mCallback) {
                mState = STATE_START;
                mSamplingPointCount = 0;

                mIsStartCounting = false;
                //mStep = STEP1;
                mIsBeginning = false;
                mStartingDet = false;
                mStartingDetCount = 0;
                mPreRegCircle = -1;
                if (null != mListener) {
                    mListener.onStartRecognition();
                }
                while (STATE_START == mState) {
                    BufferData data = mCallback.getRecognitionBuffer();
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
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    int flag = 0;

    //处理的是16位采样数据
    private void process(BufferData data) {
        int size = data.getFilledSize(); //size = 4096 大约46ms 一个字符100ms

        //16位采样的处理
        short[] temp1 = new short[size / 2];
        for (int i = 0; i < size; i++) {
            short sh1 = data.mData[i];
            sh1 &= 0xff;
            short sh2 = data.mData[++i];
            sh2 <<= 8;
            temp1[((i - 1) / 2)] = (short) ((sh1) | (sh2));
        }
        double trueHz = handler(temp1);
//        System.out.println(trueHz);
        if(mRegIndex >= 0){
            if (null != mListener) {
                mListener.onRecognition(mRegIndex);
                System.out.println("HZ:" + trueHz);
                mRegIndex = -1;
            }//通知上层listener
        }else {

        }

    }


    private double handler(short[] temp1) {
        Complex result[] = FFT.fft(temp1);
        int size = temp1.length;
        double max = Double.MIN_VALUE;
        int maxIndex = -1;

        for (int i = 0; i < size / 2; i++) {
            if (result[i].abs() > max) {
                max = result[i].abs(); //86.1328125
                maxIndex = i;
            }
        }

        double trueHz = (double) maxIndex * 44100 / size;
        mRegIndex = maxIndex - 790;

        //System.out.println("HZ:" + trueHz);
        return trueHz;
    }

//    private void reg(int samplingPointCount) {
//        if (!mIsBeginning) {
//            if (!mStartingDet) {
//                if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
//                    mStartingDet = true;
//                    mStartingDetCount = 0;
//                }
//            } else {
//                if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
//                    ++mStartingDetCount;
//
//                    if (mStartingDetCount >= MIN_REG_CIRCLE_COUNT) {
//                        mIsBeginning = true;
//                        mIsRegStart = false;
//                        mRegCount = 0;
//                    }
//                } else {
//                    mStartingDet = false;
//                }
//            }
//        } else {
//            if (!mIsRegStart) {
//                if (samplingPointCount > 0) {
//                    mRegValue = samplingPointCount;
//                    mRegIndex = INDEX[samplingPointCount];
//                    mIsRegStart = true;
//                    mRegCount = 1;
//                }
//            } else {
//                if (samplingPointCount == mRegValue) {
//                    ++mRegCount;
//
//                    if (mRegCount >= MIN_REG_CIRCLE_COUNT) {
//                        // ok
//                        if (mRegValue != mPreRegCircle) {
//                            if (null != mListener) {
//                                mListener.onRecognition(mRegIndex);
//                            }//通知上层listener
//                            mPreRegCircle = mRegValue;
//                        }
//
//                        mIsRegStart = false;
//                    }
//                } else {
//                    mIsRegStart = false;
//                }
//            }
//        }
//    }
}

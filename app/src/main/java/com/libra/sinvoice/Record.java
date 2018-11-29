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

import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.libra.sinvoice.Buffer.BufferData;

public class Record {
    private final static String TAG = "Record";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    private Callback mCallback;
    private int mFrequence;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mBufferSize;
    private Listener mListener;

    private int mState;

    public Record(Callback callback, int frequence, int channelConfig, int audioFormat, int bufferSize) {
        mState = STATE_STOP;

        mCallback = callback;
        mFrequence = frequence;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
        mBufferSize = bufferSize;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        LogHelper.d(TAG, "start()");
        if (STATE_STOP == mState) {
            final int minBufferSize = AudioRecord.getMinBufferSize(mFrequence, mChannelConfig, mAudioFormat);
            LogHelper.d(TAG, "minBufferSize:" + minBufferSize);

            if (mBufferSize >= minBufferSize) {
                mState = STATE_START;

                final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, mFrequence, mChannelConfig, mAudioFormat, mBufferSize);
                try {
                    record.startRecording();

                    if (null != mCallback) {
                        if (null != mListener) {
                            mListener.onStartRecord();
                        }

                        while (STATE_START == mState) {
                            final BufferData data = mCallback.getRecordBuffer();
                            if (null != data) {
                                if (null != data.mData) {
                                    final int bufferReadResult = record.read(data.mData, 0, mBufferSize);
                                    data.setFilledSize(bufferReadResult);
                                    mCallback.freeRecordBuffer(data);
                                } else {
                                    LogHelper.d(TAG, "get end input data, so stop");
                                    break;
                                }
                            } else {
                                LogHelper.e(TAG, "get null data");
                                break;
                            }
                        }

                        if (null != mListener) {
                            mListener.onStopRecord();
                        }
                    }

                    record.stop();
                    record.release();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

                mState = STATE_STOP;
            } else {
                LogHelper.e(TAG, "bufferSize is too small");
            }
        }
    }

    public void stop() {
        LogHelper.d(TAG, "stop()");
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    public int getState() {
        LogHelper.d(TAG, "getState()");
        return mState;
    }

    public interface Listener {
        void onStartRecord();

        void onStopRecord();
    }

    public interface Callback {
        BufferData getRecordBuffer();

        void freeRecordBuffer(BufferData buffer);
    }
}
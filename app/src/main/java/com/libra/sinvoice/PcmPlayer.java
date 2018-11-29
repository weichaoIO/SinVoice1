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

import android.media.AudioManager;
import android.media.AudioTrack;

import com.libra.sinvoice.Buffer.BufferData;

public class PcmPlayer {
    private final static String TAG = "PcmPlayer";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    private int mState;

    private Callback mCallback;
    private AudioTrack mAudioTrack;
    private long mPlayedLen;
    private Listener mListener;

    public PcmPlayer(Callback callback, int sampleRate, int channel, int format, int bufferSize) {
        mState = STATE_STOP;

        mCallback = callback;
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channel, format, bufferSize, AudioTrack.MODE_STREAM);
        mPlayedLen = 0;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        LogHelper.d(TAG, "start()");
        if (STATE_STOP == mState && null != mAudioTrack) {
            mPlayedLen = 0;
            if (null != mCallback) {
                mState = STATE_START;

                if (null != mListener) {
                    mListener.onPlayStart();
                }

                while (STATE_START == mState) {
                    final BufferData data = mCallback.getPlayBuffer();
                    if (null != data) {
                        if (null != data.mData) {
                            int len = mAudioTrack.write(data.mData, 0, data.getFilledSize());
                            if (0 == mPlayedLen) {
                                mAudioTrack.play();
                            }
                            mPlayedLen += len;
                            mCallback.freePlayData(data);
                        } else {
                            LogHelper.d(TAG, "it is the end of input, so need stop");
                            break;
                        }
                    } else {
                        LogHelper.e(TAG, "get null data");
                        break;
                    }
                }

                if (null != mAudioTrack) {
                    mAudioTrack.pause();
                    mAudioTrack.flush();
                    mAudioTrack.stop();
                }

                if (null != mListener) {
                    mListener.onPlayStop();
                }

                mState = STATE_STOP;
            }
        }
    }

    public void stop() {
        LogHelper.d(TAG, "stop()");
        mState = STATE_STOP;
    }

    public interface Listener {
        void onPlayStart();

        void onPlayStop();
    }

    public interface Callback {
        BufferData getPlayBuffer();

        void freePlayData(BufferData data);
    }
}
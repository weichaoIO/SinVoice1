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

import android.os.Environment;

import java.io.File;

public class Common {
    // 编码
    public final static int DEFAULT_TOKEN_START = 2;  // 元音开始标志
    public final static int DEFAULT_TOKEN_STOP = 3;   // 元音结束标志
    public final static int DEFAULT_TOKEN_SAME_AS_BEFORE = 128;   // 元音与前一个相同 大于127

    public static final int DEFAULT_TOKEN_START_LENGTH = 1;
    public static final int DEFAULT_MESSAGE_LENGTH = 93;
    public static final int DEFAULT_TOKEN_END_LENGTH = 1;
    public static final int DEFAULT_HMAC_LENGTH = 16;
    public static final int DEFAULT_RS_LENGTH = 16;

    public final static String DEFAULT_CODE_BOOK = "!\"#$%&'()*+,-./" +
            "0123456789" +
            ":;<=>?@" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "[\\]^_`" +
            "abcdefghijklmnopqrstuvwxyz" +
            "{|}~";// 字符范围
    public static final int DEFAULT_GEN_DURATION = 100;// 每个元音的播放时间，单位ms
    public final static int BASE_INDEX = 790;
    public final static double BASE_FREQUENCY = 44100.0 / 2048 * BASE_INDEX;// 114325 16位=2字节=2048

    public static final int BITS_8 = 128;   // 2^7
    public static final int BITS_16 = 32768;// 2^15
    public static final int DEFAULT_BITS = BITS_16;

    public static final int SAMPLE_RATE_8000 = 8000;
    public static final int SAMPLE_RATE_11250 = 11250;
    public static final int SAMPLE_RATE_16000 = 16000;
    public static final int SAMPLE_RATE_44100 = 44100;
    public static final int DEFAULT_SAMPLE_RATE = SAMPLE_RATE_44100;

    public final static int DEFAULT_BUFFER_SIZE = 4096;
    public final static int DEFAULT_BUFFER_COUNT = 3;

    // 播放
    public static final boolean DEFAULT_REPEAT = false;// 是否重复播放
    public static final int DEFAULT_MUTE_INTERVAL = 0;// 相邻元音的播放时间，单位ms

    // log file
    public static final String LOG_PATH = Environment.getExternalStorageDirectory() + File.separator + "log.txt";
}
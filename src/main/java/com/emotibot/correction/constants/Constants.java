package com.emotibot.correction.constants;

public class Constants
{
    //CorrectionServiceImpl
    public static final String ORIGIN_FILE_PATH = "ORIGIN_FILE_PATH";
    public static final String TARGET_FILE_PATH = "TARGET_FILE_PATH";
    public static final String COMMON_SENTENCE_FILE_PATH = "COMMON_SENTENCE_FILE_PATH";
    public static final String COMMON_SENTENCE_TARGET_FILE_PATH = "COMMON_SENTENCE_TARGET_FILE_PATH";
    
    //param
    public static final int CHOOSE_NUM = 50;
    public static final double WORD_ERROR_1_RATE = 0.3;
    public static final double WORD_ERROR_2_RATE = 0.5;
    public static final double WORD_ERROR_3_RATE = 0.7;
    public static final float WORD_ERROR_4_RATE = 0.9f;
    public static final double PINYIN_RIGHT_RATE = 0.1;
    public static final double WORD_ERROR_COUNT_WITHOUT_ORDER = 0.4;
    
    public static final int WORD_SINGLE_MATCH_COUNT = 1;
    public static final int WORD_MATCH_COUNT = 1;
    public static final int PINYIN_SINGLE_MATCH_COUNT = 1;
    public static final int PINYIN_MATCH_COUNT = 1;
    public static final int COMMON_SINGLE_MATCH_COUNT = 10;
    public static final int COMMON_MATCH_COUNT = 10;
    
    public static final int COMMON_WORD_TIMES = 20;
    public static final int COMMON_PINYIN_TIMES = 20;
    
    //暂时不用
    public static final int WORD_HIGH_LEN = 2;
    public static final int WORD_LOW_LEN = 10;
}

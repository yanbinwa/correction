package com.emotibot.correction.utils;

import com.emotibot.correction.element.PinyinElement;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PinyinUtils
{
    public static final String PINYIN_SPLIT = "&";
    private static HanyuPinyinOutputFormat defaultFormat = null;
    
    static
    {
        defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
    }
    
    public static boolean comparePinyin(char word1, char word2)
    {
        if (String.valueOf(word1).matches("[\u4e00-\u9fa5]+")
                && String.valueOf(word2).matches("[\u4e00-\u9fa5]+"))
        {
            try
            {
                String pinyin1 = PinyinHelper.toHanyuPinyinStringArray(word1, defaultFormat)[0];
                String pinyin2 = PinyinHelper.toHanyuPinyinStringArray(word2, defaultFormat)[0];
                return pinyin1.equals(pinyin2);
            } 
            catch (BadHanyuPinyinOutputFormatCombination e)
            {
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            return false;
        }
    }
    
    public static boolean comparePinyin2(char word1, char word2)
    {
        if (String.valueOf(word1).matches("[\u4e00-\u9fa5]+")
                && String.valueOf(word2).matches("[\u4e00-\u9fa5]+"))
        {
            try
            {
                String pinyin1 = PinyinHelper.toHanyuPinyinStringArray(word1, defaultFormat)[0].trim();
                String pinyin2 = PinyinHelper.toHanyuPinyinStringArray(word2, defaultFormat)[0].trim();
                if (pinyin1.equals(pinyin2))
                {
                    return true;
                }
                else
                {
                    if (pinyin1.endsWith("g"))
                    {
                        pinyin1 = pinyin1.substring(0, pinyin1.length() - 1);
                    }
                    if (pinyin2.endsWith("g"))
                    {
                        pinyin2 = pinyin2.substring(0, pinyin2.length() - 1);
                    }
                    return pinyin1.equals(pinyin2);
                }
            } 
            catch (BadHanyuPinyinOutputFormatCombination e)
            {
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            return false;
        }
    }
    
    public static String getPinyin(String str)
    {
        if (str == null)
        {
            return null;
        }
        char[] arrays = str.trim().toCharArray();
        StringBuilder pinyin = new StringBuilder();
        boolean isFirst = true;
        try
        {
            for (int i = 0; i < arrays.length; i ++)
            {
                if (isFirst)
                {
                    isFirst = false;
                }
                else
                {
                    pinyin.append(PINYIN_SPLIT);
                }
                String str1 = String.valueOf(arrays[i]);
                if (str1.matches("[\u4e00-\u9fa5]+"))
                {
                    String[] array = PinyinHelper.toHanyuPinyinStringArray(arrays[i], defaultFormat);
                    if (array == null)
                    {
                        pinyin.append("null");
                    }
                    else
                    {
                        pinyin.append(array[0].trim());
                    }
                }
                else
                {
                    pinyin.append(str1);
                }
            }
            return pinyin.toString();
        }
        catch (BadHanyuPinyinOutputFormatCombination e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static boolean comparePinyin(String pinyin1, String pinyin2)
    {
        if (pinyin1 == null || pinyin2 == null)
        {
            return false;
        }
        pinyin1 = pinyin1.trim();
        pinyin2 = pinyin2.trim();
        
        if (pinyin1.length() < 2 || pinyin2.length() < 2)
        {
            return false;
        }
        if (pinyin1.equals(pinyin2))
        {
            return true;
        }
        else
        {
            if (pinyin1.endsWith("g"))
            {
                pinyin1 = pinyin1.substring(0, pinyin1.length() - 1);
            }
            if (pinyin2.endsWith("g"))
            {
                pinyin2 = pinyin2.substring(0, pinyin2.length() - 1);
            }
            if (pinyin1.equals(pinyin2))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
    
    public static PinyinElement append(PinyinElement... pinyinArray)
    {
        PinyinElement ret = new PinyinElement();
        for(PinyinElement element : pinyinArray)
        {
            ret.append(element);
        }
        return ret;
    }
}

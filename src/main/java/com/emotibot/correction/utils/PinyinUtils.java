package com.emotibot.correction.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.emotibot.correction.element.PinyinElement;
import com.emotibot.correction.element.SentenceElement;
import com.emotibot.middleware.utils.StringUtils;

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
    private static final String[] HANZI_NUM = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    
    private static final String[] SHENGMUS = {"b", "p", "m", "f", "d", "t", "n", "l", "g", "k", "h", "j", "g", "x", "zh", "ch", "sh", "z", "c", "s"};
    private static final String[] YUNMUS = {"a", "o", "i", "u", "ai", "ei", "ui", "ao", "ou", "u", "ie", "an", "en", "in", "un", "ang", "eng", "ing", "v"};
    
    private static Set<String> shengmuSet;
    private static Set<String> yunmuSet;
    
    private static List<String[]> confusedList;
    private static final String[] CONFUSE_1 = {"n", "l"};
    private static final String[] CONFUSE_2 = {"h", "f"};
    private static final String[] CONFUSE_3 = {"k", "g"};
    private static final String[] CONFUSE_4 = {"i", "in"};
    private static final String[] CONFUSE_5 = {"i", "v"};
    
    private static final String A_PINYIN = "ei";
    private static final String B_PINYIN = "bi";
    private static final String D_PINYIN = "di";
    private static final String E_PINYIN = "yi";
    private static final String G_PINYIN = "ji";
    private static final String I_PINYIN = "ai";
    private static final String N_PINYIN = "en";
    private static final String O_PINYIN = "ou";
    private static final String P_PINYIN = "pi";
    private static final String R_PINYIN = "er";
    private static final String T_PINYIN = "ti";
    private static final String U_PINYIN = "you";
    private static final String V_PINYIN = "wei";
    private static final String Y_PINYIN = "wai";
    
    private static Map<String, String> charToPinyinMap;
    
    static
    {
        defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        
        shengmuSet = new HashSet<String>();
        for(String str : SHENGMUS)
        {
            shengmuSet.add(str);
        }
        
        yunmuSet = new HashSet<String>();
        for(String str : YUNMUS)
        {
            yunmuSet.add(str);
        }
        
        confusedList = new ArrayList<String[]>();
        confusedList.add(CONFUSE_1);
        confusedList.add(CONFUSE_2);
        confusedList.add(CONFUSE_3);
        confusedList.add(CONFUSE_4);
        confusedList.add(CONFUSE_5);
        
        charToPinyinMap = new HashMap<String, String>();
        charToPinyinMap.put("a", A_PINYIN);
        charToPinyinMap.put("b", B_PINYIN);
        charToPinyinMap.put("d", D_PINYIN);
        charToPinyinMap.put("e", E_PINYIN);
        charToPinyinMap.put("g", G_PINYIN);
        charToPinyinMap.put("i", I_PINYIN);
        charToPinyinMap.put("n", N_PINYIN);
        charToPinyinMap.put("o", O_PINYIN);
        charToPinyinMap.put("p", P_PINYIN);
        charToPinyinMap.put("r", R_PINYIN);
        charToPinyinMap.put("t", T_PINYIN);
        charToPinyinMap.put("u", U_PINYIN);
        charToPinyinMap.put("v", V_PINYIN);
        charToPinyinMap.put("y", Y_PINYIN);
    }
    
    public static boolean comparePinyin(char word1, char word2)
    {
        String str1 = String.valueOf(word1);
        String str2 = String.valueOf(word2);
        String pinyin1 = getPinyin(str1);
        String pinyin2 = getPinyin(str2);
        return pinyin1.equals(pinyin2);
    }
    
    public static boolean comparePinyin2(char word1, char word2)
    {
        String str1 = String.valueOf(word1);
        String str2 = String.valueOf(word2);
        String pinyin1 = getPinyin(str1);
        String pinyin2 = getPinyin(str2);
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
    
    /**
     * 这里要支持数字转拼音
     * @param str
     * @return
     */
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
                else if(str1.matches("[\u0030-\u0039]"))
                {
                    int num = Integer.parseInt(str1);
                    String str1Tmp = HANZI_NUM[num];
                    String[] array = PinyinHelper.toHanyuPinyinStringArray(str1Tmp.toCharArray()[0], defaultFormat);
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
    
    //去掉平翘舌,去掉前后鼻音
    public static String getPinyin2(String str)
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
                        String pinyinRet = array[0].trim();
                        if (pinyinRet.startsWith("zh") || pinyinRet.startsWith("sh") || pinyinRet.startsWith("ch"))
                        {
                            pinyinRet = pinyinRet.substring(0, 1) + pinyinRet.substring(2);
                        }
                        if (pinyinRet.startsWith("g"))
                        {
                            pinyinRet = pinyinRet.substring(0, pinyinRet.length() - 1);
                        }
                        pinyin.append(pinyinRet);
                    }
                }
                else if(str1.matches("[\u0030-\u0039]"))
                {
                    int num = Integer.parseInt(str1);
                    String str1Tmp = HANZI_NUM[num];
                    String[] array = PinyinHelper.toHanyuPinyinStringArray(str1Tmp.toCharArray()[0], defaultFormat);
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
            pinyin1 = clearNasalsAndPingqiao(pinyin1);
            pinyin2 = clearNasalsAndPingqiao(pinyin2);
            if (pinyin1.equals(pinyin2))
            {
                return true;
            }
            else if (commonFuzzy(pinyin1, pinyin2))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
    
    /**
     * 去掉平翘舌和前后鼻音
     */
    public static String clearNasalsAndPingqiao(String pinyin)
    {
        //1. 去掉前后鼻音
        if (pinyin.endsWith("g"))
        {
            pinyin = pinyin.substring(0, pinyin.length() - 1);
        }
        //2. 去掉平翘舌音
        if (pinyin.length() >= 2)
        {
            if (String.valueOf(pinyin.charAt(1)).equals("h"))
            {
                pinyin = pinyin.substring(0, 1) + pinyin.substring(2, pinyin.length());
            }
        }
        return pinyin;
    }

    /**
     * 考虑进一步处理，例如l-n, h-f, i-in-ing
     * 
     * @param pinyin1
     * @param pinyin2
     * @return
     */
    private static boolean commonFuzzy(String pinyin1, String pinyin2)
    {
        String shengmu1 = getShengmu(pinyin1);
        String shengmu2 = getShengmu(pinyin2);
        String yunmu1 = getYunmu(pinyin1);
        String yunmu2 = getYunmu(pinyin2);
        
        //1. 韵母相同，声母confuse
        if (!StringUtils.isEmpty(shengmu1) && !StringUtils.isEmpty(shengmu2) && shengmu1.equals(shengmu2))
        {
            if (isConfused(yunmu1, yunmu2))
            {
                return true;
            }
        }
        
        //2. 声母相同，韵母confuse
        if (!StringUtils.isEmpty(yunmu1) && !StringUtils.isEmpty(yunmu2) && yunmu1.equals(yunmu2))
        {
            if (isConfused(shengmu1, shengmu2))
            {
                return true;
            }
        }

        return false;
    }
    
    /**
     * 如果声母相同或者韵母相同，也认为是正确的
     * 
     * @param pinyin1
     * @param pinyin2
     * @return
     */
    public static boolean comparePinyin2(String pinyin1, String pinyin2)
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
        pinyin1 = clearNasalsAndPingqiao(pinyin1);
        pinyin2 = clearNasalsAndPingqiao(pinyin2);
        String shengmu1 = getShengmu(pinyin1);
        String shengmu2 = getShengmu(pinyin2);
        String yunmu1 = getYunmu(pinyin1);
        String yunmu2 = getYunmu(pinyin2);
        if (!StringUtils.isEmpty(shengmu1) && !StringUtils.isEmpty(shengmu2) && shengmu1.equals(shengmu2))
        {
            return true;
        }
        if (!StringUtils.isEmpty(yunmu1) && !StringUtils.isEmpty(yunmu2) && yunmu1.equals(yunmu2))
        {
            return true;
        }
        return false;
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
    
    public static SentenceElement append(SentenceElement... sentenceArray)
    {
        SentenceElement ret = new SentenceElement();
        for(SentenceElement element : sentenceArray)
        {
            ret.append(element);
        }
        return ret;
    }
    
    public static String getPinyinForCharacter(String character)
    {
        if (character == null)
        {
            return null;
        }
        return charToPinyinMap.get(character.toLowerCase());
    }
    
    private static String getShengmu(String pinyin)
    {
        try
        {
            String first = pinyin.substring(0, 1);
            if (!shengmuSet.contains(first))
            {
                return null;
            }
            if (first.length() > 1 && (first.equals("s") || first.equals("c") || first.equals("z")))
            {
                if (String.valueOf(pinyin.charAt(1)).equals("h"))
                {
                    first += "h";
                }
            }
            return first;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    private static String getYunmu(String pinyin)
    {
        String first = getShengmu(pinyin);
        int start = 0;
        if (!StringUtils.isEmpty(first))
        {
            start = first.length();
        }
        return pinyin.substring(start);
    }
    
    private static boolean isConfused(String str1, String str2)
    {
        if (StringUtils.isEmpty(str1) || StringUtils.isEmpty(str2))
        {
            return false;
        }
        for (String[] confuses : confusedList)
        {
            if (isContance(confuses, str1) && isContance(confuses, str2))
            {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isContance(String[] array, String str)
    {
        for (int i = 0; i < array.length; i ++)
        {
            if (array[i].equals(str))
            {
                return true;
            }
        }
        return false;
    }
}

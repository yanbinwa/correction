package com.emotibot.correction.utils;

import com.emotibot.correction.constants.Constants;
import com.emotibot.correction.element.SentenceElement;

/**
 * 计算两个字符串的编辑距离，考虑音同字不同的情况
 * 
 * @author emotibot
 *
 */

public class EditDistanceUtils
{
    public static int getEditDistance(String str1, String str2)
    {
        int len1 = str1.length();
        int len2 = str2.length();
        int[][] dif = new int[len1 + 1][len2 + 1];
        for (int a = 0; a <= len1; a++) 
        {  
            dif[a][0] = a;  
        }
        for (int a = 0; a <= len2; a++) 
        {  
            dif[0][a] = a;  
        }
        int temp;  
        for (int i = 1; i <= len1; i++) {  
            for (int j = 1; j <= len2; j++) {  
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) 
                {  
                    temp = 0;  
                } 
                else 
                {  
                    temp = 1;  
                }  
                dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1, dif[i - 1][j] + 1);  
            }  
        } 
        return dif[len1][len2];
    }
    
    public static double getEditDistance(SentenceElement ele1, SentenceElement ele2)
    {
        String sentence1 = ele1.getSentence();
        String[] pinyinArray1 = ele1.getPinyin();
        String sentence2 = ele2.getSentence();
        String[] pinyinArray2 = ele2.getPinyin();
        int len1 = sentence1.length();
        int len2 = sentence2.length();
        int wordErrorCount1 = 0;
        int wordErrorCount2 = 0;
        int wordErrorCount3 = 0;
        int pinyinSameCount = 0;
        
        int[][] dif = new int[len1 + 1][len2 + 1];
        for (int a = 0; a <= len1; a++) 
        {  
            dif[a][0] = a;  
        }
        for (int a = 0; a <= len2; a++) 
        {  
            dif[0][a] = a;  
        }
        int temp;  
        for (int i = 1; i <= len1; i++) 
        {  
            for (int j = 1; j <= len2; j++) 
            {  
                if (sentence1.charAt(i - 1) == sentence2.charAt(j - 1)) 
                {  
                    temp = 0;
                } 
                else
                {
                    String pingyin1 = pinyinArray1[i - 1];
                    String pingyin2 = pinyinArray2[j - 1];
                    if (pingyin1.equals(pingyin2))
                    {
                        temp = 0;
                        wordErrorCount1 ++;
                    }
                    //TODO，这里要看是否最后使用到了，如果没有用到，反而会造成错误
                    else if (PinyinUtils.comparePinyin(pingyin1, pingyin2))
                    {
                        temp = 0;
                        wordErrorCount2 ++;
                    }
//                    else if (PinyinUtils.comparePinyin2(pingyin1, pingyin2))
//                    {
//                        temp = 0;
//                        wordErrorCount3 ++;
//                    }
                    else
                    {
                        temp = 1;
                    }
                }
                dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1, dif[i - 1][j] + 1);
            }  
        } 
        return dif[len1][len2] + wordErrorCount1 * Constants.WORD_ERROR_1_RATE 
                + wordErrorCount2 * Constants.WORD_ERROR_2_RATE 
                + wordErrorCount3 * Constants.WORD_ERROR_3_RATE
                - pinyinSameCount * Constants.PINYIN_RIGHT_RATE;
    }
    
    /**
     * 首先是比较是否有相同的字，如果有，就直接消除掉，之后比较是否有相同的音，如果有，也直接消除掉，之后判断两个element是否完全匹配出来了
     * 
     * @param ele1
     * @param ele2
     * @return
     */
    @SuppressWarnings("unused")
    public static int getEditDistanceWithoutOrder(SentenceElement ele1, SentenceElement ele2)
    {
        boolean[] tag1 = new boolean[ele1.getLength()];
        boolean[] tag2 = new boolean[ele2.getLength()];
        for (int i = 0; i < tag1.length; i ++)
        {
            tag1[i] = false;
        }
        for (int i = 0; i < tag2.length; i ++)
        {
            tag2[i] = false;
        }
        String sentence1 = ele1.getSentence();
        String[] pinyinArray1 = ele1.getPinyin();
        String sentence2 = ele2.getSentence();
        String[] pinyinArray2 = ele2.getPinyin();
        int wordErrorCount = 0;
        for(int i = 0; i < ele1.getLength(); i ++)
        {
            char ch1 = sentence1.charAt(i);
            String pingyin1 = pinyinArray1[i];
            boolean isMatch = false;
            for(int j = 0; j < ele2.getLength(); j ++)
            {
                if (tag2[j])
                {
                    continue;
                }
                char ch2 = sentence2.charAt(j);
                if (ch1 == ch2)
                {
                    tag1[i] = true;
                    tag2[j] = true;
                    isMatch = true;
                    break;
                }
            }
            if (isMatch)
            {
                continue;
            }
            for(int j = 0; j < ele2.getLength(); j ++)
            {
                if (tag2[j])
                {
                    continue;
                }
                String pingyin2 = pinyinArray2[j];
                if (PinyinUtils.comparePinyin(pingyin1, pingyin2))
                {
                    tag1[i] = true;
                    tag2[j] = true;
                    isMatch = true;
                }
//                else if (PinyinUtils.comparePinyin2(pingyin1, pingyin2))
//                {
//                    tag1[i] = true;
//                    tag2[j] = true;
//                    isMatch = true;
//                    wordErrorCount ++;
//                }
            }
        }
        int ret = 0;
        for (int i = 0; i < ele1.getLength(); i ++)
        {
            if (!tag1[i])
            {
                ret += 1;
            }
        }
        for (int i = 0; i < ele2.getLength(); i ++)
        {
            if (!tag2[i])
            {
                ret += 1;
            }
        }
        return ret + wordErrorCount;
    }
    
    /**
     * 当ele2中完全包含ele1的元素时，会得到的结果
     * 
     * 小理玩具 -> 查理的玩具小屋
     * 
     * 这里计算有哪些是连续匹配的
     * 
     * @param ele1
     * @param ele2
     * @return
     */
    @SuppressWarnings("unused")
    public static int getMatchParterWithoutOrder(SentenceElement ele1, SentenceElement ele2)
    {
        boolean[] tag1 = new boolean[ele1.getLength()];
        boolean[] tag2 = new boolean[ele2.getLength()];
        int[] matchIndex = new int[ele1.getLength()];
        for (int i = 0; i < tag1.length; i ++)
        {
            tag1[i] = false;
        }
        for (int i = 0; i < tag2.length; i ++)
        {
            tag2[i] = false;
        }
        String sentence1 = ele1.getSentence();
        String[] pinyinArray1 = ele1.getPinyin();
        String sentence2 = ele2.getSentence();
        String[] pinyinArray2 = ele2.getPinyin();
        int wordErrorCount = 0;
        for(int i = 0; i < ele1.getLength(); i ++)
        {
            char ch1 = sentence1.charAt(i);
            String pingyin1 = pinyinArray1[i];
            boolean isMatch = false;
            for(int j = 0; j < ele2.getLength(); j ++)
            {
                if (tag2[j])
                {
                    continue;
                }
                char ch2 = sentence2.charAt(j);
                if (ch1 == ch2)
                {
                    tag1[i] = true;
                    tag2[j] = true;
                    isMatch = true;
                    matchIndex[i] = j;
                    break;
                }
            }
            if (isMatch)
            {
                continue;
            }
            for(int j = 0; j < ele2.getLength(); j ++)
            {
                if (tag2[j])
                {
                    continue;
                }
                String pingyin2 = pinyinArray2[j];
                if (PinyinUtils.comparePinyin(pingyin1, pingyin2))
                {
                    tag1[i] = true;
                    tag2[j] = true;
                    isMatch = true;
                    matchIndex[i] = j;
                    break;
                }
            }
        }
        int parter = 1;
        int lastIndex = matchIndex[0];
        for (int i = 1; i < matchIndex.length; i ++)
        {
            if (matchIndex[i] != lastIndex + 1)
            {
                parter ++;
            }
            lastIndex = matchIndex[i];
        }
        return parter;
    }
    
    private static int min(int a, int b, int c)
    {
        return a < b ? Math.min(a, c) : Math.min(b, c);
    }
}

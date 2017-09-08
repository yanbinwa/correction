package com.emotibot.correction.utils;

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
        int wordErrorCount = 0;
        
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
                    }
                    else if (PinyinUtils.comparePinyin(pingyin1, pingyin2))
                    {
                        temp = 0;
                        wordErrorCount ++;
                    }
                    else
                    {
                        temp = 1;
                    }
                }
                dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1, dif[i - 1][j] + 1);
            }  
        } 
        return dif[len1][len2] + wordErrorCount * 0.1;
    }
    
    private static int min(int a, int b, int c)
    {
        return a < b ? Math.min(a, c) : Math.min(b, c);
    }
}

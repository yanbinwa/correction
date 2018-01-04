package com.emotibot.correction.utils;

import org.junit.Test;

public class PinyinUtilsTest
{

    @Test
    public void test()
    {        
        String str1 = "lang";
        String str2 = "tan";
        System.out.println(PinyinUtils.comparePinyin(str1, str2));
        
        float f = 2.0f;
        if (f == 2)
        {
            System.out.println("true");
        }
    }

}

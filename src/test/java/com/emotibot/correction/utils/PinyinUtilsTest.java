package com.emotibot.correction.utils;

import org.junit.Test;

public class PinyinUtilsTest
{

    @Test
    public void test()
    {
        String str = "3生3世";
        String ele = PinyinUtils.getPinyin(str);
        System.out.println(ele);
        
        String str1 = "3";
        String str2 = "三";
        System.out.println(PinyinUtils.comparePinyin(str1.toCharArray()[0], str2.toCharArray()[0]));
    }

}

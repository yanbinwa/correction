package com.emotibot.correction.utils;

import org.junit.Test;

import com.emotibot.correction.element.SentenceElement;

public class EditDistanceUtilsTest
{

    public static final String TARGET = "女";
    public static final String INPUT = "率";
    
    @Test
    public void test()
    {
        SentenceElement ele1 = new SentenceElement(TARGET);
        SentenceElement ele2 = new SentenceElement(INPUT);
        System.out.print(EditDistanceUtils.getEditDistance(ele1, ele2));
    }

}

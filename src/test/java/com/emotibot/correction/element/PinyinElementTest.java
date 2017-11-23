package com.emotibot.correction.element;

import org.junit.Test;

public class PinyinElementTest
{

    public static String str1 = "我是王彦彬";
    public static String str2 = "我";
    public static String str3 = "柜";
    
    @Test
    public void test()
    {
        test2();
    }
    
    @SuppressWarnings("unused")
    private void test1()
    {
        SentenceElement element1 = new SentenceElement(str1);
        SentenceElement element2 = new SentenceElement(str3);
        System.out.println(element1);
        System.out.println(element2);
        
        //remove
        System.out.println(element1.remove(element2));
        
        //remove pinyin
        System.out.println(element1.remove(element2.getPinyinEle()));
        
        //sub
        System.out.println(element1.getPinyinEle().subPinyinElement(2, 3));
        
        System.out.println(element1.subSentenceElement(2, 3));
        
        //contains
        System.out.println(element1.contains(element2));
        
        System.out.println(element1.contains(element2.getPinyinEle()));
        
        //append
        System.out.println(element1.getPinyinEle().append(element2.getPinyinEle()));
        
        System.out.println(element1.append(element2));
    }
    
    @SuppressWarnings("unused")
    private void test2()
    {
        SentenceElement element1 = new SentenceElement("时空探长");
        SentenceElement element2 = new SentenceElement("时空弹唱");
        SentenceElement element3 = new SentenceElement("时空罪恶");
        
        System.out.println(element1);
        System.out.println(element2);
        System.out.println(element3);
        
    }

}

package com.emotibot.correction.element;

import com.emotibot.correction.utils.PinyinUtils;

public class SentenceElement
{
    private String sentence = null;
    private String[] pinyin = null;
    
    public SentenceElement()
    {
        
    }
    
    public SentenceElement(String sentence, String[] pinyin)
    {
        this.sentence = sentence;
        this.pinyin = pinyin;
    }
    
    public SentenceElement(String sentence)
    {
        this.sentence = sentence;
        String pinyinStr = PinyinUtils.getPinyin(sentence);
        pinyin = pinyinStr.trim().split("&");
    }
    
    public String getSentence()
    {
        return this.sentence;
    }
    
    public void setSentence(String sentence)
    {
        this.sentence = sentence;
    }
    
    public String[] getPinyin()
    {
        return this.pinyin;
    }
    
    public void setPinyin(String[] pinyin)
    {
        this.pinyin = pinyin;
    }
    
    @Override
    public String toString()
    {
        return this.sentence;
    }
}

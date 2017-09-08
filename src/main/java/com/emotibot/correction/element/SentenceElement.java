package com.emotibot.correction.element;

import com.emotibot.correction.utils.PinyinUtils;

public class SentenceElement
{
    private String sentence = null;
    private String[] pinyin = null;
    private PinyinElement element = new PinyinElement();
    
    public SentenceElement()
    {
        
    }
    
    public SentenceElement(SentenceElement other)
    {
        this.sentence = other.sentence;
        this.pinyin = other.pinyin;
        this.element = other.element;
    }
    
    public SentenceElement(String sentence)
    {
        this.sentence = sentence;
        String pinyinStr = PinyinUtils.getPinyin(sentence).trim();
        element.setPinyin(pinyinStr);
        pinyin = pinyinStr.split(PinyinUtils.PINYIN_SPLIT);
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
    
    public PinyinElement getPinyinStr()
    {
        return this.element;
    }
    
    public void setPinyinStr(PinyinElement element)
    {
        this.element = element;
    }
    
    public boolean contains(PinyinElement element)
    {
        if (element == null)
        {
            return false;
        }
        return this.element.contains(element);
    }
    
    @Override
    public String toString()
    {
        return this.sentence + ":" + this.element.getPinyin();
    }
    
    @Override
    public int hashCode()
    {
        String str = "" + this.sentence + this.element.getPinyin();
        return str.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof SentenceElement))
        {
            return false;
        }
        SentenceElement other = (SentenceElement) obj;
        
        if (this.sentence == null || this.element == null || 
                other.sentence == null || other.element == null)
        {
            return false;
        }
        
        if (!this.sentence.equals(other.sentence))
        {
            return false;
        }
        else if (!this.element.equals(other.element))
        {
            return false;
        }
        else
        {
            return true;
        }
    }
}

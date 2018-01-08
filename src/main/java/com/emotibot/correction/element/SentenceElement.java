package com.emotibot.correction.element;

import java.util.ArrayList;
import java.util.List;

import com.emotibot.correction.utils.PinyinUtils;

/**
 * 经测试，汉字到拼音的转换比较，对应查询语句和片库尽量只进行一次转换，满足append，
 * remove, sub等操作。
 * 
 * @author emotibot
 *
 */

public class SentenceElement
{
    private String sentence = "";
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
        String pinyinStr = "";
        for(String tmp : pinyin)
        {
            pinyinStr += tmp + PinyinUtils.PINYIN_SPLIT;
        }
        if (pinyinStr.endsWith(PinyinUtils.PINYIN_SPLIT))
        {
            pinyinStr = pinyinStr.substring(0, pinyinStr.length() - 1);
        }
        this.element = new PinyinElement();
        this.element.setPinyin(pinyinStr);
    }
    
    public PinyinElement getPinyinEle()
    {
        return this.element;
    }
    
    public void setPinyinEle(PinyinElement element)
    {
        this.element = element;
        this.pinyin = element.getPinyin().split(PinyinUtils.PINYIN_SPLIT);
    }
    
    /**
     * 考虑拼音
     * @param element
     * @return
     */
    public boolean contains(PinyinElement element)
    {
        if (element == null)
        {
            return false;
        }
        return this.element.contains(element);
    }
    
    /**
     * 考虑汉字
     * @param element
     * @return
     */
    public boolean contains(SentenceElement element)
    {
        if (element == null)
        {
            return false;
        }
        return this.sentence.contains(element.getSentence());
    }
    
    public int getLength()
    {
        return this.sentence.length();
    }
    
    public SentenceElement append(SentenceElement other)
    {
        this.sentence += other.sentence;
        this.element.append(other.getPinyinEle());
        this.pinyin = element.getPinyin().split(PinyinUtils.PINYIN_SPLIT);
        return this;
    }
    
    public SentenceElement subSentenceElement(int start, int end)
    {
        SentenceElement element = new SentenceElement();
        if (start >= end)
        {
            return element;
        }
        if (end > this.getLength())
        {
            return null;
        }
        String sentence1 = this.sentence.substring(start, end);
        PinyinElement pinyinEle1 = this.element.subPinyinElement(start, end);
        element.setSentence(sentence1);
        element.setPinyinEle(pinyinEle1);
        return element;
    }
    
    public List<SentenceElement> getSingleSentenceElement()
    {
        List<SentenceElement> singleSentenceElements = new ArrayList<SentenceElement>();
        for(int i = 0; i < this.getLength(); i ++)
        {
            singleSentenceElements.add(subSentenceElement(i, i + 1));
        }
        return singleSentenceElements;
    }
    
    /**
     * 考虑拼音
     * @param other
     * @return
     */
    public SentenceElement remove(PinyinElement other)
    {
        if (this.isEmpty())
        {
            return this;
        }
        if (!this.getPinyinEle().contains(other))
        {
            return this;
        }
        int startIndex = this.element.getPinyin().indexOf(other.getPinyin());
        if (startIndex < 0)
        {
            return this;
        }
        
        String str1 = this.element.getPinyin().substring(0, startIndex);
        int startTimes = 0;
        for (int i = 0; i < str1.length(); i ++)
        {
            if (String.valueOf(str1.toCharArray()[i]).equals(PinyinUtils.PINYIN_SPLIT))
            {
                startTimes ++;
            }
        }
        
        int endTimes = startTimes + other.getLength();
        return this.subSentenceElement(0, startTimes).append(this.subSentenceElement(endTimes, this.getLength()));
    }
    
    /**
     * 考虑汉字
     */
    public SentenceElement remove(SentenceElement other)
    {
        if (this.isEmpty())
        {
            return this;
        }
        if (!this.contains(other))
        {
            return this;
        }
        int startIndex = this.sentence.indexOf(other.sentence);
        int endIndex = other.sentence.length() + startIndex;
        return this.subSentenceElement(0, startIndex).append(this.subSentenceElement(endIndex, this.getLength()));
    }
    
    public boolean isEmpty()
    {
        if (sentence == null || sentence.trim().equals(""))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    @Override
    public String toString()
    {
        return this.sentence + ":" + this.element.getPinyin();
    }
    
    @Override
    public int hashCode()
    {
        String str = this.sentence;
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
        
        if (this.sentence.equals(other.sentence))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}

package com.emotibot.correction.element;

import com.emotibot.correction.utils.PinyinUtils;

public class PinyinElement
{
    private String pinyin = "";
    
    public PinyinElement()
    {
        
    }
    
    public PinyinElement(String sentence)
    {
        this.pinyin = PinyinUtils.getPinyin(sentence);
    }
    
    public PinyinElement(PinyinElement other)
    {
        this.pinyin = other.pinyin;
    }
    
    public void setPinyin(String pinyin)
    {
        this.pinyin = pinyin;
    }
    
    public String getPinyin()
    {
        return this.pinyin;
    }
    
    public PinyinElement append(PinyinElement other)
    {
        if (this.isEmpty())
        {
            this.pinyin = other.pinyin;
        }
        else
        {
            this.pinyin = this.pinyin + PinyinUtils.PINYIN_SPLIT + other.pinyin;
        }
        return this;
    }
    
    public int getLength()
    {
        if (isEmpty())
        {
            return 0;
        }
        else
        {
            String[] tmp = this.pinyin.split(PinyinUtils.PINYIN_SPLIT);
            return tmp.length;
        }
    }
    
    //这里要保证拼音的完整性
    public boolean contains(PinyinElement other)
    {
        if (other == null)
        {
            return false;
        }
        String tmp = PinyinUtils.PINYIN_SPLIT + this.pinyin + PinyinUtils.PINYIN_SPLIT;
        String tmp1 = PinyinUtils.PINYIN_SPLIT + other.pinyin + PinyinUtils.PINYIN_SPLIT;
        return tmp.contains(tmp1);
    }
    
    public boolean isEmpty()
    {
        if (pinyin == null || pinyin.trim().equals(""))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public PinyinElement remove(PinyinElement other)
    {
        PinyinElement ret = new PinyinElement();
        if (other == null || !this.contains(other))
        {
            ret.setPinyin(this.pinyin);
            return ret;
        }
        else if (this.pinyin.equals(other.pinyin))
        {
            return ret;
        }
        
        if (this.pinyin.endsWith(other.pinyin))
        {
            String tmp = this.pinyin.replace(PinyinUtils.PINYIN_SPLIT + other.pinyin, "");
            ret.setPinyin(tmp);
        }
        else
        {
            String tmp = this.pinyin.replace(other.pinyin + PinyinUtils.PINYIN_SPLIT, "");
            ret.setPinyin(tmp);
        }
        return ret;
    }
    
    @Override
    public String toString()
    {
        return this.pinyin;
    }
    
    @Override
    public int hashCode()
    {
        return pinyin.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof PinyinElement))
        {
            return false;
        }
        PinyinElement other = (PinyinElement) obj;
        
        if (this.pinyin.equals(other.pinyin))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}

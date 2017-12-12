package com.emotibot.correction.service;

import java.util.List;

public interface CorrectionService
{
    public List<String> correct(String targetStr);
    
    public List<String> correctWithPinyin(String targetStr);
    
    public String getLikelyNameEntity(String nameEntity);
}

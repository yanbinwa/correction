package com.emotibot.correction.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.emotibot.correction.constants.Constants;
import com.emotibot.correction.element.PinyinElement;
import com.emotibot.correction.element.SentenceElement;
import com.emotibot.correction.utils.EditDistanceUtils;
import com.emotibot.correction.utils.PinyinUtils;
import com.emotibot.correction.utils.SegementUtils;
import com.emotibot.middleware.conf.ConfigManager;
import com.emotibot.middleware.utils.StringUtils;

public class CorrectionServiceImpl implements CorrectionService
{
    //TODO 
    private int selectNum = 5;
    private int hasError = 0;
    private long totalTokensCount = 0L;
    @SuppressWarnings("unused")
    private long totalCommonTokensCount = 0L;
    
    private Map<String, Integer> wordCountMap = new HashMap<String, Integer>();
    private Map<String, Integer> commonWordCountMap = new HashMap<String, Integer>();
    private Map<Integer, List<SentenceElement>> sentenceLengthToSentenceMap = 
                                                new HashMap<Integer, List<SentenceElement>>();
    private String originalFilePath = null;
    private String targetFilePath = null;
    private String commonSentenceFilePath = null;
    private String commonSentenceTargetFilePath = null;
    
    //pinyin
    private long totalTokensCount2 = 0L;
    private Map<PinyinElement, Integer> pinyinCountMap = new HashMap<PinyinElement, Integer>();
    
    private ReentrantLock lock = new ReentrantLock();
    
    public CorrectionServiceImpl()
    {
        originalFilePath = ConfigManager.INSTANCE.getPropertyString(Constants.ORIGIN_FILE_PATH);
        targetFilePath = ConfigManager.INSTANCE.getPropertyString(Constants.TARGET_FILE_PATH);
        commonSentenceFilePath = ConfigManager.INSTANCE.getPropertyString(Constants.COMMON_SENTENCE_FILE_PATH);
        commonSentenceTargetFilePath = ConfigManager.INSTANCE.getPropertyString(Constants.COMMON_SENTENCE_TARGET_FILE_PATH);
        try
        {
            if (commonSentenceFilePath != null && commonSentenceTargetFilePath != null)
            {
                SegementUtils.segementFile(originalFilePath, targetFilePath, commonSentenceFilePath, commonSentenceTargetFilePath);
            }
            else
            {
                SegementUtils.segementFile(originalFilePath, targetFilePath);
            }
        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }
        initWordCountMap();
        initCommonWordCountMap();
        initPinyinCountMap();
        initSentenceMap();
    }
    
    @Override
    public List<String> correct(String targetStr)
    {
        return suggest(targetStr);
    }
    
    private void initWordCountMap()
    {
        lock.lock();
        BufferedReader br = null;
        try
        {
            Map<String, Integer> wordCountMapTmp = new HashMap<String, Integer>();
            File targetFile = new File(targetFilePath);
            br = new BufferedReader(new FileReader(targetFile));
            
            String wordsline = null;
            while ((wordsline = br.readLine()) != null)
            {
                String[] words = wordsline.trim().split(" ");
                for (int i = 0; i < words.length; i++)
                {
                    int wordCount = wordCountMapTmp.get(words[i]) == null ? 0 : wordCountMapTmp.get(words[i]);
                    wordCountMapTmp.put(words[i], wordCount + 1);
                    totalTokensCount += 1;
                    if (words.length > 1 && i < words.length - 1)
                    {
                        StringBuffer wordStrBuf = new StringBuffer();
                        wordStrBuf.append(words[i]).append(words[i + 1]);
                        int wordStrCount = wordCountMapTmp.get(wordStrBuf.toString()) == null 
                                                             ? 0 : wordCountMapTmp.get(wordStrBuf.toString());
                        wordCountMapTmp.put(wordStrBuf.toString(), wordStrCount+1);
                        totalTokensCount += 1;
                    }
                }
            }
            wordCountMap = wordCountMapTmp;
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        finally
        {
            lock.unlock();
            if (br != null)
            {
                try
                {
                    br.close();
                } 
                catch (IOException e)
                {
                    //Do nothing
                }
            }
        }
    }
    
    private void initCommonWordCountMap()
    {
        if (StringUtils.isEmpty(commonSentenceTargetFilePath))
        {
            return;
        }
        lock.lock();
        BufferedReader br = null;
        try
        {
            Map<String, Integer> commonWordCountMapTmp = new HashMap<String, Integer>();
            File targetFile = new File(commonSentenceTargetFilePath);
            br = new BufferedReader(new FileReader(targetFile));
            
            String wordsline = null;
            while ((wordsline = br.readLine()) != null)
            {
                String[] words = wordsline.trim().split(" ");
                for (int i = 0; i < words.length; i++)
                {
                    int wordCount = commonWordCountMapTmp.get(words[i]) == null ? 0 : commonWordCountMapTmp.get(words[i]);
                    commonWordCountMapTmp.put(words[i], wordCount + 1);
                    totalCommonTokensCount += 1;
                    
                    String word = words[i];
                    for (int j = 0; j < word.length(); j ++)
                    {
                        String chat = String.valueOf(word.charAt(j));
                        int wordCount1 = commonWordCountMapTmp.get(chat) == null ? 0 : commonWordCountMapTmp.get(chat);
                        commonWordCountMapTmp.put(chat, wordCount1 + 1);
                        totalCommonTokensCount += 1;
                    }
                    
                    if (words.length > 1 && i < words.length - 1)
                    {
                        StringBuffer wordStrBuf = new StringBuffer();
                        wordStrBuf.append(words[i]).append(words[i + 1]);
                        int wordStrCount = commonWordCountMapTmp.get(wordStrBuf.toString()) == null 
                                                             ? 0 : commonWordCountMapTmp.get(wordStrBuf.toString());
                        commonWordCountMapTmp.put(wordStrBuf.toString(), wordStrCount + 1);
                        totalCommonTokensCount += 1;
                    }
                }
            }
            commonWordCountMap = commonWordCountMapTmp;
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        finally
        {
            lock.unlock();
            if (br != null)
            {
                try
                {
                    br.close();
                } 
                catch (IOException e)
                {
                    //Do nothing
                }
            }
        }
    }

    private void initSentenceMap()
    {
        lock.lock();
        BufferedReader br = null;
        try
        {
            Map<Integer, List<SentenceElement>> sentenceLengthToSentenceMapTmp = 
                                                        new HashMap<Integer, List<SentenceElement>>();
            File originalFile = new File(originalFilePath);
            br = new BufferedReader(new FileReader(originalFile));
            
            String sentence = null;
            while ((sentence = br.readLine()) != null)
            {
                int length = sentence.length();
                List<SentenceElement> sentenceElementList = sentenceLengthToSentenceMapTmp.get(length);
                if (sentenceElementList == null)
                {
                    sentenceElementList = new ArrayList<SentenceElement>();
                    sentenceLengthToSentenceMapTmp.put(length, sentenceElementList);
                }
                SentenceElement element = new SentenceElement(sentence.trim());
                sentenceElementList.add(element);
            }
            sentenceLengthToSentenceMap = sentenceLengthToSentenceMapTmp;
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        finally
        {
            lock.unlock();
            if (br != null)
            {
                try
                {
                    br.close();
                } 
                catch (IOException e)
                {
                    //Do nothing
                }
            }
        }        
    }
    
    private List<String> suggest(String sInput)
    {
        List<SentenceElement> correctedList = new ArrayList<SentenceElement>();
        List<SentenceElement> crtTempList = new ArrayList<SentenceElement>();

        char[] str2char = sInput.toCharArray();
        String[] sInputResult = new String[str2char.length];
        for (int t = 0; t < str2char.length; t++)
        {
            sInputResult[t] = String.valueOf(str2char[t]);
        }
        String[] MaxAndSecondMaxSequnce = getMaxAndSecondMaxSequnce(sInputResult);
        System.out.println(Arrays.toString(MaxAndSecondMaxSequnce));
        if (MaxAndSecondMaxSequnce == null)
        {
            return new ArrayList<String>();
        }
        if (hasError != 0)
        {
            if (MaxAndSecondMaxSequnce.length > 1)
            {
                String maxSequence = MaxAndSecondMaxSequnce[0];
                String maxSequence2 = MaxAndSecondMaxSequnce[1];
                //这里遍历sentence，可以做成并行处理，提高效率
                List<SentenceElement> candidateSentenceList = getCandidateSentenceList(sInput);
                
                for (int j = 0; j < candidateSentenceList.size(); j++)
                {
                    String sentence = candidateSentenceList.get(j).getSentence();
                    if (maxSequence2.equals(""))
                    {
                        if (sentence.contains(maxSequence)) 
                        {
                            correctedList.add(candidateSentenceList.get(j));
                        }
                    }
                    else 
                    {
                        if (sentence.contains(maxSequence) && sentence.contains(maxSequence2))
                        {
                            crtTempList.add(candidateSentenceList.get(j));
                        }
                        else if (sentence.contains(maxSequence)) 
                        {
                            correctedList.add(candidateSentenceList.get(j));
                        }
                        else if (sentence.contains(maxSequence2))
                        {
                            correctedList.add(candidateSentenceList.get(j));
                        }
                    }
                }
                
                if (crtTempList.size() > 0)
                {
                    correctedList.clear();
                    correctedList.addAll(crtTempList);
                }
                SentenceElement element = new SentenceElement(sInput);
                correctedList = sortList(element, correctedList);
            }
        }
        else 
        {
            SentenceElement element = new SentenceElement(sInput);
            correctedList.add(element);
        }
        
        return getSentenceList(correctedList);
    }
    
    /**
     * 这里应该是找到前两个可以匹配到最长的token，同时要选择出现频率较低的，这样可以包含更对的特征信息
     * 同时还要对于其子字符串中提取出出现频率最大的字串，将其从字符串中删除
     * 
     * @param sInputResult
     * @return
     */
    private String[] getMaxAndSecondMaxSequnce(String[] sInputResult)
    {
        //这句话中每个单词开始能够找到的最长的String，并且该String是在tokenMap中可以找的，把这个写入到correctTokens中
        List<String> correctTokens = getCorrectTokens(sInputResult);
        String[] maxAndSecondMaxSeq = new String[2];
        if (correctTokens.size() == 0) 
        {
            return null;
        }
        else if (correctTokens.size() == 1)
        {
            maxAndSecondMaxSeq[0]=correctTokens.get(0);
            maxAndSecondMaxSeq[1]=correctTokens.get(0);
            return maxAndSecondMaxSeq;
        }
        
        String maxSequence = correctTokens.get(0);
        String maxSequence2 = correctTokens.get(correctTokens.size() - 1);
        String littleword = "";
        for (int i = 1; i < correctTokens.size(); i++)
        {
            //优先匹配最长的，所以最长的应该是在maxSequence中的
            if (correctTokens.get(i).length() > maxSequence.length())
            {
                maxSequence = correctTokens.get(i);
            } 
            //如果长度一致，就比较出现的频率
            else if (correctTokens.get(i).length() == maxSequence.length())
            {
                //单个单词
                if (correctTokens.get(i).length() == 1)
                {
                    if (probBetweenTowTokens(correctTokens.get(i)) > probBetweenTowTokens(maxSequence)) 
                    {
                        //为什么是maxSequence2？
                        maxSequence2 = correctTokens.get(i);
                    }
                }
                //select words with smaller probability for multi-word, because the smaller has more self information
                else if (correctTokens.get(i).length() > 1)
                {
                    if (probBetweenTowTokens(correctTokens.get(i)) <= probBetweenTowTokens(maxSequence)) 
                    {
                        //这里为什么选择频率低的呢？
                        maxSequence2 = correctTokens.get(i);
                    }
                }
            } 
            else if (correctTokens.get(i).length() > maxSequence2.length())
            {
                maxSequence2 = correctTokens.get(i);
            } 
            else if (correctTokens.get(i).length() == maxSequence2.length())
            {
                if (probBetweenTowTokens(correctTokens.get(i)) > probBetweenTowTokens(maxSequence2))
                {
                    maxSequence2 = correctTokens.get(i);
                }
            }
        }
        //delete the sub-word from a string
        if (maxSequence2.length() == maxSequence.length())
        {
            int maxseqvaluableTokens = maxSequence.length();
            int maxseq2valuableTokens = maxSequence2.length();
            float min_truncate_prob_a = 0 ;
            float min_truncate_prob_b = 0;
            String aword = "";
            String bword = "";
            //这里在选择出maxSequence和maxSequence2后，会查看其subString出现的最大频次，出现频率越多，说明其带有的特征信息越少，所以要把这部分除去
            for (int i = 0; i < correctTokens.size(); i++)
            {
                float tokenprob = probBetweenTowTokens(correctTokens.get(i));
                if ((!maxSequence.equals(correctTokens.get(i))) && maxSequence.contains(correctTokens.get(i)))
                {
                    if (tokenprob >= min_truncate_prob_a)
                    {
                        min_truncate_prob_a = tokenprob ;
                        aword = correctTokens.get(i);
                    }
                }
                else if ((!maxSequence2.equals(correctTokens.get(i))) && maxSequence2.contains(correctTokens.get(i)))
                {
                    if (tokenprob >= min_truncate_prob_b)
                    {
                        min_truncate_prob_b = tokenprob;
                        bword = correctTokens.get(i);
                    }
                }
            }
            //System.out.println(min_truncate_prob_a + " VS " + min_truncate_prob_b);
            //maxSequence的subString频次较小，说明maxSequence2的substring肯定也有了，就会对token的权重进行修改
            //这里是什么情况？？
            if (aword.length() > 0 && min_truncate_prob_a < min_truncate_prob_b)
            {
                //对长度进行修改
                maxseqvaluableTokens -= 1 ;
                littleword = maxSequence.replace(aword, "");
            }
            else 
            {
                maxseq2valuableTokens -= 1 ;
                String temp = maxSequence2;
                //如果maxSequence也包含maxSequence2除去共性较多的信息后，那么littleword就是maxSequence2
                if (maxSequence.contains(temp.replace(bword, "")))
                {
                    littleword =  maxSequence2;
                }
                else 
                {
                    littleword =  maxSequence2.replace(bword, "");
                }
            }
            
            if (maxseqvaluableTokens < maxseq2valuableTokens)
            {
                maxSequence = maxSequence2;
                maxSequence2 = littleword;
            }
            else 
            {
                maxSequence2 = littleword;
            }
            
        }
        maxAndSecondMaxSeq[0] = maxSequence;
        maxAndSecondMaxSeq[1] = maxSequence2;
        return maxAndSecondMaxSeq;
    }
    
    private List<String> getCorrectTokens(String[] sInputResult)
    {
        List<String> correctTokens = new ArrayList<String>();
        float probOne = 0;
        List<Integer> isCorrect = new ArrayList<Integer>();
        for (int i = 0; i < sInputResult.length; i++)
        {
            //这里获取到该单个字对应的token数量占总token的比例
            probOne = probBetweenTowTokens(sInputResult[i]);
            if (probOne <= 0)
            {
                isCorrect.add(i, 0);
            } 
            else 
            {
                isCorrect.add(i, 1);
            }
        }
     
        //含有两个字符以上的单词
        if (sInputResult.length > 2)
        {
            //这里所有的单个的字都匹配上了
            if (!isCorrect.contains(0))
            {
                //这里是将该sInputResult中所有连在一起的组合的可能性，都查找一遍
                for (int i = 0; i < sInputResult.length - 1; i++)
                {
                    StringBuffer tokenbuf = new StringBuffer();
                    tokenbuf.append(sInputResult[i]);
                    for(int j = i + 1; j < sInputResult.length; j++)
                    {
                        float b = probBetweenTowTokens(tokenbuf.toString() + sInputResult[j]);
                        //这里应该是保证最大匹配吧
                        if (b > 0)
                        {
                            tokenbuf.append(sInputResult[j]);
                        }
                        else
                        {
                            hasError = 1;
                            if (j < sInputResult.length-1 && 
                                    probBetweenTowTokens(tokenbuf.toString() + sInputResult[j] + sInputResult[j + 1]) > 0)
                            {
                                tokenbuf.append(sInputResult[j] + sInputResult[j + 1]);
                            }
                            else
                            {
                                break;
                            }
                        }                       
                    }
                    correctTokens.add(tokenbuf.toString());
                }
                
                if (probBetweenTowTokens(sInputResult[sInputResult.length - 1]) > 0)
                {
                    correctTokens.add(sInputResult[sInputResult.length - 1]);
                }
            }
            else 
            {
                for (int i = 0; i < sInputResult.length - 1; i++)
                {
                    StringBuffer tokenbuf = new StringBuffer();
                    int a = isCorrect.get(i);
                    //单个词匹配上了
                    if (a > 0)
                    {
                        tokenbuf.append(sInputResult[i]);
                        for(int j = i + 1; j < sInputResult.length; j++)
                        {
                            float b = probBetweenTowTokens(tokenbuf.toString() + sInputResult[j]);
                            if (b > 0) 
                            {
                                tokenbuf.append(sInputResult[j]);
                            }
                            else
                            {
                                hasError = 2;
                                break;
                            }
                        }
                        correctTokens.add(tokenbuf.toString());
                    }
                    //虽然这个单词没有匹配成功，但是其与后面的匹配成功了，所以也是可以加入的
                    else if (probBetweenTowTokens(sInputResult[i] + sInputResult[i + 1]) > 0.0)
                    {
                        tokenbuf.append(sInputResult[i]).append(sInputResult[i + 1]);
                        for(int j = i + 2; j < sInputResult.length; j++)
                        {
                            float b = probBetweenTowTokens(tokenbuf.toString() + sInputResult[j]);
                            if (b > 0) 
                            {
                                tokenbuf.append(sInputResult[j]);
                            }
                            else
                            {
                                hasError = 2;
                                break;
                            }
                        }
                        //这里的correctTokens可能为空
                        correctTokens.add(tokenbuf.toString());
                    }
                }
            }
        } 
        else if (sInputResult.length == 2)
        {
            if (probBetweenTowTokens(sInputResult[0] + sInputResult[1]) > 0)
            {
                correctTokens.add(sInputResult[0] + sInputResult[1]);
            }
        }
        return correctTokens ;
    }
    
    private float probBetweenTowTokens(String token)
    {
        int count = wordCountMap.get(token) == null ? 0 : wordCountMap.get(token);
        int commonCount = commonWordCountMap.get(token) == null ? 0 : commonWordCountMap.get(token);
        count -= commonCount * 20;
        if (count < 0)
        {
            return (float) 0.0;
        }
        if (totalTokensCount > 0 )
        {
            return (float) count / totalTokensCount;
        }
        else
        {
            return (float) 0.0;
        }        
    }
    
    private List<SentenceElement> getCandidateSentenceList(String sInput)
    {
        List<SentenceElement> retMoveNameList = new ArrayList<SentenceElement>();
        int moveLength = sInput.length();
        //TODO
        for (int i = 1; i <= moveLength + 10; i ++)
        {
            List<SentenceElement> moveNameList = sentenceLengthToSentenceMap.get(i);
            if (moveNameList != null)
            {
                retMoveNameList.addAll(moveNameList);
            }
        }
        return retMoveNameList;
    }
    
    private List<SentenceElement> sortList(final SentenceElement originName, List<SentenceElement> originList)
    {
        //TODO 这回需要去重，因为片名库中可能有重复的
        Set<SentenceElement> set = new HashSet<SentenceElement>(originList);
        originList = new ArrayList<SentenceElement>(set);
        Collections.sort(originList, new Comparator<SentenceElement>() 
        {
            public int compare(SentenceElement o1, SentenceElement o2)
            {
                double distance1 = EditDistanceUtils.getEditDistance(o1, originName);
                double distance2 = EditDistanceUtils.getEditDistance(o2, originName);
                if (distance1 > distance2)
                {
                    return 1;
                }
                else if (distance1 < distance2)
                {
                    return -1;
                }
                else
                {
                    return 0;
                }
            }
        });
        
        if (originList.size() > selectNum)
        {
            return originList.subList(0, selectNum);
        }
        else
        {
            return originList;
        }
    }
    
    private List<String> getSentenceList(List<SentenceElement> elementList)
    {
        List<String> moveNameList = new ArrayList<String>();
        for (SentenceElement element : elementList)
        {
            moveNameList.add(element.getSentence());
        }
        return moveNameList;
    }
    
    /* ------------------------------------------- pinyin ------------------------------------------*/
    
    @Override
    public List<String> correctWithPinyin(String targetStr)
    {
        return suggest2(targetStr);
    }
    
    private void initPinyinCountMap()
    {
        lock.lock();
        BufferedReader br = null;
        try
        {
            Map<PinyinElement, Integer> wordCountMapTmp = new HashMap<PinyinElement, Integer>();
            File targetFile = new File(targetFilePath);
            br = new BufferedReader(new FileReader(targetFile));
            
            String wordsline = null;
            while ((wordsline = br.readLine()) != null)
            {
                String[] words = wordsline.trim().split(" ");
                List<PinyinElement> pinyinList = new ArrayList<PinyinElement>();
                for (String word : words)
                {
                    pinyinList.add(new PinyinElement(word));
                }
                for (int i = 0; i < pinyinList.size(); i ++)
                {
                    PinyinElement element = pinyinList.get(i);
                    int wordCount = wordCountMapTmp.get(element) == null ? 0 : wordCountMapTmp.get(element);
                    wordCountMapTmp.put(element, wordCount + 1);
                    totalTokensCount2 += 1;
                    if (words.length > 1 && i < words.length - 1)
                    {
                        PinyinElement element1 = PinyinUtils.append(pinyinList.get(i), pinyinList.get(i + 1));
                        int wordStrCount = wordCountMapTmp.get(element1) == null ? 0 : wordCountMapTmp.get(element1);
                        wordCountMapTmp.put(element1, wordStrCount+1);
                        totalTokensCount2 += 1;
                    }
                }               
            }
            pinyinCountMap = wordCountMapTmp;
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        finally
        {
            lock.unlock();
            if (br != null)
            {
                try
                {
                    br.close();
                } 
                catch (IOException e)
                {
                    //Do nothing
                }
            }
        }
    }
    
    private List<String> suggest2(String sInput)
    {
        List<SentenceElement> correctedList = new ArrayList<SentenceElement>();
        List<SentenceElement> crtTempList = new ArrayList<SentenceElement>();

        char[] str2char = sInput.toCharArray();
        PinyinElement[] sInputResult = new PinyinElement[str2char.length];
        for (int t = 0; t < str2char.length; t++)
        {
            sInputResult[t] = new PinyinElement(String.valueOf(str2char[t]));
        }
        PinyinElement[] MaxAndSecondMaxSequnce = getMaxAndSecondMaxSequnce2(sInputResult);
        System.out.println(Arrays.toString(MaxAndSecondMaxSequnce));
        MaxAndSecondMaxSequnce = adjustMaxAndSecondMaxSequnceForPinyin(MaxAndSecondMaxSequnce);
        if (MaxAndSecondMaxSequnce == null || MaxAndSecondMaxSequnce.length == 0)
        {
            return new ArrayList<String>();
        }
        if (hasError != 0)
        {
            if (MaxAndSecondMaxSequnce.length > 1)
            {
                PinyinElement maxSequence = MaxAndSecondMaxSequnce[0];
                PinyinElement maxSequence2 = MaxAndSecondMaxSequnce[1];
                //这里遍历sentence，可以做成并行处理，提高效率
                List<SentenceElement> candidateSentenceList = getCandidateSentenceList2(sInput);
                
                for (int j = 0; j < candidateSentenceList.size(); j++)
                {
                    SentenceElement sentenceElement = candidateSentenceList.get(j);
                    if (maxSequence2.isEmpty())
                    {
                        if (sentenceElement.contains(maxSequence))
                        {
                            correctedList.add(candidateSentenceList.get(j));
                        }
                    }
                    else 
                    {
                        if (sentenceElement.contains(maxSequence) && sentenceElement.contains(maxSequence2))
                        {
                            crtTempList.add(candidateSentenceList.get(j));
                        }
                        else if (sentenceElement.contains(maxSequence)) 
                        {
                            correctedList.add(candidateSentenceList.get(j));
                        }
                        else if (sentenceElement.contains(maxSequence2))
                        {
                            correctedList.add(candidateSentenceList.get(j));
                        }
                    }
                }
                
                if (crtTempList.size() > 0)
                {
                    correctedList.clear();
                    correctedList.addAll(crtTempList);
                }
                SentenceElement element = new SentenceElement(sInput);
                correctedList = sortList(element, correctedList);
            }
            //TODO
            else if (MaxAndSecondMaxSequnce.length == 1)
            {
                PinyinElement maxSequence = MaxAndSecondMaxSequnce[0];
                List<SentenceElement> candidateSentenceList = getCandidateSentenceList2(sInput);
                
                for (int j = 0; j < candidateSentenceList.size(); j++)
                {
                    SentenceElement sentenceElement = candidateSentenceList.get(j);
                    if (sentenceElement.contains(maxSequence))
                    {
                        correctedList.add(candidateSentenceList.get(j));
                    }
                }
            }
        }
        else 
        {
            SentenceElement element = new SentenceElement(sInput);
            correctedList.add(element);
        }
        
        return getSentenceList(correctedList);
    }
    
    private PinyinElement[] adjustMaxAndSecondMaxSequnceForPinyin(PinyinElement[] maxAndSecondMaxSequnce)
    {
        if (maxAndSecondMaxSequnce == null || maxAndSecondMaxSequnce.length == 0)
        {
            return null;
        }
        List<PinyinElement> retList = new ArrayList<PinyinElement>();
        for (PinyinElement element : maxAndSecondMaxSequnce)
        {
            if (element.getLength() >= 2)
            {
                retList.add(element);
            }
        }
        return retList.toArray(new PinyinElement[retList.size()]);
    }
    
    /**
     * 这里应该是找到前两个可以匹配到最长的token，同时要选择出现频率较低的，这样可以包含更对的特征信息
     * 同时还要对于其子字符串中提取出出现频率最大的字串，将其从字符串中删除
     * 
     * 对于拼音，不能输出单个的音节
     * 
     * @param sInputResult
     * @return
     */
    private PinyinElement[] getMaxAndSecondMaxSequnce2(PinyinElement[] sInputResult)
    {
        //这句话中每个单词开始能够找到的最长的String，并且该String是在tokenMap中可以找的，把这个写入到correctTokens中
        List<PinyinElement> correctTokens = getCorrectTokens2(sInputResult);
        PinyinElement[] maxAndSecondMaxSeq = new PinyinElement[2];
        if (correctTokens.size() == 0) 
        {
            return null;
        }
        else if (correctTokens.size() == 1)
        {
            maxAndSecondMaxSeq[0] = correctTokens.get(0);
            maxAndSecondMaxSeq[1] = correctTokens.get(0);
            return maxAndSecondMaxSeq;
        }
        
        PinyinElement maxSequence = correctTokens.get(0);
        PinyinElement maxSequence2 = correctTokens.get(correctTokens.size() - 1);
        PinyinElement littleword = new PinyinElement();
        for (int i = 1; i < correctTokens.size(); i++)
        {
            //优先匹配最长的，所以最长的应该是在maxSequence中的
            if (correctTokens.get(i).getLength() > maxSequence.getLength())
            {
                maxSequence = correctTokens.get(i);
            } 
            //如果长度一致，就比较出现的频率
            else if (correctTokens.get(i).getLength() == maxSequence.getLength())
            {
                //单个单词
                if (correctTokens.get(i).getLength() == 1)
                {
                    if (probBetweenTowTokens2(correctTokens.get(i)) > probBetweenTowTokens2(maxSequence)) 
                    {
                        //为什么是maxSequence2？
                        maxSequence2 = correctTokens.get(i);
                    }
                }
                //select words with smaller probability for multi-word, because the smaller has more self information
                else if (correctTokens.get(i).getLength() > 1)
                {
                    if (probBetweenTowTokens2(correctTokens.get(i)) <= probBetweenTowTokens2(maxSequence)) 
                    {
                        //这里为什么选择频率低的呢？
                        maxSequence2 = correctTokens.get(i);
                    }
                }
            } 
            else if (correctTokens.get(i).getLength() > maxSequence2.getLength())
            {
                maxSequence2 = correctTokens.get(i);
            } 
            else if (correctTokens.get(i).getLength() == maxSequence2.getLength())
            {
                if (probBetweenTowTokens2(correctTokens.get(i)) > probBetweenTowTokens2(maxSequence2))
                {
                    maxSequence2 = correctTokens.get(i);
                }
            }
        }
        //delete the sub-word from a string
        if (maxSequence2.getLength() == maxSequence.getLength())
        {
            int maxseqvaluableTokens = maxSequence.getLength();
            int maxseq2valuableTokens = maxSequence2.getLength();
            float min_truncate_prob_a = 0 ;
            float min_truncate_prob_b = 0;
            PinyinElement aword = new PinyinElement();
            PinyinElement bword = new PinyinElement();
            //这里在选择出maxSequence和maxSequence2后，会查看其subString出现的最大频次，出现频率越多，说明其带有的特征信息越少，所以要把这部分除去
            for (int i = 0; i < correctTokens.size(); i++)
            {
                float tokenprob = probBetweenTowTokens2(correctTokens.get(i));
                if ((!maxSequence.equals(correctTokens.get(i))) && maxSequence.contains(correctTokens.get(i)))
                {
                    if (tokenprob >= min_truncate_prob_a)
                    {
                        min_truncate_prob_a = tokenprob ;
                        aword = correctTokens.get(i);
                    }
                }
                else if ((!maxSequence2.equals(correctTokens.get(i))) && maxSequence2.contains(correctTokens.get(i)))
                {
                    if (tokenprob >= min_truncate_prob_b)
                    {
                        min_truncate_prob_b = tokenprob;
                        bword = correctTokens.get(i);
                    }
                }
            }
            //System.out.println(min_truncate_prob_a + " VS " + min_truncate_prob_b);
            //maxSequence的subString频次较小，说明maxSequence2的substring肯定也有了，就会对token的权重进行修改
            //这里是什么情况？？
            if (aword.getLength() > 0 && min_truncate_prob_a < min_truncate_prob_b)
            {
                //对长度进行修改
                maxseqvaluableTokens -= 1 ;
                littleword = maxSequence.remove(aword);
            }
            else if ((maxSequence2.getLength() - bword.getLength()) >= 2)
            {
                maxseq2valuableTokens -= 1 ;
                PinyinElement temp = new PinyinElement(maxSequence2);
                //如果maxSequence也包含maxSequence2除去共性较多的信息后，那么littleword就是maxSequence2
                if (maxSequence.contains(temp.remove(bword)))
                {
                    littleword =  maxSequence2;
                }
                else 
                {
                    littleword =  maxSequence2.remove(bword);
                }
            }
            
            if (maxseqvaluableTokens < maxseq2valuableTokens)
            {
                maxSequence = maxSequence2;
                maxSequence2 = littleword;
            }
            else 
            {
                maxSequence2 = littleword;
            }
            
        }
        maxAndSecondMaxSeq[0] = maxSequence;
        maxAndSecondMaxSeq[1] = maxSequence2;
        return maxAndSecondMaxSeq;
    }
    
    private List<PinyinElement> getCorrectTokens2(PinyinElement[] sInputResult)
    {
        List<PinyinElement> correctTokens = new ArrayList<PinyinElement>();
        float probOne = 0;
        List<Integer> isCorrect = new ArrayList<Integer>();
        for (int i = 0; i < sInputResult.length; i++)
        {
            //这里获取到该单个字对应的token数量占总token的比例
            probOne = probBetweenTowTokens2(sInputResult[i]);
            if (probOne <= 0)
            {
                isCorrect.add(i, 0);
            } 
            else 
            {
                isCorrect.add(i, 1);
            }
        }
     
        //含有两个字符以上的单词
        if (sInputResult.length > 2)
        {
            //这里所有的单个的字都匹配上了
            if (!isCorrect.contains(0))
            {
                //这里是将该sInputResult中所有连在一起的组合的可能性，都查找一遍
                for (int i = 0; i < sInputResult.length - 1; i++)
                {
                    PinyinElement tokenbuf = new PinyinElement();
                    tokenbuf.append(sInputResult[i]);
                    for(int j = i + 1; j < sInputResult.length; j++)
                    {
                        float b = probBetweenTowTokens2(PinyinUtils.append(tokenbuf, sInputResult[j]));
                        //这里应该是保证最大匹配吧
                        if (b > 0)
                        {
                            tokenbuf.append(sInputResult[j]);
                        }
                        else
                        {
                            hasError = 1;
                            if (j < sInputResult.length - 1 && 
                                    probBetweenTowTokens2(PinyinUtils.append(tokenbuf, sInputResult[j], sInputResult[j + 1])) > 0)
                            {
                                tokenbuf.append(sInputResult[j]).append(sInputResult[j + 1]);
                            }
                            else
                            {
                                break;
                            }
                        }                       
                    }
                    correctTokens.add(tokenbuf);
                }
                
                if (probBetweenTowTokens2(sInputResult[sInputResult.length - 1]) > 0)
                {
                    correctTokens.add(sInputResult[sInputResult.length - 1]);
                }
            }
            else 
            {
                for (int i = 0; i < sInputResult.length - 1; i++)
                {
                    PinyinElement tokenbuf = new PinyinElement();
                    int a = isCorrect.get(i);
                    //单个词匹配上了
                    if (a > 0)
                    {
                        tokenbuf.append(sInputResult[i]);
                        for(int j = i + 1; j < sInputResult.length; j++)
                        {
                            float b = probBetweenTowTokens2(PinyinUtils.append(tokenbuf, sInputResult[j]));
                            if (b > 0) 
                            {
                                tokenbuf.append(sInputResult[j]);
                            }
                            else
                            {
                                hasError = 2;
                                break;
                            }
                        }
                        correctTokens.add(tokenbuf);
                    }
                    //虽然这个单词没有匹配成功，但是其与后面的匹配成功了，所以也是可以加入的
                    else if (probBetweenTowTokens2(PinyinUtils.append(sInputResult[i], sInputResult[i + 1])) > 0.0)
                    {
                        tokenbuf.append(sInputResult[i]).append(sInputResult[i + 1]);
                        for(int j = i + 2; j < sInputResult.length; j++)
                        {
                            float b = probBetweenTowTokens2(PinyinUtils.append(tokenbuf, sInputResult[j]));
                            if (b > 0) 
                            {
                                tokenbuf.append(sInputResult[j]);
                            }
                            else
                            {
                                hasError = 2;
                                break;
                            }
                        }
                        //这里的correctTokens可能为空
                        correctTokens.add(tokenbuf);
                    }
                }
            }
        } 
        else if (sInputResult.length == 2)
        {
            if (probBetweenTowTokens2(PinyinUtils.append(sInputResult[0], sInputResult[1])) > 0)
            {
                correctTokens.add(PinyinUtils.append(sInputResult[0], sInputResult[1]));
            }
        }
        return correctTokens;
    }
    
    private float probBetweenTowTokens2(PinyinElement token)
    {
        int count = pinyinCountMap.get(token) == null ? 0 : pinyinCountMap.get(token);
        if (totalTokensCount2 > 0 )
        {
            return (float) count / totalTokensCount2;
        }
        else
        {
            return (float) 0.0;
        }        
    }
    
    private List<SentenceElement> getCandidateSentenceList2(String sInput)
    {
        List<SentenceElement> retMoveNameList = new ArrayList<SentenceElement>();
        int moveLength = sInput.length();
        //TODO
        for (int i = 1; i <= moveLength * 2; i ++)
        {
            List<SentenceElement> moveNameList = sentenceLengthToSentenceMap.get(i);
            if (moveNameList != null)
            {
                retMoveNameList.addAll(moveNameList);
            }
        }
        return retMoveNameList;
    }
}

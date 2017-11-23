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

import org.apache.log4j.Logger;

import com.emotibot.correction.constants.Constants;
import com.emotibot.correction.element.PinyinElement;
import com.emotibot.correction.element.SentenceElement;
import com.emotibot.correction.utils.EditDistanceUtils;
import com.emotibot.correction.utils.PinyinUtils;
import com.emotibot.correction.utils.SegementUtils;
import com.emotibot.middleware.conf.ConfigManager;
import com.emotibot.middleware.utils.StringUtils;

public class CorrectionService2Impl implements CorrectionService
{
    private String originalFilePath = null;
    private String targetFilePath = null;
    private String commonSentenceFilePath = null;
    private String commonSentenceTargetFilePath = null;
    
    private int hasError = 0;
    private long totalTokensCount = 0L;
    private long totalTokensCount2 = 0L;
    @SuppressWarnings("unused")
    private long totalCommonTokensCount = 0L;
    
    private static final Logger logger = Logger.getLogger(CorrectionService2Impl.class);
    /**
     * 记录汉字到拼音的映射
     */
    private Map<String, Integer> wordCountMap = new HashMap<String, Integer>();
    private Map<PinyinElement, Integer> pinyinCountMap = new HashMap<PinyinElement, Integer>();
    /**
     * 去掉重复字段，没有拼音，只有汉字，也需要逐字来处理
     */
    private Map<String, Integer> commonWordCountMap = new HashMap<String, Integer>();
    
    /**
     * token 到sentence的映射，汉字和拼音均有逐字的映射
     */
    private Map<String, Set<String>> tokenToTargetSentenceMap = new HashMap<String,Set<String>>();
    private Map<String, Set<String>> pinyinTokenToTargetSentenceMap = new HashMap<String,Set<String>>();
    
    /**
     * 标准语句到sentenceElement的映射，在最后排序时需要使用
     */
    private Map<String, SentenceElement> targetSentenceToElementMap = new HashMap<String, SentenceElement>();
    
    private ReentrantLock lock = new ReentrantLock();
    
    public CorrectionService2Impl()
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
            initWordCountMap();
            initPinyinCountMap();
            initCommonWordCountMap();
            initSentenceMap();
        } 
        
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
            //分词前的句子
            String originalLine = null;
            while ((wordsline = br.readLine()) != null)
            {
                originalLine = wordsline.replace(" ", "");
                String[] words = wordsline.trim().split(" ");
                for (int i = 0; i < words.length; i++)
                {
                    /**
                     * 这里是直接的分词结果，所以可以加的比较多
                     */
                    int wordCount = wordCountMapTmp.get(words[i]) == null ? 0 : wordCountMapTmp.get(words[i]);
                    wordCountMapTmp.put(words[i], wordCount + Constants.WORD_MATCH_COUNT);
                    addTokenToTargetSentenceMap(words[i], originalLine);
                    totalTokensCount += Constants.WORD_MATCH_COUNT;
                    
                    /** ----------------------- single word start ----------------------*/
                    String word = words[i];
                    for (int j = 0; j < word.length(); j ++)
                    {
                        String chat = String.valueOf(word.charAt(j));
                        int wordCount1 = wordCountMapTmp.get(chat) == null ? 0 : wordCountMapTmp.get(chat);
                        wordCountMapTmp.put(chat, wordCount1 + Constants.WORD_SINGLE_MATCH_COUNT);
                        addTokenToTargetSentenceMap(chat, originalLine);
                        totalTokensCount += Constants.WORD_SINGLE_MATCH_COUNT;
                    }
                    /** ----------------------- single word end ----------------------*/
                    
                    if (words.length > 1 && i < words.length - 1)
                    {
                        StringBuffer wordStrBuf = new StringBuffer();
                        wordStrBuf.append(words[i]).append(words[i + 1]);
                        int wordStrCount = wordCountMapTmp.get(wordStrBuf.toString()) == null 
                                                             ? 0 : wordCountMapTmp.get(wordStrBuf.toString());
                        wordCountMapTmp.put(wordStrBuf.toString(), wordStrCount + Constants.WORD_MATCH_COUNT);
                        addTokenToTargetSentenceMap(wordStrBuf.toString(), originalLine);
                        totalTokensCount += Constants.WORD_MATCH_COUNT;
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
    
    private void initPinyinCountMap()
    {
        lock.lock();
        BufferedReader br = null;
        try
        {
            Map<PinyinElement, Integer> pinyinCountMapTmp = new HashMap<PinyinElement, Integer>();
            File targetFile = new File(targetFilePath);
            br = new BufferedReader(new FileReader(targetFile));
            
            String wordsline = null;
            String targetline = null;
            while ((wordsline = br.readLine()) != null)
            {
                targetline = wordsline.replace(" ", "");
                String[] words = wordsline.trim().split(" ");
                List<PinyinElement> pinyinList = new ArrayList<PinyinElement>();
                for (String word : words)
                {
                    pinyinList.add(new PinyinElement(word));
                }
                for (int i = 0; i < pinyinList.size(); i ++)
                {
                    PinyinElement element = pinyinList.get(i);
                    int wordCount = pinyinCountMapTmp.get(element) == null ? 0 : pinyinCountMapTmp.get(element);
                    pinyinCountMapTmp.put(element, wordCount + Constants.PINYIN_MATCH_COUNT);
                    addPinyinTokenToTargetSentenceMap(element.getPinyin(), targetline);
                    totalTokensCount2 += Constants.PINYIN_MATCH_COUNT;
                    
                    /** ----------------------- single word start ----------------------*/
                    for (int j = 0; j < element.getLength(); j ++)
                    {
                        PinyinElement element1 = element.subPinyinElement(j, j + 1);
                        int wordCount1 = pinyinCountMapTmp.get(element1) == null ? 0 : pinyinCountMapTmp.get(element1);
                        pinyinCountMapTmp.put(element1, wordCount1 + Constants.PINYIN_SINGLE_MATCH_COUNT);
                        addPinyinTokenToTargetSentenceMap(element1.getPinyin(), targetline);
                        totalTokensCount2 += Constants.PINYIN_SINGLE_MATCH_COUNT;
                    }
                    /** ----------------------- single word end ----------------------*/
                    
                    if (words.length > 1 && i < words.length - 1)
                    {
                        PinyinElement element1 = PinyinUtils.append(pinyinList.get(i), pinyinList.get(i + 1));
                        int wordStrCount = pinyinCountMapTmp.get(element1) == null ? 0 : pinyinCountMapTmp.get(element1);
                        pinyinCountMapTmp.put(element1, wordStrCount + Constants.PINYIN_MATCH_COUNT);
                        addPinyinTokenToTargetSentenceMap(element1.getPinyin(), targetline);
                        totalTokensCount2 += Constants.PINYIN_MATCH_COUNT;
                    }
                }               
            }
            pinyinCountMap = pinyinCountMapTmp;
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
                    commonWordCountMapTmp.put(words[i], wordCount + Constants.COMMON_MATCH_COUNT);
                    totalCommonTokensCount += Constants.COMMON_MATCH_COUNT;
                    
                    String word = words[i];
                    for (int j = 0; j < word.length(); j ++)
                    {
                        String chat = String.valueOf(word.charAt(j));
                        int wordCount1 = commonWordCountMapTmp.get(chat) == null ? 0 : commonWordCountMapTmp.get(chat);
                        commonWordCountMapTmp.put(chat, wordCount1 + Constants.COMMON_SINGLE_MATCH_COUNT);
                        totalCommonTokensCount += Constants.COMMON_SINGLE_MATCH_COUNT;
                    }
                    
                    if (words.length > 1 && i < words.length - 1)
                    {
                        StringBuffer wordStrBuf = new StringBuffer();
                        wordStrBuf.append(words[i]).append(words[i + 1]);
                        int wordStrCount = commonWordCountMapTmp.get(wordStrBuf.toString()) == null 
                                                             ? 0 : commonWordCountMapTmp.get(wordStrBuf.toString());
                        commonWordCountMapTmp.put(wordStrBuf.toString(), wordStrCount + Constants.COMMON_MATCH_COUNT);
                        totalCommonTokensCount += Constants.COMMON_MATCH_COUNT;
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
            Map<String, SentenceElement> targetSentenceToElementMapTmp = new HashMap<String, SentenceElement>();
            File originalFile = new File(originalFilePath);
            br = new BufferedReader(new FileReader(originalFile));
            
            String sentence = null;
            while ((sentence = br.readLine()) != null)
            {
                SentenceElement element = new SentenceElement(sentence.trim());
                targetSentenceToElementMapTmp.put(sentence.trim(), element);
            }
            targetSentenceToElementMap = targetSentenceToElementMapTmp;
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
    
    private void addTokenToTargetSentenceMap(String token, String sentence)
    {
        Set<String> targetSentenceSet = tokenToTargetSentenceMap.get(token);
        if (targetSentenceSet == null)
        {
            targetSentenceSet = new HashSet<String>();
            tokenToTargetSentenceMap.put(token, targetSentenceSet);
        }
        targetSentenceSet.add(sentence);
    }
    
    private void addPinyinTokenToTargetSentenceMap(String token, String sentence)
    {
        Set<String> targetSentenceSet = pinyinTokenToTargetSentenceMap.get(token);
        if (targetSentenceSet == null)
        {
            targetSentenceSet = new HashSet<String>();
            pinyinTokenToTargetSentenceMap.put(token, targetSentenceSet);
        }
        targetSentenceSet.add(sentence);
    }
    
    /*********************************** word only start ************************************/
    
    @Override
    public List<String> correct(String targetStr)
    {
        return suggest(targetStr);
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
        logger.info(Arrays.toString(MaxAndSecondMaxSequnce));
        if (MaxAndSecondMaxSequnce == null)
        {
            return new ArrayList<String>();
        }
        //TODO: 举例天堂回讯，正确是天堂回信，分词出天堂，同时天堂也在正确的片名中，如何处理？
        if (MaxAndSecondMaxSequnce.length > 1)
        {
            String maxSequence = MaxAndSecondMaxSequnce[0];
            String maxSequence2 = MaxAndSecondMaxSequnce[1];
            //这里遍历sentence，可以做成并行处理，提高效率
            List<SentenceElement> candidateSentenceList = getCandidateSentenceList(maxSequence, maxSequence2);
            
            //这里是汉字匹配
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
        }
        if (hasError == 0)
        {
            //可能音同字不同
            if (targetSentenceToElementMap.containsKey(sInput))
            {
                SentenceElement element = new SentenceElement(sInput);
                correctedList.add(element);
            }
        }
        SentenceElement element = new SentenceElement(sInput);
        correctedList = sortList(element, correctedList);
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
            if (compareFloatToInt(probOne, 0) <= 0)
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
                        if (compareFloatToInt(b, 0) > 0)
                        {
                            tokenbuf.append(sInputResult[j]);
                        }
                        else
                        {
                            hasError = 1;
                            //虽然tokenbuf.toString() + sInputResult[j]不存在，但是tokenbuf.toString() + sInputResult[j] + sInputResult[j + 1]存在
                            if (j < sInputResult.length - 1 && 
                                    compareFloatToInt(probBetweenTowTokens(tokenbuf.toString() + sInputResult[j] + sInputResult[j + 1]), 0) > 0)
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
                
                if (compareFloatToInt(probBetweenTowTokens(sInputResult[sInputResult.length - 1]), 0) > 0)
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
                            if (compareFloatToInt(b, 0) > 0) 
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
                    else if (compareFloatToInt(probBetweenTowTokens(sInputResult[i] + sInputResult[i + 1]), 0) > 0)
                    {
                        tokenbuf.append(sInputResult[i]).append(sInputResult[i + 1]);
                        for(int j = i + 2; j < sInputResult.length; j++)
                        {
                            float b = probBetweenTowTokens(tokenbuf.toString() + sInputResult[j]);
                            if (compareFloatToInt(b, 0) > 0)
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
                if (compareFloatToInt(probBetweenTowTokens(sInputResult[sInputResult.length - 1]), 0) > 0)
                {
                    correctTokens.add(sInputResult[sInputResult.length - 1]);
                }
            }
        } 
        else if (sInputResult.length == 2)
        {
            if (this.compareFloatToInt(probBetweenTowTokens(sInputResult[0] + sInputResult[1]), 0) > 0)
            {
                correctTokens.add(sInputResult[0] + sInputResult[1]);
            }
            else
            {
                if (this.compareFloatToInt(probBetweenTowTokens(sInputResult[0]), 0) > 0)
                {
                    correctTokens.add(sInputResult[0]);
                }
                if (this.compareFloatToInt(probBetweenTowTokens(sInputResult[1]), 0) > 0)
                {
                    correctTokens.add(sInputResult[1]);
                }
            }
        }
        else if (sInputResult.length == 1)
        {
            if (this.compareFloatToInt(probBetweenTowTokens(sInputResult[0]), 0) > 0)
            {
                correctTokens.add(sInputResult[0]);
            }
        }
        return correctTokens;
    }
    
    private float probBetweenTowTokens(String token)
    {
        int count = wordCountMap.get(token) == null ? 0 : wordCountMap.get(token);
        int commonCount = commonWordCountMap.get(token) == null ? 0 : commonWordCountMap.get(token);
        count -= commonCount * Constants.COMMON_WORD_TIMES;
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
    
    //这里直接抓取token
    private List<SentenceElement> getCandidateSentenceList(String maxSequence, String maxSequence2)
    {
        Set<String> retStringSet = new HashSet<>();
        if (!StringUtils.isEmpty(maxSequence))
        {
            Set<String> targetSentenceList = tokenToTargetSentenceMap.get(maxSequence);
            if (targetSentenceList != null)
            {
                retStringSet.addAll(targetSentenceList);
            }
        }
        if (!StringUtils.isEmpty(maxSequence2))
        {
            Set<String> targetSentenceList = tokenToTargetSentenceMap.get(maxSequence2);
            if (targetSentenceList != null)
            {
                retStringSet.addAll(targetSentenceList);
            }
        }
        List<SentenceElement> retMoveNameList = new ArrayList<SentenceElement>();
        for (String target : retStringSet)
        {
            SentenceElement element = targetSentenceToElementMap.get(target);
            if (element != null)
            {
                retMoveNameList.add(element);
            }
        }
        return retMoveNameList;
    }
    
    /*********************************** word only end ************************************/

    
    
    
    
    /*********************************** pinyin start ************************************/
    
    @Override
    public List<String> correctWithPinyin(String targetStr)
    {
        return suggest2(targetStr);
    }
    
    private List<String> suggest2(String sInput)
    {
        List<SentenceElement> correctedList = new ArrayList<SentenceElement>();
        List<SentenceElement> crtTempList = new ArrayList<SentenceElement>();

        char[] str2char = sInput.toCharArray();
        SentenceElement[] sInputResult = new SentenceElement[str2char.length];
        for (int t = 0; t < str2char.length; t++)
        {
            sInputResult[t] = new SentenceElement(String.valueOf(str2char[t]));
        }
        SentenceElement[] MaxAndSecondMaxSequnce = getMaxAndSecondMaxSequnce2(sInputResult);
        logger.info(Arrays.toString(MaxAndSecondMaxSequnce));
        if (MaxAndSecondMaxSequnce == null || MaxAndSecondMaxSequnce.length == 0)
        {
            return new ArrayList<String>();
        }
        if (MaxAndSecondMaxSequnce.length > 1)
        {
            SentenceElement maxSequence = MaxAndSecondMaxSequnce[0];
            SentenceElement maxSequence2 = MaxAndSecondMaxSequnce[1];
            //这里遍历sentence，可以做成并行处理，提高效率
            List<SentenceElement> candidateSentenceList = getCandidateSentenceList2(maxSequence.getPinyinEle().getPinyin(), maxSequence2.getPinyinEle().getPinyin());
            
            //这里比较拼音，所以调用的是contains(PinyinElement element)
            for (int j = 0; j < candidateSentenceList.size(); j++)
            {
                SentenceElement sentenceElement = candidateSentenceList.get(j);
                if (maxSequence2 == null || maxSequence2.getLength() == 0)
                {
                    if (sentenceElement.contains(maxSequence.getPinyinEle()))
                    {
                        correctedList.add(candidateSentenceList.get(j));
                    }
                }
                else 
                {
                    if (sentenceElement.contains(maxSequence.getPinyinEle()) && sentenceElement.contains(maxSequence2.getPinyinEle()))
                    {
                        crtTempList.add(candidateSentenceList.get(j));
                    }
                    else if (sentenceElement.contains(maxSequence.getPinyinEle())) 
                    {
                        correctedList.add(candidateSentenceList.get(j));
                    }
                    else if (sentenceElement.contains(maxSequence2.getPinyinEle()))
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
        }
        else if (MaxAndSecondMaxSequnce.length == 1)
        {
            SentenceElement maxSequence = MaxAndSecondMaxSequnce[0];
            List<SentenceElement> candidateSentenceList = getCandidateSentenceList2(maxSequence.getPinyinEle().getPinyin(), null);
            
            for (int j = 0; j < candidateSentenceList.size(); j++)
            {
                SentenceElement sentenceElement = candidateSentenceList.get(j);
                if (sentenceElement.contains(maxSequence.getPinyinEle()))
                {
                    correctedList.add(candidateSentenceList.get(j));
                }
            }
        }
        if (hasError == 0)
        {
            //可能音同字不同
            if (targetSentenceToElementMap.containsKey(sInput))
            {
                SentenceElement element = new SentenceElement(sInput);
                correctedList.add(element);
            }
        }

        SentenceElement element = new SentenceElement(sInput);
        correctedList = sortList(element, correctedList);
        
        return getSentenceList(correctedList);
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
    private SentenceElement[] getMaxAndSecondMaxSequnce2(SentenceElement[] sInputResult)
    {
        //这句话中每个单词开始能够找到的最长的String，并且该String是在tokenMap中可以找的，把这个写入到correctTokens中
        List<SentenceElement> correctTokens = getCorrectTokens2(sInputResult);
        SentenceElement[] maxAndSecondMaxSeq = new SentenceElement[2];
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
        
        SentenceElement maxSequence = correctTokens.get(0);
        SentenceElement maxSequence2 = correctTokens.get(correctTokens.size() - 1);
        SentenceElement littleword = new SentenceElement();
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
            SentenceElement aword = new SentenceElement();
            SentenceElement bword = new SentenceElement();
            //这里在选择出maxSequence和maxSequence2后，会查看其subString出现的最大频次，出现频率越多，说明其带有的特征信息越少，所以要把这部分除去
            for (int i = 0; i < correctTokens.size(); i++)
            {
                float tokenprob = probBetweenTowTokens2(correctTokens.get(i));
                if ((!maxSequence.equals(correctTokens.get(i))) && maxSequence.contains(correctTokens.get(i).getPinyinEle()))
                {
                    if (tokenprob >= min_truncate_prob_a)
                    {
                        min_truncate_prob_a = tokenprob ;
                        aword = correctTokens.get(i);
                    }
                }
                else if ((!maxSequence2.equals(correctTokens.get(i))) && maxSequence2.contains(correctTokens.get(i).getPinyinEle()))
                {
                    if (tokenprob >= min_truncate_prob_b)
                    {
                        min_truncate_prob_b = tokenprob;
                        bword = correctTokens.get(i);
                    }
                }
            }
            logger.debug(min_truncate_prob_a + " VS " + min_truncate_prob_b);
            //maxSequence的subString频次较小，说明maxSequence2的substring肯定也有了，就会对token的权重进行修改
            //这里是什么情况？？
            if (aword.getLength() > 0 && min_truncate_prob_a < min_truncate_prob_b)
            {
                //对长度进行修改
                maxseqvaluableTokens -= 1 ;
                littleword = maxSequence.remove(aword.getPinyinEle());
            }
            else if ((maxSequence2.getLength() - bword.getLength()) >= 2)
            {
                maxseq2valuableTokens -= 1 ;
                SentenceElement temp = new SentenceElement(maxSequence2);
                //如果maxSequence也包含maxSequence2除去共性较多的信息后，那么littleword就是maxSequence2
                if (maxSequence.contains((temp.remove(bword.getPinyinEle()).getPinyinEle())))
                {
                    littleword =  maxSequence2;
                }
                else 
                {
                    littleword =  maxSequence2.remove(bword.getPinyinEle());
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
    
    private List<SentenceElement> getCorrectTokens2(SentenceElement[] sInputResult)
    {
        List<SentenceElement> correctTokens = new ArrayList<SentenceElement>();
        float probOne = 0;
        List<Integer> isCorrect = new ArrayList<Integer>();
        for (int i = 0; i < sInputResult.length; i++)
        {
            //这里获取到该单个字对应的token数量占总token的比例
            probOne = probBetweenTowTokens2(sInputResult[i]);
            if (this.compareFloatToInt(probOne, 0) <= 0)
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
                    SentenceElement tokenbuf = new SentenceElement();
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
                                    compareFloatToInt(probBetweenTowTokens2(PinyinUtils.append(tokenbuf, sInputResult[j], sInputResult[j + 1])), 0) > 0)
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
                
                if (compareFloatToInt(probBetweenTowTokens2(sInputResult[sInputResult.length - 1]), 0) > 0)
                {
                    correctTokens.add(sInputResult[sInputResult.length - 1]);
                }
            }
            else 
            {
                for (int i = 0; i < sInputResult.length - 1; i++)
                {
                    SentenceElement tokenbuf = new SentenceElement();
                    int a = isCorrect.get(i);
                    //单个词匹配上了
                    if (a > 0)
                    {
                        tokenbuf.append(sInputResult[i]);
                        for(int j = i + 1; j < sInputResult.length; j++)
                        {
                            float b = probBetweenTowTokens2(PinyinUtils.append(tokenbuf, sInputResult[j]));
                            if (compareFloatToInt(b, 0) > 0) 
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
                    else if (compareFloatToInt(probBetweenTowTokens2(PinyinUtils.append(sInputResult[i], sInputResult[i + 1])), 0) > 0.0)
                    {
                        tokenbuf.append(sInputResult[i]).append(sInputResult[i + 1]);
                        for(int j = i + 2; j < sInputResult.length; j++)
                        {
                            float b = probBetweenTowTokens2(PinyinUtils.append(tokenbuf, sInputResult[j]));
                            if (compareFloatToInt(b, 0) > 0) 
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
                if (compareFloatToInt(probBetweenTowTokens2(sInputResult[sInputResult.length - 1]), 0) > 0)
                {
                    correctTokens.add(sInputResult[sInputResult.length - 1]);
                }
            }
        } 
        else if (sInputResult.length == 2)
        {
            if (compareFloatToInt(probBetweenTowTokens2(PinyinUtils.append(sInputResult[0], sInputResult[1])), 0) > 0)
            {
                correctTokens.add(PinyinUtils.append(sInputResult[0], sInputResult[1]));
            }
            if (compareFloatToInt(probBetweenTowTokens2(sInputResult[0]), 0) > 0)
            {
                correctTokens.add(PinyinUtils.append(sInputResult[0]));
            }
            if (compareFloatToInt(probBetweenTowTokens2(sInputResult[1]), 0) > 0)
            {
                correctTokens.add(PinyinUtils.append(sInputResult[1]));
            }
        }
        else if (sInputResult.length == 1)
        {
            if (compareFloatToInt(probBetweenTowTokens2(sInputResult[0]), 0) > 0)
            {
                correctTokens.add(PinyinUtils.append(sInputResult[0]));
            }
        }
        return correctTokens;
    }
    
    private float probBetweenTowTokens2(SentenceElement token)
    {
        int count = pinyinCountMap.get(token.getPinyinEle()) == null ? 0 : pinyinCountMap.get(token.getPinyinEle());
        String tokenWord = token.getSentence();
        int commonCount = commonWordCountMap.get(tokenWord) == null ? 0 : commonWordCountMap.get(tokenWord);
        count -= commonCount * Constants.COMMON_PINYIN_TIMES;
        if (totalTokensCount2 > 0 )
        {
            return (float) count / totalTokensCount2;
        }
        else
        {
            return (float) 0.0;
        }
    }
    
    private List<SentenceElement> getCandidateSentenceList2(String maxSequence, String maxSequence2)
    {
        Set<String> retStringSet = new HashSet<>();
        if (!StringUtils.isEmpty(maxSequence))
        {
            Set<String> targetSentenceList = pinyinTokenToTargetSentenceMap.get(maxSequence);
            if (targetSentenceList != null)
            {
                retStringSet.addAll(targetSentenceList);
            }
        }
        if (!StringUtils.isEmpty(maxSequence2))
        {
            Set<String> targetSentenceList = pinyinTokenToTargetSentenceMap.get(maxSequence2);
            if (targetSentenceList != null)
            {
                retStringSet.addAll(targetSentenceList);
            }
        }
        List<SentenceElement> retMoveNameList = new ArrayList<SentenceElement>();
        for (String target : retStringSet)
        {
            SentenceElement element = targetSentenceToElementMap.get(target);
            if (element != null)
            {
                retMoveNameList.add(element);
            }
        }
        return retMoveNameList;
    }
    
    /*********************************** pinyin end ************************************/
    
    private List<String> getSentenceList(List<SentenceElement> elementList)
    {
        List<String> moveNameList = new ArrayList<String>();
        for (SentenceElement element : elementList)
        {
            moveNameList.add(element.getSentence());
        }
        return moveNameList;
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
        
        if (originList.size() > Constants.CHOOSE_NUM)
        {
            return originList.subList(0, Constants.CHOOSE_NUM);
        }
        else
        {
            return originList;
        }
    }
    
    private int compareFloatToInt(float floatVal, int intVal)
    {
        if(Math.abs(floatVal - intVal) < 1e-5)
        {
            return 0;
        }
        else if (floatVal > intVal)
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }
    
}

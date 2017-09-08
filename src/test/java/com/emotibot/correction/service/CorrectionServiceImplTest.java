package com.emotibot.correction.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class CorrectionServiceImplTest
{
    private static final String UNIT_TEST_FILE_PATH = "/Users/emotibot/Documents/workspace/other/correction/file/test.txt";
    
    @Test
    public void test()
    {
        CorrectionService correctionService = new CorrectionServicePinyinImpl();
        BufferedReader br = null;
        try 
        {
            br = new BufferedReader(new FileReader(new File(UNIT_TEST_FILE_PATH)));
        } 
        catch (FileNotFoundException e) 
        {
            e.printStackTrace();
        }
        
        int totalLineNum = 0 ;
        int positiveNum = 0 ;
        String line = null;
        List<String> result = null;
        try 
        {
            while ((line=br.readLine()) != null)
            {
                totalLineNum += 1 ;
                String[] line_gbk = line.trim().split("\t");
                String errorName = line_gbk[1];
                String normalName = line_gbk[0];
                long startTime = System.currentTimeMillis();
                result = correctionService.correct(errorName);
                if (result.contains(normalName)) 
                {
                    positiveNum ++ ;
                    System.out.println("输入电影为: [" + errorName + "]; 候选电影为： " + result + "; 目标电影为: [" + normalName + "]");
                }
                else
                {
                    System.out.println("没有找到合适的电影。输入电影为: [" + errorName + "]; 候选电影为： " + result + "; 目标电影为: [" + normalName + "]");
                }
                long endTime = System.currentTimeMillis();
                System.out.println("用时: [" + (endTime - startTime) + " ms]");
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                br.close();
            } 
            catch (IOException e)
            {
                // Do nothing
            }
        }
        
        float ratio = (float) ((1.0 * positiveNum / totalLineNum) * 100) ;
        System.out.println("正确率为: " + positiveNum + "/" + totalLineNum + " = " + ratio + "%");
    }

}

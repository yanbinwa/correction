package com.emotibot.correction.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.List;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;

public class SegementUtils
{
    public static void segementFile(String originFilePath, String targetFilePath) throws Exception
    {
        segementFileNormal(originFilePath, targetFilePath);
    }
    
    public static void segementFile(String originFilePath, String targetFilePath, String commonSentenceFilePath, String commonSentenceTargetFilePath) throws Exception
    {
        segementFileNormal(originFilePath, targetFilePath);
        segementFileWithCommonSentence(commonSentenceFilePath, commonSentenceTargetFilePath);
    }
    
    private static void segementFileNormal(String originFilePath, String targetFilePath) throws Exception
    {
        boolean ret = checkFiles(originFilePath, targetFilePath, true);
        if (!ret)
        {
            throw new Exception();
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(originFilePath)));
        FileWriter fw = new FileWriter(targetFilePath);
        try
        {
            String line = null;
            while((line = br.readLine()) != null)
            {
                List<Term> termList = StandardTokenizer.segment(line);
                String output = "";
                for(Term term : termList)
                {
                    output = output + term.word + " ";
                }
                fw.write(output.trim() + "\r\n");
            }
        }
        finally
        {
            if (br != null)
            {
                br.close();
            }
            if (fw != null)
            {
                fw.close();
            }
        }
    }
    
    
    private static void segementFileWithCommonSentence(String commonSentenceFilePath, String commonSentenceTargetFilePath) throws Exception
    {
        
        boolean ret = checkFiles(commonSentenceFilePath, commonSentenceTargetFilePath, true);
        if (!ret)
        {
            throw new Exception();
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(commonSentenceFilePath)));
        FileWriter fw = new FileWriter(commonSentenceTargetFilePath);
        try
        {
            String line = null;
            while((line = br.readLine()) != null)
            {
                List<Term> termList = StandardTokenizer.segment(line);
                String output = "";
                for(Term term : termList)
                {
                    output = output + term.word + " ";
                }
                fw.write(output.trim() + "\r\n");
            }
        }
        finally
        {
            if (br != null)
            {
                br.close();
            }
            if (fw != null)
            {
                fw.close();
            }
        }
    }
    
    
    private static boolean checkFiles(String originFilePath, String targetFilePath, boolean rewrite)
    {
        File originFile = new File(originFilePath);
        if (!originFile.exists() || !originFile.isFile())
        {
            System.out.println("origin file not exist");
            return false;
        }
        
        File segementFile = new File(targetFilePath);
        if (segementFile.exists())
        {
            if (segementFile.isDirectory())
            {
                System.out.println("segement file is exist and is directory");
                return false;
            }
            else
            {
                if (rewrite)
                {
                    segementFile.delete();
                }
            }
        }
        return true;
    }
}

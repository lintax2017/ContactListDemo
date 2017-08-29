package com.droid;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class PinYinStringHelper  {

	/**
	 * 得到 全拼
	 *
	 * @param src
	 * @return
	 */
	public static String getPingYin(String src) {
		char[] t1 = null;

		if(src.trim().length()<1){
			return null;
		}
		//遇到多音字，先转换为单音字（根据指定的表），再取拼音
		String firstChar = src.trim().substring(0,1);
		if(specialHanzi.containsKey(firstChar)) {
			src = src.replace(firstChar,specialHanzi.get(firstChar));
			LogUtil.logWithMethod(new Exception(),"firstChar="+firstChar+" src="+src);
		}

		t1 = src.toCharArray();
		String[] t2 = new String[t1.length];
		HanyuPinyinOutputFormat t3 = new HanyuPinyinOutputFormat();
		t3.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		t3.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		t3.setVCharType(HanyuPinyinVCharType.WITH_V);
		String t4 = "";
		int t0 = t1.length;
		try {
			for (int i = 0; i < t0; i++) {
				// 判断是否为汉字字符
				if (java.lang.Character.toString(t1[i]).matches(
						"[\\u4E00-\\u9FA5]+")) {
					t2 = PinyinHelper.toHanyuPinyinStringArray(t1[i], t3);
					t4 += t2[0];
				} else {
					t4 += java.lang.Character.toString(t1[i]);
				}
			}
			return t4.toUpperCase();
		} catch (BadHanyuPinyinOutputFormatCombination e1) {
			e1.printStackTrace();
		}
		return t4.toUpperCase();
	}
	public static String getFirstPingYin(String src) {
		char[] t1 = null;
		if(src.length()<1){
			return null;
		}

		if(!isHanzi(src)){
			return src.substring(0,1).toUpperCase();
		}

		//遇到多音字，先转换为单音字（根据指定的表），再取拼音
		String firstChar = src.substring(0,1);
		if(specialHanzi.containsKey(firstChar)) {
			src = specialHanzi.get(firstChar);
//			LogUtil.logWithMethod(new Exception(),"firstChar="+firstChar+" src="+src);
		} else {
			src = firstChar;
		}

		t1 = src.toCharArray();
		String[] t2 = new String[t1.length];
		HanyuPinyinOutputFormat t3 = new HanyuPinyinOutputFormat();
		t3.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		t3.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		t3.setVCharType(HanyuPinyinVCharType.WITH_V);
		String t4 = "";
		int t0 = t1.length;
		try {
			for (int i = 0; i < t0; i++) {
				// 判断是否为汉字字符
				if (java.lang.Character.toString(t1[i]).matches(
						"[\\u4E00-\\u9FA5]+")) {
					t2 = PinyinHelper.toHanyuPinyinStringArray(t1[i], t3);
					t4 += t2[0];
				} else {
					t4 += java.lang.Character.toString(t1[i]);
				}
			}
			return t4.toUpperCase();
		} catch (BadHanyuPinyinOutputFormatCombination e1) {
			e1.printStackTrace();
		}
		return t4.toUpperCase();
	}
	/**
	 * 得到首字母
	 *
	 * @param str
	 * @return
	 */
	public static String getHeadChar(String str) {
		if(str.trim().length()<1){
			return null;
		}
		//遇到多音字，先转换为单音字（根据指定的表），再取拼音
		String firstChar = str.trim().substring(0,1);
		if(specialHanzi.containsKey(firstChar)) {
			str = specialHanzi.get(firstChar);
			LogUtil.logWithMethod(new Exception(),"firstChar="+firstChar+" str="+str);
		}

		String convert = "";
		char word = str.charAt(0);
		String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
		if (pinyinArray != null) {
			convert += pinyinArray[0].charAt(0);
		} else {
			convert += word;
		}
		return convert.toUpperCase();
	}

	/**
	 * 得到中文首字母缩写
	 *
	 * @param str
	 * @return
	 */
	public static String getPinYinHeadChar(String str) {
		if(str.trim().length()<1){
			return null;
		}
		String convert = "";
		for (int j = 0; j < str.length(); j++) {
			char word = str.charAt(j);
			String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
			if (pinyinArray != null) {
				convert += pinyinArray[0].charAt(0);
			} else {
				convert += word;
			}
		}
		return convert.toUpperCase();
	}


	//多音字替换为我们希望的同样发音的单音字，可以继续增加
	public static Map<String,String> specialHanzi = new HashMap<String,String>();

	static {
		specialHanzi.put("重", "虫");//重->虫
		specialHanzi.put("贾", "甲");//贾->甲
		specialHanzi.put("瞿", "渠");//瞿->渠
		specialHanzi.put("单", "擅");//单->擅
		specialHanzi.put("沈", "审");//沈->审
		specialHanzi.put("解", "谢");//解->谢
		specialHanzi.put("俞", "于");//俞->于
		specialHanzi.put("曾", "增");//曾->增
	}


	public static boolean isHanzi(String str){
		char c = str.charAt(0);
		// 正则表达式，判断首字母是否是英文字母
		Pattern pattern = Pattern.compile("[\\u4E00-\\u9FA5]+");
		if (pattern.matcher(c + "").matches()) {
			return true;
		}
		return false;
	}

}

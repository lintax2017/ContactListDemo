package com.droid;

import android.util.Log;

//import com.bitcom.goodpark.constant.Constants;

import java.util.StringTokenizer;

public class LogUtil {

	public static final int VERBOSE = 1;
	public static final int DEBUG = 2;
	public static final int INFO = 3;
	public static final int WARN = 4;
	public static final int ERROR = 5;
	public static final int NOTHING = 6;
	
	public static final int LEVEL = VERBOSE;//use it to control the output info level
//	public static int LEVEL = (Constants.IS_RELEASE) ? NOTHING : VERBOSE;//use it to control the output info level
	
	public static void v(String tag, String msg) {
		if (LEVEL <= VERBOSE) {
			Log.v(tag, msg);
		}
	}

	public static void d(String tag, String msg) {
		if (LEVEL <= DEBUG) {
			Log.d(tag, msg);
		}
	}

	public static void i(String tag, String msg) {
		if (LEVEL <= INFO) {
			Log.i(tag, msg);
		}
	}

	public static void w(String tag, String msg) {
		if (LEVEL <= WARN) {
			Log.w(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (LEVEL <= ERROR) {
			Log.e(tag, msg);
		}
	}
	
	public static void logWithMethod(Exception e) {
		if (LEVEL > INFO) {
			return ;
		}
		
		StackTraceElement[] trace = e.getStackTrace();
		if (trace == null || trace.length == 0) {
			i("error", "log: get trace info failed");
		}
		
		//get simple class name from trace[0].getClassName()
		String str = trace[0].getClassName() ;
		String split = ".";
		String class_name="";
		StringTokenizer token = new StringTokenizer(str, split);
		while (token.hasMoreTokens()) {  			  
			class_name = token.nextToken();			
		} 
		
		i(class_name, "log: " + trace[0].getMethodName() + ":" + trace[0].getLineNumber());
	}
	
	public static void logWithMethod(Exception e, String msg) {
		if (LEVEL > INFO) {
			return ;
		}
		
		StackTraceElement[] trace = e.getStackTrace();
		if (trace == null || trace.length == 0) {
			i("error", "log: get trace info failed");
		}
		
		//get simple class name from trace[0].getClassName()
		String str = trace[0].getClassName() ;
		String split = ".";
		String class_name="";
		StringTokenizer token = new StringTokenizer(str, split);
		while (token.hasMoreTokens()) {  			  
			class_name = token.nextToken();			
		} 

		i(class_name, "log: " + trace[0].getMethodName() + ":" + trace[0].getLineNumber() + ": " + msg);
		
	}
	

}

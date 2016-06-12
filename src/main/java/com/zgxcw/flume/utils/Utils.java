/**
 * 
 */
package com.zgxcw.flume.utils;

import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * @author summer
 *
 */
public class Utils {

	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	
	public static int toInt(Object val,int defVal){
		try{
			double r = toDouble(val,defVal);
			return (int)r;
		}catch(NumberFormatException ex){
			logger.error("Object "+val+" is not a number.",ex);
			return defVal;
		}
	}
	
	public static double toDouble(Object val,double defVal){
		try{
			return Double.parseDouble(val.toString());
		}catch(NumberFormatException ex){
			logger.error("Object "+val+" is not a number.",ex);
			return defVal;
		}
	}
	
	public static Date toDate(String date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return sdf.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
			return new Date();
		}
	}
	
	public static String toDate0(String date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		if(StringUtils.isEmpty(date))
			return sdf.format(new Date());
		int pos = date.lastIndexOf(":");
		if(pos == -1)
			return sdf.format(new Date());
		date = date.substring(0,pos);
		return date.replaceAll("[\\s|:|-]+", "");
		
	}
	
	public static void main(String[] args) throws Exception{
		
		System.out.println(Integer.parseInt("90"));
		
		System.out.println(toInt("90.91",0));
		
		System.out.println(toInt("-1.91",0));
		
		System.out.println(toDate0("08:10:10,266"));
		System.out.println(toDate0("2016-05-14 08:10:10,266"));
		System.out.println(toDate0("asdasd010,266"));
	}
}

package com.prakashs;

import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.PropertyConfigurator;

public class Main {

	
	public static Properties PROPERTIES;
	public static String[] arguments;
	
	//
	// args[0] - Application properties
	public static void main(String[] args) throws Exception {
		System.setProperty("date.today", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		arguments = args;
		PROPERTIES = new Properties();
		PROPERTIES.load(new FileReader(args[0]));
		
		PropertyConfigurator.configure(PROPERTIES.getProperty("log.properties.file"));

		Runnable instance = (Runnable)Class.forName(args[1]).newInstance();
		new Thread(instance).run();
		
//		long time = 1353384963;
//		System.out.println(new Date(time*1000).toString());
//		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2012-11-20 09:30").getTime());
		
	}

}

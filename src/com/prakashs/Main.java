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
		//Pattern p = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}.[a-zA-z]+.log");
		//System.out.println(p.matcher(args[0]).matches());
		
		//new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse("Tue Sep 06 09:17:11 IST 2011");
		
	}

}

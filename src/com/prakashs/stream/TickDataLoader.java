package com.prakashs.stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.prakashs.Main;


public class TickDataLoader implements Runnable {
    private final static Logger __logger = Logger.getLogger(TickDataLoader.class.getName());
    protected final static SimpleDateFormat __dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    // Other private data
    // instrument,tick,bid,bid_qty,ask,ask_qty
    private HashMap<String, String> loadedFiles;
    
    private boolean loadToDb = false;
    private boolean loadOnlyCSV = true;

    private static String SCRIP_TO_TEST = "UNKNOWN";

    public void run() {
        try{
        	loadedFiles = new HashMap<String, String>();
        	File file = new File(Main.arguments[2]);

        	if(file.isDirectory()){
	        		File[] files = file.listFiles();
	                for(File f : files){
	                    __logger.info("Processing file " + f.getAbsolutePath());
	                    loadTickData(f.getAbsolutePath());
	                }
            }else{
                loadTickData(Main.arguments[2]);
            }
        }catch(Exception ex){
            __logger.error("main", ex);
        }

    }

    private String doubleSlashFileName(String copyStmt) {
        copyStmt = copyStmt.replaceAll("\\\\","\\\\\\\\");
        return copyStmt;
    }

    
    public void loadTickData(String file) throws Exception {
    	LogType type = getLogType(new File(file).getName());
    	
    	if(type == LogType.UNKNOWN) return;
    	//if(type == LogType.SYMMAP){ loadSymMap(file); return; }
    	
    	BufferedReader reader = new BufferedReader(new FileReader(file));
    	String line = null;
    	
    	HashMap<String, TickData> instrumentData = new HashMap<String, TickData>();
    	FileWriter fileWriter = new FileWriter(file+".out");

        FileWriter testInput = new FileWriter(file + ".test.input");
        FileWriter testOutput = new FileWriter(file + ".test.output");
    	
    	String fileDate = null;
    	while((line = reader.readLine()) != null){
    		TickData tickData = null;
    		
    		if(type == LogType.INSTRUMENT) tickData = TickData.parseInstrumentData(line);
    		if(type == LogType.INDEX) {
                if(line.split(",")[1].equals("null")) continue;
                tickData = TickData.parseIndexData(line);
            }

            // We want to test this.
            if(tickData.scrip.equals(SCRIP_TO_TEST)){

                testInput.write(new StringBuffer(tickData.scrip).append(",").append(tickData.tick).append(",").
                        append(tickData).append("\n").toString());
            }
    		
    		Date d = new Date(tickData.tick);
    		if(fileDate == null)fileDate = new SimpleDateFormat("MMM-dd-yy").format(d);
    		int chk = d.getHours()*100+d.getMinutes();
    		if(chk > 1530) continue;
    		
    		TickData lastTick = instrumentData.get(tickData.scrip);
    		if(lastTick != null){
                if(tickData.bid <= 0) tickData.bid = lastTick.bid;
                if(tickData.bidQty <= 0) tickData.bidQty = lastTick.bidQty;
                if(tickData.ask <= 0) tickData.ask = lastTick.ask;
                if(tickData.askQty <= 0) tickData.askQty = lastTick.askQty;
                
//                while(lastTick.tick < tickData.tick - 1000){
//                    lastTick.tick = lastTick.tick + 1000;
//                    String aLine = new StringBuffer(tickData.scrip).append(",").append(lastTick.tick).append(",").
//                                    append(lastTick).append("\n").toString();
//                    fileWriter.write(aLine);
//                }

                if(lastTick.tick != tickData.tick){
                    long tick = lastTick.tick;
                    while(tick <= tickData.tick - 1000){
                        String aLine = new StringBuffer(tickData.scrip).append(",").append(tick).append(",").
                                append(lastTick).append("\n").toString();

                        if(lastTick.bidQty > 0 && lastTick.bid > 0 && lastTick.askQty > 0 && lastTick.ask > 0)
                        {
                            fileWriter.write(aLine);
                        }

                        if(tickData.scrip.equals(SCRIP_TO_TEST)){
                            testOutput.write(aLine);
                        }

                        tick = tick + 1000;
                    }
                }
                
            }else{
                String aLine = new StringBuffer(tickData.scrip).append(",").append(tickData.tick).append(",").
                        append(tickData).append("\n").toString();
                if(tickData.bidQty > 0 && tickData.bid > 0 && tickData.askQty > 0 && tickData.ask > 0)
                {
                    fileWriter.write(aLine);
                }
                if(tickData.scrip.equals(SCRIP_TO_TEST)){
                    testOutput.write(aLine);
                }
            }

    		instrumentData.put(tickData.scrip, tickData);
    	}
    	
    	reader.close();
    	fileWriter.close();
        testInput.close();
        testOutput.close();
    	
    	String table = "instrument_data";
    	if(type == LogType.INDEX) table = "index_data";
    }


    enum LogType{
    	SYMMAP, INDEX, INSTRUMENT, UNKNOWN
    }
    
    private static Pattern __symmap_pattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}.symmap.log");
    private static Pattern __index_pattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}.index.log");
    private static Pattern __instrument_pattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}.instrument.log");
    
	private LogType getLogType(String fileName) {
		if(__symmap_pattern.matcher(fileName).matches()) return LogType.SYMMAP;
    	if(__index_pattern.matcher(fileName).matches()) return LogType.INDEX;
    	if(__instrument_pattern.matcher(fileName).matches()) return LogType.INSTRUMENT;
    	
    	return LogType.UNKNOWN;
	}

}

class TickData{
	public long tick;
	public String scrip;
    public float bid;
    public float bidQty;
    public float ask;
    public float askQty;
    
    public boolean isIndexTick = false;

    public String toString(){
    	if(isIndexTick) return ""+bid;
    	
        return new StringBuffer().append(bid).append(",").append(bidQty).append(",").append(ask).append(",").
                append(askQty).toString();
    }
    
    public static TickData parseInstrumentData(String line) throws Exception{
    	String[] values = line.split(",");
    	TickData tickData = new TickData();
        try{
            tickData.tick = Long.parseLong(values[1]);
        }catch(Exception ex){
            tickData.tick = TickDataLoader.__dateParser.parse(values[1]).getTime();
        }
    	tickData.scrip = values[2];
    	tickData.bid = Float.parseFloat(values[4]);
    	tickData.bidQty = Float.parseFloat(values[5]);
    	tickData.ask = Float.parseFloat(values[6]);
    	tickData.askQty = Float.parseFloat(values[7]);
    	
    	return tickData;
    }
    
    public static TickData parseIndexData(String line) throws Exception {
    	String[] values = line.split(",");
    	TickData tickData = new TickData();
        try{
            tickData.tick = Long.parseLong(values[1]);
        }catch(Exception ex){
            tickData.tick = TickDataLoader.__dateParser.parse(values[1]).getTime();
        }
    	tickData.scrip = values[2];
    	tickData.bid = Float.parseFloat(values[4]) / 100;
    	tickData.isIndexTick = true;
    	
    	return tickData;
    }
}

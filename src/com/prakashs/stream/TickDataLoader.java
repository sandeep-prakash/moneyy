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

    // Driver info
    private final static String DRIVER_CLASS = "org.postgresql.Driver";
    private final static String JDBC_URL = "jdbc:postgresql://localhost/mdata";
    private final static String USER_ID = "postgres";
    private final static String PASSWORD = "p@ssw0rd";

    private final static Logger __logger = Logger.getLogger(TickDataLoader.class.getName());
    protected final static SimpleDateFormat __dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    private static Connection __connection;

    // Statements
    private final static String INSERT_KEY_STMT = "select insert_key(?,?)";
    private final static String GET_INDEX_VALUE = "select get_index_value(?)";
    private final static String BULK_COPY_STMT = "copy %s from '%s' with delimiter ','";
    private final static String INSERT_FILE_NAME = "insert into file_list values(?)";

    // Other private data
    // instrument,tick,bid,bid_qty,ask,ask_qty
    private HashMap<String, String> loadedFiles;
    
    private boolean loadToDb = true;
    private boolean loadOnlyCSV = false;

    public void run() {
        try{
        	buildConnection();
        	loadedFiles = new HashMap<String, String>();
        	File file = new File(Main.arguments[2]);
        	if(Main.arguments.length > 3) loadToDb = Boolean.parseBoolean(Main.arguments[3]);
        	if(Main.arguments.length > 4) loadOnlyCSV = Boolean.parseBoolean(Main.arguments[4]);
        	
        	if(!loadOnlyCSV){
	        	if(file.isDirectory()){
	        		File[] files = file.listFiles();
	                for(File f : files){
	                    __logger.info("Processing file " + f.getAbsolutePath());
	                    loadTickData(f.getAbsolutePath());
	                }
	        	}else{
	        		loadTickData(Main.arguments[2]);
	        	}
        	}else{ // Load Only CSV files
        		__logger.info("Loading only CSV files...");
        		if(file.isDirectory()){
        			__logger.info("Processing directory...");
	        		File[] files = file.listFiles();
	                for(File f : files){
	                    __logger.info("Processing file " + f.getAbsolutePath());
	                    String table = "instrument_data";
	                    if(f.getAbsolutePath().indexOf(".index.") > 0){
		        			table = "index_data";
		        		}
	                    loadPGCSVFiles(table, f.getAbsolutePath());
	                    String date = f.getName().substring(0, 10);
	                    date = new SimpleDateFormat("MMM-dd-yy").format(new SimpleDateFormat("yyyy-MM-dd").parse(date));
	                    insertFileName(date);
	                }
	        	}else{
	        		String table = "instrument_data";
	        		if(Main.arguments[2].indexOf(".index.") > 0){
	        			table = "index_data";
	        		}
	        		loadPGCSVFiles(table, Main.arguments[2]);
	        		String date = new File(Main.arguments[2]).getName().substring(0, 10);
                    date = new SimpleDateFormat("MMM-dd-yy").format(new SimpleDateFormat("yyyy-MM-dd").parse(date));
                    insertFileName(date);
	        	}
        	}
        	__connection.commit();
        	__connection.close();
        }catch(Exception ex){
            __logger.error("main", ex);
        }

    }

    public void buildConnection() throws Exception{
		try {
			Class.forName(DRIVER_CLASS);
		} catch(java.lang.ClassNotFoundException e) {
			__logger.error("buildConnection", e);
        }
		__connection = DriverManager.getConnection(JDBC_URL, USER_ID, PASSWORD);
        __connection.setAutoCommit(false);
	}

    private String doubleSlashFileName(String copyStmt) {
        copyStmt = copyStmt.replaceAll("\\\\","\\\\\\\\");
        return copyStmt;
    }

    
    public void loadTickData(String file) throws Exception {
    	LogType type = getLogType(new File(file).getName());
    	
    	if(type == LogType.UNKNOWN) return;
    	if(type == LogType.SYMMAP){ loadSymMap(file); return; }
    	
    	BufferedReader reader = new BufferedReader(new FileReader(file));
    	String line = null;
    	
    	HashMap<String, TickData> instrumentData = new HashMap<String, TickData>();
    	FileWriter fileWriter = new FileWriter(file+".out");
    	
    	String fileDate = null;
    	while((line = reader.readLine()) != null){
    		TickData tickData = null;
    		
    		if(type == LogType.INSTRUMENT) tickData = TickData.parseInstrumentData(line);
    		if(type == LogType.INDEX) tickData = TickData.parseIndexData(line);
    		
    		Date d = new Date(tickData.tick);
    		if(fileDate == null)fileDate = new SimpleDateFormat("MMM-dd-yy").format(d);
    		int chk = d.getHours()*100+d.getMinutes();
    		if(chk > 1530) continue;
    		
    		TickData lastTick = instrumentData.get(tickData.scrip);
    		if(lastTick != null){
                if(tickData.bid < 0) tickData.bid = lastTick.bid;
                if(tickData.bidQty < 0) tickData.bidQty = lastTick.bidQty;
                if(tickData.ask < 0) tickData.ask = lastTick.ask;
                if(tickData.askQty < 0) tickData.askQty = lastTick.askQty;
                
                while(lastTick.tick < tickData.tick - 1000){
                    lastTick.tick = lastTick.tick + 1000;
                    String aLine = new StringBuffer(tickData.scrip).append(",").append(lastTick.tick).append(",").
                                    append(lastTick).append("\n").toString();
                    fileWriter.write(aLine);
                }
                
            }
    		
    		String aLine = new StringBuffer(tickData.scrip).append(",").append(tickData.tick).append(",").
                    				append(tickData).append("\n").toString();
    		if(lastTick != null && tickData.tick != lastTick.tick) fileWriter.write(aLine);
    		instrumentData.put(tickData.scrip, tickData);
    	}
    	
    	reader.close();
    	fileWriter.close();
    	
    	String table = "instrument_data";
    	if(type == LogType.INDEX) table = "index_data";
    	
    	if(loadToDb) loadPGCSVFiles(table, new File(file+".out").getAbsolutePath());
    	
    	if(!loadedFiles.containsKey(fileDate)){
	    	insertFileName(fileDate);
	        loadedFiles.put(fileDate, "");
    	}
    }

	private void insertFileName(String fileDate) throws SQLException {
		PreparedStatement pStmt = __connection.prepareStatement(INSERT_FILE_NAME);
		pStmt.setString(1, fileDate);
		pStmt.executeUpdate();
	}
    
    private void loadSymMap(String file) throws Exception {
    	PreparedStatement keyLoaderStmt = __connection.prepareStatement(INSERT_KEY_STMT);

        String line = null;
        BufferedReader fileReader = new BufferedReader(new FileReader(file));
        
        while((line = fileReader.readLine()) != null) {
            String[] values = line.trim().split(",");
            
            if(values.length == 4) {
                keyLoaderStmt.setString(1, values[1]);
				keyLoaderStmt.setString(2, values[2]);
				keyLoaderStmt.executeQuery();
            }
        }
        
        __connection.commit();
    }
    
    private void loadPGCSVFiles(String table, String file) throws SQLException {
        String copyStmt = new Formatter().format(BULK_COPY_STMT, table, file).out().toString();
        copyStmt = doubleSlashFileName(copyStmt);
        __logger.info("Copy statement is " + copyStmt);
        // Copy Instrument file to table
        __connection.createStatement().executeUpdate(copyStmt);
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
	
	public double getIndexValue(long tick) throws Exception {
		PreparedStatement pStmt = __connection.prepareStatement(GET_INDEX_VALUE);
        pStmt.setLong(1, tick);
        
        ResultSet rs = pStmt.executeQuery();
        if(rs.next()){
        	return (double)rs.getLong(1)/100.0;
        }else{
        	return 0;
        }
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
    	tickData.tick = TickDataLoader.__dateParser.parse(values[0]).getTime();
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
    	tickData.tick = TickDataLoader.__dateParser.parse(values[0]).getTime();
    	tickData.scrip = values[2];
    	tickData.bid = Float.parseFloat(values[4]);
    	tickData.isIndexTick = true;
    	
    	return tickData;
    }
}

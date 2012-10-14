package com.prakashs.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.omnesys.mw.classes.CIndexStruct;
import com.omnesys.mw.classes.CScripInfo;
import com.omnesys.mw.classes.CSensexInfo;
import com.omnesys.mw.classes.CServerRequest;
import com.omnesys.mw.classes.CStreamData;
import com.omnesys.mw.classes.CTouchLineInfo;
import com.prakashs.Main;
import com.prakashs.strategy.Box;
import com.prakashs.strategy.VolatilityDifference;

/**
 * Created by IntelliJ IDEA.
 * User: sandeep
 * Date: 9/4/11
 * Time: 7:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarketDataService implements Runnable{
	
	private static Logger __instrumentLogger = Logger.getLogger("log.instrument");
	private static Logger __indexLogger = Logger.getLogger("log.index");
	private static Logger __symLogger = Logger.getLogger("log.symmap");
	private static Logger __logger = Logger.getLogger("log.debug");
	
	private ObjectInputStream inStream;
	private Date exchangeTime;
	private HashMap<String, String> symbolMap;
	
	public MarketDataService() throws Exception{
		
	}

	private void initConnection() throws IOException, UnknownHostException,
			SocketException {
		Socket socket = new Socket(
							InetAddress.getByName(Main.PROPERTIES.getProperty("mds.server.ip")), 
												Integer.parseInt(Main.PROPERTIES.getProperty("mds.server.port")));
		socket.setSoTimeout(Integer.parseInt(Main.PROPERTIES.getProperty("recv.timeout")));
		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
		inStream = new ObjectInputStream(socket.getInputStream());
		
		__logger.debug("Instream created");
		
		CServerRequest sRequest = new CServerRequest();
		sRequest.iReqType = 0;
		sRequest.oAccountId = Main.PROPERTIES.getProperty("mds.account.id");
		sRequest.sPortfolio = Main.PROPERTIES.getProperty("mds.account.portfolio");
		sRequest.iDDInteractive = 0;
		
		out.writeObject(sRequest);
		out.flush();
		
		__logger.debug("Request for market data placed...");
		
		sRequest = new CServerRequest();
		sRequest.iReqType = 451;
		sRequest.oAccountId = Main.PROPERTIES.getProperty("mds.account.id");;
		sRequest.iDDInteractive = 0;
		sRequest.sPortfolio = "S&P CNX Nifty";
		sRequest.sExchange = "nse_cm";
		
		out.writeObject(sRequest);
		out.flush();
		
		__logger.debug("Request for INDEX data placed...");
	}

	@Override
	public void run() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String todayStr = df.format(new Date());
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		long stopTime = 0;
		long startTime = 0;
		try{
			startTime = df.parse(todayStr + " 09:15").getTime();
			stopTime = df.parse(todayStr + " 15:35").getTime();
			long sleepFor = startTime - new Date().getTime();
			if(sleepFor > 0){
				__logger.info("Will sleep for " + sleepFor);
				Thread.sleep(sleepFor);
			}
			__logger.info("Will start process now...");
		}catch(Exception ex){
			__logger.error(ex.getMessage(), ex);
		}
		
		try{
			initConnection();
			__logger.debug("Request placed to server...Now, we wait.");
		}catch(Exception ex){
			__logger.error("Could not connect to server.", ex);
			System.exit(0);
		}
		
		while(new Date().getTime() < stopTime){
			try{
				CStreamData oData = ((CStreamData)inStream.readObject());
				
				
				if(oData.iMsgCode == 1){
					CTouchLineInfo tick = ((CTouchLineInfo)oData.oStreamObj);
					// Log tick
					__instrumentLogger.info(toString(tick));
				}
				
				else if(oData.iMsgCode == 0){
					Object [] scrips = (Object[])(Object[])oData.oStreamObj;
					symbolMap = new HashMap<String, String>();
					for(int i = 0; i < scrips.length; i++){
						CScripInfo scrip = (CScripInfo)scrips[i];	
						__symLogger.info(scrip.oExchange + "," + scrip.oScripNo +","+scrip.sTradingSym+","+scrip.oSymbol);
						symbolMap.put(scrip.sTradingSym, scrip.oScripNo);
					}
					initDisruptor();
				}
				
				else if(oData.iMsgCode == 2){
					CSensexInfo tick = ((CSensexInfo)oData.oStreamObj);
					__indexLogger.info(toString(tick));
				}
				
				else if(oData.iMsgCode == 4){
					exchangeTime = (Date)oData.oStreamObj;
				}
				
				else if(oData.iMsgCode == 450){
					Vector oObj = (Vector)oData.oStreamObj;
					
					for (int i = 0; i < oObj.size(); i++) {
			              CIndexStruct cind = (CIndexStruct)oObj.get(i);
			              __logger.debug(cind.s_EXCH_SEG + "," + cind.s_SYMBOL);
					}
				}
				
				else{
					__logger.warn("Unknown code: " + oData.iMsgCode);					
				}
				
				if(ringBuffer != null) publish(oData);   
				
			}catch(Exception ex){
				__logger.error(ex.getMessage(), ex);
				if(ex instanceof SocketTimeoutException || ex instanceof ObjectStreamException || ex instanceof EOFException){
					// Retry
					try {
						__logger.error("Encountered error.", ex);
						initConnection();
					} catch (Exception e) {
						
						// Giving up now...
						__logger.error("Giving up on the connection as reinit failed.", e);
					} 
				}
			}
		}
		
		System.exit(0);
		
	}

	private void publish(CStreamData oData) {
		long sequence = ringBuffer.next();
		CStreamData event = ringBuffer.get(sequence);
		event.iMsgCode = oData.iMsgCode;
		event.oStreamObj = oData.oStreamObj;

		// make the event available to EventProcessors
		ringBuffer.publish(sequence);
	}
	
	private String toString(CTouchLineInfo tick){
		StringBuffer sb = new StringBuffer();
		sb.append(exchangeTime);
		sb.append(",");
		sb.append(tick.oScripNo);
		sb.append(",");
		sb.append(tick.lFeedTime);
		sb.append(",");
		sb.append(tick.iBuyRate);
		sb.append(",");
		sb.append(tick.iBuyQty);
		sb.append(",");
		sb.append(tick.iSellRate);
		sb.append(",");
		sb.append(tick.iSellQty);
		sb.append(",");
		sb.append(tick.iLTRate);
		sb.append(",");
		sb.append(tick.iLTQty);
		sb.append(",");
		sb.append(tick.iOpenInterest);
		sb.append(",");
		sb.append(tick.iTotalBuyQuantity);
		sb.append(",");
		sb.append(tick.iTotalSellQuantity);
		
		return sb.toString();
	}
	
	private String toString(CSensexInfo info){
		StringBuffer sb = new StringBuffer();
		
		sb.append(exchangeTime);
		sb.append(",");
		sb.append(info.oName);
		sb.append(",");
		sb.append(info.lFeedTime);
		sb.append(",");
		sb.append(info.iValue);
		
		return sb.toString();
	}
	
	private static SimpleDateFormat __dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
	
	public static CSensexInfo stringToCSensexInfo(String line) throws Exception{
		CSensexInfo csInfo = new CSensexInfo();
		String[] data = line.split(",");
		csInfo.oName = data[1];
		csInfo.lFeedTime = Long.parseLong(data[2]);
		csInfo.iValue = Integer.parseInt(data[3]);
		return csInfo;
	}
	
	public static CTouchLineInfo stringToCTouchLineInfo(String line) throws Exception{
		CTouchLineInfo ctInfo = new CTouchLineInfo();
		String[] data = line.split(",");
		ctInfo.oScripNo = data[2];
		ctInfo.lFeedTime = __dateFormat.parse(data[0]).getTime();
		ctInfo.iBuyRate = Double.parseDouble(data[4]);
		ctInfo.iBuyQty = (int)Double.parseDouble(data[5]);
		ctInfo.iSellRate = Double.parseDouble(data[6]);
		ctInfo.iSellQty = (int)Double.parseDouble(data[7]);
		
		return ctInfo;
	}
	
	private RingBuffer<CStreamData> ringBuffer;
	private void initDisruptor(){
		Disruptor<CStreamData> disruptor =
				  new Disruptor<CStreamData>(EVENT_FACTORY, 1024, Executors.newCachedThreadPool());
		String[] instruments = Main.PROPERTIES.getProperty("vds.instruments").split(";");
		EventHandlerGroup<CStreamData> handler = null; 
		for(String s: instruments){
			String[] keys = s.split(",");
			int strike = Integer.parseInt(keys[0].substring(3, 7));
			String nmScrip = symbolMap.get("NIFTY12"+keys[0]);
			String fmScrip = symbolMap.get("NIFTY12"+keys[1]);
			__logger.info("Creating VolatilityDifference Strategy with " + nmScrip + ", "+fmScrip + ", " + strike);
			VolatilityDifference vds = new VolatilityDifference(nmScrip, fmScrip, strike);
			handler = disruptor.handleEventsWith(vds);
			__logger.info("Strategy added.."+vds.toString());
		}
		
		ringBuffer = disruptor.start();
	}
	
	public final static EventFactory<CStreamData> EVENT_FACTORY = new EventFactory<CStreamData>()
    {
        public CStreamData create()
        {
            return new CStreamData();
        }

		@Override
		public CStreamData newInstance() {
			// TODO Auto-generated method stub
			return new CStreamData();
		}
    };
	
}

package com.prakashs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import com.omnesys.mw.classes.CScripInfo;
import com.omnesys.mw.classes.CSensexInfo;
import com.omnesys.mw.classes.CServerRequest;
import com.omnesys.mw.classes.CStreamData;
import com.omnesys.mw.classes.CTouchLineInfo;

public class ConnectionTester {
	
	private static Date exchangeTime = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		String serverIP = "125.17.127.101";
		int port = 443;
		
		//connectAlt();
		sendOrder();
		
//		Socket socket = new Socket(InetAddress.getByName(serverIP), port);
//		
//		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
//		//System.out.println("got InPutStream--");
//		
//		CServerRequest sRequest = new CServerRequest();
//		sRequest.iReqType = 0;
//		sRequest.oAccountId = "DS0097-13906";
//		sRequest.sPortfolio = "Test";
//		sRequest.iDDInteractive = 0;
//		
//		out.writeObject(sRequest);
//		out.flush();
//		System.out.println("Request placed to server...please wait");
//		
//		while(true){
//			try{
//				CStreamData oData = ((CStreamData)ois.readObject());
//				//if(oData != null) System.out.println("Message Code: " + oData.iMsgCode);
//				
//				if(oData.iMsgCode == 462){
//					//String s = ((Vector)oData.oStreamObj).elementAt(0).toString();
//					//System.out.println(s);
//				}
//				
//				else if(oData.iMsgCode == 1){
//					CTouchLineInfo oTouchLine = ((CTouchLineInfo)oData.oStreamObj);
//					System.out.println(toString(oTouchLine));
//				}
//				
//				else if(oData.iMsgCode == 0){
//					Object [] scrips = (Object[])(Object[])oData.oStreamObj;
//					System.out.println("Scrip-Trading Sym Map");
//					for(int i = 0; i < scrips.length; i++){
//						CScripInfo scrip = (CScripInfo)scrips[i];	
//						System.out.println(scrip.oExchange + "," + scrip.oScripNo +","+scrip.sTradingSym+","+scrip.oSymbol+",SN:"+scrip.oScripName+",Type:"+scrip.sOptionType+",ED:"+scrip.iExpiryDate+",SP:"+scrip.iStrikePrice+",BLQ:"+scrip.iBoardLotQty+",shp:"+scrip.shPrecision);
//					}
//				}
//				
//				else if(oData.iMsgCode == 2){
//					CSensexInfo oSensex = ((CSensexInfo)oData.oStreamObj);
//					System.out.println(toString(oSensex));
//				}
//				
//				else if(oData.iMsgCode == 4){
//					exchangeTime = (Date)oData.oStreamObj;
//				}
//				
//				else{
//					System.err.println(new Date() + ": Unknown code " + oData.iMsgCode);
//				}
//			}catch(Exception ex){
//				System.err.println(new Date() + ":" + ex.getMessage());
//				ex.printStackTrace(System.err);
//				//System.out.println(ex.getMessage());
//			}
//		}

	}
	
	private static void connectAlt() throws Exception{
		String sIP = "125.17.127.73";
		int port = 443;
		
		Socket socket = new Socket();
		byte[] b = new byte[500];
		socket.connect(new InetSocketAddress(sIP, port));
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		Vector oSocket = (Vector)ois.readObject();
		
		System.out.println("{0}: " + oSocket.get(0));
		System.out.println("{1}: " + oSocket.get(1));
		
	}
	
	private static String toString(CTouchLineInfo info){
		StringBuffer sb = new StringBuffer();
		sb.append(new Date().toString());
		sb.append(",");
		sb.append(exchangeTime);
		sb.append(",");
		sb.append(info.oScripNo);
		sb.append(",");
		sb.append(new Date(info.lFeedTime*1000));
		sb.append(",");
		sb.append(info.iBuyRate);
		sb.append(",");
		sb.append(info.iBuyQty);
		sb.append(",");
		sb.append(info.iSellRate);
		sb.append(",");
		sb.append(info.iSellQty);
		sb.append(",");
		sb.append(info.iLTRate);
		sb.append(",");
		sb.append(info.iLTQty);
		sb.append(",");
		sb.append(info.iOpenInterest);
		sb.append(",");
		sb.append(info.iTotalBuyQuantity);
		sb.append(",");
		sb.append(info.iTotalSellQuantity);
		
		return sb.toString();
	}
	
	private static String toString(CSensexInfo info){
		StringBuffer sb = new StringBuffer();
		
		sb.append(new Date().toString());
		sb.append(",");
		sb.append(exchangeTime);
		sb.append(",");
		sb.append(info.oName);
		sb.append(",");
		sb.append(new Date(info.lFeedTime*1000));
		sb.append(",");
		sb.append(info.iValue);
		
		return sb.toString();
	}
	
	private static void sendOrder(){
		Object[] aObject = new Object[16];
		aObject[0] = "NFO";
		aObject[1] = "NIFTY";
		aObject[2] = "";
		aObject[3] = "48251";
		aObject[4] = "5.00";
		aObject[5] = "Buy";
		aObject[6] = "OPTIDX";
		aObject[7] = "29SEP2011";
        aObject[8] = "CE";

        aObject[9] = "5200.00";
        aObject[10] = "1001773800";
        aObject[11] = "50";
        aObject[12] = "NIFTY11SEP5200CE";
        aObject[13] = "NIFTY11SEP5000CE";
        aObject[14] = new Boolean(true);
        Short sObj = new Short((short)2);
        aObject[15] = sObj;
        
        put_Order(aObject);
	}
	
	public static String getExpiryDate(int i, String Exch_seg)
	  {
	    Calendar calendar = Calendar.getInstance();
	    calendar.clear();

	    if (Exch_seg.equals("ICEX"))
	    {
	      calendar.set(1980, 0, 0, 0, 0, 0);
	      calendar.add(13, i);
	    }
	    else
	    {
	      calendar.set(1980, 0, 0, 0, 0, 0);
	      calendar.add(13, i + 86400);
	    }

	    return calendar.get(5) + monthArray[calendar.get(2)] + calendar.get(1);
	  }

	public static String[] monthArray = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
	
	private static URLConnection getServletConnection() throws Exception
  {
    URLConnection con = null;
    try
    {
      URL urlServlet = null;

      URL ul = new URL("https://www.now-online.in/NOW/servlet/");

      urlServlet = new URL(ul, "NESTPlaceOrderIntermediet");

      con = urlServlet.openConnection();

      con.setDoInput(true);
      con.setDoOutput(true);
      con.setUseCaches(false);
      con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
    }
    catch (Exception e)
    {
      System.out.println("Exception in getServletConnection() : " + e);
    }
    return con;
  }
	
	private static void put_Order(Object[] order)
	  {
	    try
	    {
	    	
	      Vector oSubmit = new Vector();
	      oSubmit.add("NFO");
	      oSubmit.add(order[13]);
	      oSubmit.add("DS0097-13906");
	      oSubmit.add("S");
	      oSubmit.add("DAY");
	      oSubmit.add("50");
	      oSubmit.add("100.00");
	      oSubmit.add("Limit");
	      oSubmit.add("0");
	      oSubmit.add("0");
	      oSubmit.add("DS0097-13906");
	      oSubmit.add(new Boolean(false));
	      oSubmit.add("NRML");
	      
	      URLConnection con;
	      con = getServletConnection();
	      
	      OutputStream outstream = con.getOutputStream();
	      ObjectOutputStream oos = new ObjectOutputStream(outstream);
	      oos.writeObject(oSubmit);
	      oos.flush();
	      oos.close();

	      InputStream instr = con.getInputStream();
	      ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
	      String sResult = (String)inputFromServlet.readObject();
	      System.out.println(sResult);
	      inputFromServlet.close();
	      instr.close();
	      if (sResult.startsWith("API"))
	      {
	        System.out.println("Warning: " + sResult.toString());
	        
	      }
	      else if(true)
	      {
	        //JFrame frame = null;
	        //JOptionPane.showMessageDialog(frame, "      Order Placed at " + this.sExch + " with Order No : " + ()Double.parseDouble(sResult.toString()), "Order Placed", -1);

	        //requestFocus();
	    	  System.out.println("Order placed: " + sResult.toString());
	      }
	      else
	      {
	       //
	      }

	    }
	    catch (Exception ex)
	    {
	      System.out.println("Exception in put_Order() : " + ex.toString());
	      ex.printStackTrace();
	    }
	  }

}

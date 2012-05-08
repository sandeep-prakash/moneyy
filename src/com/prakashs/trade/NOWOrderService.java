package com.prakashs.trade;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import com.prakashs.Main;

/**
 * 
 * @author Sandeep Prakash
 * Implementation for the NSE NOW Platform
 * Reverse engineered from the NSE NOW Java Applet
 */
public class NOWOrderService implements OrderService, Runnable {

	@Override
	public void placeOrder(Order order) {
		
		Vector oSubmit = new Vector();
		oSubmit.add("NFO");
		oSubmit.add(order.getSecurityId());
		oSubmit.add(Main.PROPERTIES.get("mds.account.id").toString());
		String orderType = "B";
		if(order.getOrderType() == Order.SELL_ORDER) orderType = "S";
		oSubmit.add(orderType);
		oSubmit.add("DAY");
		oSubmit.add(""+order.getQuantity());
		oSubmit.add(""+order.getPrice());
		oSubmit.add("Limit");
		oSubmit.add("0");
		oSubmit.add("0");
		oSubmit.add(Main.PROPERTIES.get("mds.account.id").toString());
		oSubmit.add(new Boolean(true));
		oSubmit.add("NRML");
		
		try{
			putOrder(oSubmit);
		}catch(Exception ex){
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
	public void cancelOrder(Order order) {
		// TODO Auto-generated method stub

	}

	@Override
	public void modifyOrder(Order order) {
		// TODO Auto-generated method stub

	}
	
	private void putOrder(Vector oSubmit) throws Exception {
		URLConnection connection = getServletConnection();
		OutputStream os = connection.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(oSubmit);
		oos.flush();
		oos.close();
		
		InputStream instr = connection.getInputStream();
		ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
		String sResult = (String)inputFromServlet.readObject();
		inputFromServlet.close();
		instr.close();
		
		System.out.println("Result: " + sResult);
	}

	
	//
	// Helpers for this implementation
	//
	private URLConnection getServletConnection() throws MalformedURLException, IOException
/*      */   {
/* 1692 */     URLConnection con = null;
/*      */     try
/*      */     {
/* 1695 */       URL urlServlet = null;
/*      */ 
/* 1697 */       URL ul = new URL(Main.PROPERTIES.getProperty("mds.servletpath"));
/*      */ 
/* 1699 */       urlServlet = new URL(ul, "NESTPlaceOrderIntermediet");
/*      */ 
/* 1701 */       con = urlServlet.openConnection();
/*      */ 
/* 1703 */       con.setDoInput(true);
/* 1704 */       con.setDoOutput(true);
/* 1705 */       con.setUseCaches(false);
/* 1706 */       con.setRequestProperty(
/* 1707 */         "Content-Type", 
/* 1708 */         "application/x-java-serialized-object");
/*      */     }
/*      */     catch (Exception e)
/*      */     {
/* 1714 */       System.out.println("Exception in getServletConnection() : " + e);
/*      */     }
/* 1716 */     return con;
/*      */   }

	@Override
	public void run() {
		
		Order bOrder = Order.sellOrder("NIFTY12MAY5100CE", 60, 50);
		placeOrder(bOrder);
	}
}

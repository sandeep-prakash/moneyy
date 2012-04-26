package com.prakashs.strategy;

import org.apache.log4j.Logger;

import com.lmax.disruptor.EventHandler;
import com.omnesys.mw.classes.CStreamData;
import com.omnesys.mw.classes.CTouchLineInfo;

public class Box implements EventHandler<CStreamData> {
	
	private static Logger __logger = Logger.getLogger(Box.class.getName());
	
	private String k1CallKey;
	private String k2CallKey;
	private String k1PutKey;
	private String k2PutKey;
	
	private Quote k1Call;
	private Quote k2Call;
	private Quote k1Put;
	private Quote k2Put;
	
	
	private double k1, k2, minArb = 2.5;
	
	public Box(String k1CallKey, String k2CallKey, String k1PutKey, String k2PutKey, double k1, double k2){
		this.k1CallKey = k1CallKey;
		this.k2CallKey = k2CallKey;
		this.k1PutKey = k1PutKey;
		this.k2PutKey = k2PutKey;
		this.k1 = k1;
		this.k2 = k2;
	}

	@Override
	public void onEvent(CStreamData oData, long sequence, boolean endOfBatch)
			throws Exception {
		
		if(oData.iMsgCode == 1){
			CTouchLineInfo tick = ((CTouchLineInfo)oData.oStreamObj);
			
			if(tick.oScripNo.equalsIgnoreCase(k1CallKey)){
				if(k1Call == null)k1Call = new Quote();
				
				if(tick.iBuyRate > 0) k1Call.bid = tick.iBuyRate;
				if(tick.iSellRate > 0) k1Call.ask = tick.iSellRate;
			}
			
			if(tick.oScripNo.equalsIgnoreCase(k2CallKey)){
				if(k2Call == null)k2Call = new Quote();
				
				if(tick.iBuyRate > 0) k2Call.bid = tick.iBuyRate;
				if(tick.iSellRate > 0) k2Call.ask = tick.iSellRate;
			}
			
			if(tick.oScripNo.equalsIgnoreCase(k1PutKey)){
				if(k1Put == null)k1Put = new Quote();
				
				if(tick.iBuyRate > 0) k1Put.bid = tick.iBuyRate;
				if(tick.iSellRate > 0) k1Put.ask = tick.iSellRate;
			}
			
			if(tick.oScripNo.equalsIgnoreCase(k2PutKey)){
				if(k2Put == null)k2Put = new Quote();
				
				if(tick.iBuyRate > 0) k2Put.bid = tick.iBuyRate;
				if(tick.iSellRate > 0) k2Put.ask = tick.iSellRate;
			}
			
			if(k1Call != null && k2Call != null && k1Put != null && k2Put != null)
			{
				if(buyBox())
				{
					__logger.info("Found a buyBox arb opportunity.");
				}
				if(sellBox()){
					__logger.info("Found a sellBox arb opportunity.");
				}
			}
		}
		
	}

	private boolean buyBox(){
		
		return (k1Call.bid + k2Put.bid - k2Call.ask - k1Put.ask - minArb) > (k2-k1);
		
	}
	
	private boolean sellBox(){
		
		return Math.abs(k2Call.bid + k1Put.bid - k1Call.ask - k2Put.ask) - minArb > (k2 - k1);
		
	}
	
	class Quote{
		public double bid;
		public double ask;
	}
}

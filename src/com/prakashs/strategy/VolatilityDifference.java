package com.prakashs.strategy;


import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jquantlib.*;
import org.jquantlib.daycounters.*;
import org.jquantlib.exercise.*;
import org.jquantlib.instruments.*;
import org.jquantlib.processes.BlackScholesMertonProcess;

import org.jquantlib.quotes.*;
import org.jquantlib.termstructures.*;
import org.jquantlib.termstructures.volatilities.*;
import org.jquantlib.termstructures.yieldcurves.*;

import org.jquantlib.time.*;
import org.jquantlib.time.calendars.*;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.lmax.disruptor.EventHandler;
import com.omnesys.mw.classes.CSensexInfo;
import com.omnesys.mw.classes.CStreamData;
import com.omnesys.mw.classes.CTouchLineInfo;
import com.prakashs.Main;
import com.prakashs.stream.TickDataLoader;

// Manpreet: +234 7056001930

public class VolatilityDifference implements EventHandler<CStreamData> {
	
	private static Logger __logger = Logger.getLogger(VolatilityDifference.class.getName());
	
	private double nmOptionValue;
	private double fmOptionValue;
	private double underlying;
	
	private int strike = 5000;
	private double rate = 0.0924;
	
	private int nmTicks = 0;
	private int fmTicks = 0;
	
	private Date nmExDate = new Date(31,5,2012);
	private Date fmExDate = new Date(28,6,2012);
	
	private String nmKey="45118";
	private String fmKey="52649";
	
	private static Calendar __calendar = new India();
    private static Date __todaysDate = Date.todaysDate();
    //private static Date __todaysDate = new Date(23, 9, 2011);
    private static DayCounter dayCounter = new Actual365Fixed();
    
    private ArrayList<Double> initWindow = new ArrayList<Double>();
    
    private java.util.Date exchangeTime = null;
    private java.util.Date startTime = null;
    private int windowSize = 0; // Window size in milliseconds
    
    private int currentState = INITIAL;
    private final static int INITIAL = 0;
    private final static int WINDOW_EXPIRED = 1;
    private final static int SOLD_DIFF = 2;
    private final static int BOUGHT_DIFF = 3;
    
    private double volLL;
    private double volUL;
    private double paid;
    
    private Twitter twitter;
    
    public VolatilityDifference(String nmKey, String fmKey, int strike){
    	try{
    		this.nmKey = nmKey;
    		this.fmKey = fmKey;
    		this.strike = strike;
    		windowSize = Integer.parseInt(Main.PROPERTIES.getProperty("vds.windowSize"))*1000;
    		ConfigurationBuilder cb = new ConfigurationBuilder();
    		cb.setDebugEnabled(true)
    		  .setOAuthConsumerKey("qHzxaFrfzktB7DonlQXLzQ")
    		  .setOAuthConsumerSecret("WTcdE2vN9U3nl3t6mQkUbfF4PNNey2t2dp78AEajp8")
    		  .setOAuthAccessToken("351435596-KpFoMU2mdiu5OhdBi0BRPpdd7bQgkJFakkTV6Zh6")
    		  .setOAuthAccessTokenSecret("80OjYaYPmmKYG6DAUCSfstJEMPl2UnnLJunF8r0GGXQ");
    		TwitterFactory tf = new TwitterFactory(cb.build());
    		twitter = tf.getInstance();
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
	
	public VolatilityDifference(String nmKey, Date nmExDate, String fmKey, Date fmExDate, int strike){
    	this(nmKey, fmKey, strike);
		this.nmExDate = nmExDate;
		this.fmExDate = fmExDate;
    }

	@Override
	public void onEvent(CStreamData oData, long sequence, boolean endOfBatch)
			throws Exception {
		
		//__logger.info("Event received..." + nmKey);
		
		if(oData.iMsgCode == 1){
			CTouchLineInfo tick = ((CTouchLineInfo)oData.oStreamObj);
			
			//exchangeTime = new java.util.Date(tick.lFeedTime);
			
			// Instrument
			if(tick.oScripNo.equalsIgnoreCase(nmKey)){
				if(tick.iBuyRate > 0){
					if(currentState == SOLD_DIFF) nmOptionValue = tick.iSellRate;
					else nmOptionValue = tick.iBuyRate;
					nmTicks++;
				}
			}
			if(tick.oScripNo.equalsIgnoreCase(fmKey)){
				if(tick.iSellRate > 0){
					if(currentState == SOLD_DIFF) fmOptionValue = tick.iBuyRate;
					else fmOptionValue = tick.iSellRate;
					fmTicks++;
				}
			}
			
			if(!tick.oScripNo.equalsIgnoreCase(nmKey) && !tick.oScripNo.equalsIgnoreCase(fmKey)) return;
			
			if(nmOptionValue <= 0 || fmOptionValue <= 0 || underlying <= 0 || underlying >= 6000) return;
			
			__logger.info("Underlying at " + tick.lFeedTime + " is " + underlying);
			__logger.info(nmKey+".Values at " + exchangeTime + ",NM: " + nmOptionValue + ", FM: " + fmOptionValue);
			try{
				double diff = impliedVolatility("call", underlying, strike, __todaysDate, nmExDate, rate, 0, nmOptionValue)
							- impliedVolatility("call", underlying, strike, __todaysDate, fmExDate, rate, 0, fmOptionValue);
			
				initWindow.add(diff);
				
				__logger.info(nmKey+".Added diff " + diff + " at " + exchangeTime + " for NM: " + nmOptionValue + " and FM: " + fmOptionValue);
				
				if(currentState == INITIAL){
					if(exchangeTime.getTime() - startTime.getTime() > windowSize){
						
						double m = mean(initWindow);
						double sd = sd(initWindow);
						
						__logger.info(nmKey+".Window size used " + initWindow.size());
						__logger.info(nmKey+".Mean: " + m);
						__logger.info(nmKey+".SD: " + sd);
						
						double offset = 2.5*sd;
						
						if(Math.abs(m/sd) < 1) offset = 2.0*sd;
						if(Math.abs(m/sd) >= 1) offset = 2.5*sd;
						
						volLL = m - offset;
						volUL = m + offset;
						
						__logger.info(nmKey+".UL and LL calculated: " + volUL + ", " + volLL);
						__logger.info(nmKey+".Window size used " + initWindow.size());
						String message = String.format(nmKey+".UL: %1$s. LL: %2$s.", volUL, volLL);
						sendTwitterMessage(message);
						currentState = WINDOW_EXPIRED;
					}
				}
				
				if(currentState == WINDOW_EXPIRED){
					if(diff > volUL){
						// Buy
						paid = nmOptionValue - fmOptionValue;
						String message = String.format("Sell at %1$s. nmKey: %2$s at %3$s. fmKey: %4$s at %5$s", 
															exchangeTime, nmKey, nmOptionValue, fmKey, fmOptionValue);
						
						__logger.info(message);
						sendTwitterMessage(message);
						currentState = SOLD_DIFF;
					}
				}
				
				if(currentState == SOLD_DIFF){
					double gap = fmOptionValue - nmOptionValue;
					if(diff < volLL || (gap + paid) > 4){
						// Sell
						String message = String.format("Bought at %1$s. nmKey: %2$s at %3$s. fmKey: %4$s at %5$s", 
								exchangeTime, nmKey, nmOptionValue, fmKey, fmOptionValue);
						
						__logger.info(message);
						sendTwitterMessage(message);
						currentState = BOUGHT_DIFF;
					}
				}
			}catch(Exception ex){
				//__logger.error("Error while calculating IV for 
				ex.printStackTrace();
			}
		}
		
		else if(oData.iMsgCode == 2){
			CSensexInfo tick = ((CSensexInfo)oData.oStreamObj);
			// Index
			if(tick.iValue > 0) underlying = ((double)tick.iValue)/100.0;
		}
		
		else if(oData.iMsgCode == 4){
			exchangeTime = (java.util.Date)oData.oStreamObj;
			
			if(startTime == null) startTime = exchangeTime;
		}
		
		else{
			//--		
		}
	}

	public static double impliedVolatility(String type_, double underlying, double strike, Date todaysDate, Date exDate, double rate, double yield, double price){
        Option.Type type = Option.Type.Put;
        if(type_.equalsIgnoreCase("call")) type = Option.Type.Call;
        
        new Settings().setEvaluationDate(todaysDate);
        
        Exercise europeanExercise = new EuropeanExercise(exDate);
        Handle<Quote> underlyingH = new Handle<Quote>(new SimpleQuote(underlying));
        Handle<YieldTermStructure> flatDividendTS = new Handle<YieldTermStructure>(new FlatForward(todaysDate, yield, dayCounter));
        Handle<YieldTermStructure> flatTermStructure = new Handle<YieldTermStructure>(new FlatForward(todaysDate, rate, dayCounter));
        double volGuess = 0;
        Handle<BlackVolTermStructure> flatVolTS = new Handle<BlackVolTermStructure>(new BlackConstantVol(todaysDate, __calendar, volGuess, dayCounter));
        BlackScholesMertonProcess stochasticProcess = new BlackScholesMertonProcess(underlyingH, flatDividendTS, flatTermStructure, flatVolTS);
        
        Payoff payoff = new PlainVanillaPayoff(type, strike);
        VanillaOption europeanOption = new EuropeanOption(payoff, europeanExercise);
        
        double iv = 0;
        try{
        	iv = europeanOption.impliedVolatility(price, stochasticProcess);
        }catch(Exception ex){
        	__logger.error("Encountered error while calculating for " + strike + ", "+ underlying +", " + todaysDate + ", " + exDate + ", " + price);
        }
        if(iv == 0){
        	try{
        		iv = europeanOption.impliedVolatility(price, stochasticProcess, 0.001);
        	}catch(Exception ex){
        		__logger.error("Failed even at 0.001 calculating for " + strike + ", "+ underlying +", " + todaysDate + ", " + exDate + ", " + price);
        	}
        }
        if(iv == 0){
        	try{
        		iv = europeanOption.impliedVolatility(price, stochasticProcess, 0.01);
        	}catch(Exception ex){
        		__logger.error("Failed even at 0.01 calculating for " + strike + ", "+ underlying +", " + todaysDate + ", " + exDate + ", " + price);
        	}
        }
        
        return iv;
    }
	
	private double mean(ArrayList<Double> data){
		double m = 0;
		for(double d : data){
			m += d;
		}
		
		return m/data.size();
	}
	
	private double sd(ArrayList<Double> data){
		double m = mean(data);
		double sum = 0;
		for(double d: data){
			sum += ((d-m)*(d-m));
		}
		
		return Math.sqrt(sum/data.size());
	}
	
	private void sendTwitterMessage(String message){
		try{
			twitter.updateStatus(message);
		}catch(Exception ex){
			__logger.error("Error while sending twitter message.", ex);
		}
	}
}

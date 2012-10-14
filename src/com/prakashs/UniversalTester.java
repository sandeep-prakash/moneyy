package com.prakashs;

import org.jquantlib.time.Date;

import com.prakashs.strategy.VolatilityDifference;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

//
// This class will be house a lot of temporary code.
//
public class UniversalTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		// Twitter test
		//testTwitterAPI();
		testJQuantLib();

	}
	//6355366
	private static void testTwitterAPI() throws Exception {
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("qHzxaFrfzktB7DonlQXLzQ")
		  .setOAuthConsumerSecret("WTcdE2vN9U3nl3t6mQkUbfF4PNNey2t2dp78AEajp8")
		  .setOAuthAccessToken("351435596-KpFoMU2mdiu5OhdBi0BRPpdd7bQgkJFakkTV6Zh6")
		  .setOAuthAccessTokenSecret("80OjYaYPmmKYG6DAUCSfstJEMPl2UnnLJunF8r0GGXQ");
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		//DirectMessage msg = twitter.sendDirectMessage("prakash_sandeep", "test direct message1.");
		Status status = twitter.updateStatus("This is a test message13.");
		//System.out.println(msg + " published a message.");
	}
	private static void testJQuantLib() throws Exception{
		System.out.println(VolatilityDifference.impliedVolatility("call", 4887.45, 4900, Date.todaysDate(), new Date(27,10,2011), 0.0925, 0, 124.2));
		System.out.println(VolatilityDifference.impliedVolatility("call", 4887.45, 4900, Date.todaysDate(), new Date(24,11,2011), 0.0925, 0, 213.9));
	}
	
	private static void testPlaceOrder() throws Exception {
		
	}
}

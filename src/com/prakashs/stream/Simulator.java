package com.prakashs.stream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.omnesys.mw.classes.CStreamData;
import com.omnesys.mw.classes.CTouchLineInfo;
import com.prakashs.Main;
import com.prakashs.strategy.VolatilityDifference;

public class Simulator implements Runnable{
	
	
	
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

	@Override
	public void run() {
		
		// Setup the ring buffer
		Disruptor<CStreamData> disruptor =
				  new Disruptor<CStreamData>(EVENT_FACTORY, 1024, Executors.newCachedThreadPool(), 
				                                       com.lmax.disruptor.ClaimStrategy.Option.SINGLE_THREADED,
				                                       com.lmax.disruptor.WaitStrategy.Option.YIELDING);
		disruptor.handleEventsWith(new VolatilityDifference("","",0));
		RingBuffer<CStreamData> ringBuffer = disruptor.start();
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(Main.arguments[2]));
			String line = null;
			
			while((line = reader.readLine()) != null){
				CTouchLineInfo info = MarketDataService.stringToCTouchLineInfo(line);
				long sequence = ringBuffer.next();
				CStreamData event = ringBuffer.get(sequence);
				event.iMsgCode = 1;
				event.oStreamObj = info;

				// make the event available to EventProcessors
				ringBuffer.publish(sequence);   
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

}

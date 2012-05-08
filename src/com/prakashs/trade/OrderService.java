package com.prakashs.trade;

public interface OrderService {
	
	// Send order
	public void placeOrder(Order order);
	
	// Cancel order
	public void cancelOrder(Order order);
	
	// Modify order
	public void modifyOrder(Order order);

}

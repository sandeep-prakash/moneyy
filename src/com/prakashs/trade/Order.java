package com.prakashs.trade;

public class Order {
	
	public static int BUY_ORDER = 0;
	public static int SELL_ORDER = 1;
	
	private String securityId;
	private int orderType;
	private double price;
	private int quantity;
	
	private Order(){
		
	}
	
	public static Order buyOrder(String securityId, double price, int quantity){
		
		Order order = new Order();
		order.orderType = BUY_ORDER;
		order.securityId = securityId;
		order.price = price;
		order.quantity = quantity;
		
		return order;
	}
	
	public static Order sellOrder(String securityId, double price, int quantity){
		
		Order order = new Order();
		order.orderType = SELL_ORDER;
		order.securityId = securityId;
		order.price = price;
		order.quantity = quantity;
		
		return order;
	}

	public String getSecurityId() {
		return securityId;
	}

	public int getOrderType() {
		return orderType;
	}

	public double getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}
	
	
	
}

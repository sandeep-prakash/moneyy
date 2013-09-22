show_sym_map <- function (myconn){
	sym_map.local <- sqlQuery(myconn, "SELECT * FROM SYMBOL_MAP")
	sym_map.local
}

get_db_connection <- function(){
	library(RODBC)
	myconn <- odbcConnect("tick_data")
	return (myconn)
}

find_symbol <- function (myconn, symbol){
	sym_map.local <- sqlQuery(myconn, paste("SELECT * FROM SYMBOL_MAP WHERE INSTRUMENT_NAME = '", symbol, "'", sep=""))
	sym_map.local
}

get_tick_data <- function(myconn, inst_a, inst_b, date){
	sqlStr <- paste("SELECT X.TICK_TIME, 'INST_A_BID' = X.BID_PRICE, 'INST_A_ASK' = X.ASK_PRICE, 'INST_B_BID' = Y.BID_PRICE, 'INST_B_ASK' = Y.ASK_PRICE FROM INSTRUMENT_TICK_DATA X JOIN INSTRUMENT_TICK_DATA Y ON X.FILE_DATE = Y.FILE_DATE AND X.TICK_TIME = Y.TICK_TIME WHERE X.INSTRUMENT_ID = '", inst_a, "' AND Y.INSTRUMENT_ID = '", inst_b, "'", " AND X.FILE_DATE = '", date, "' ORDER BY X.TICK_TIME", sep="")
	print(sqlStr)
	return (sqlQuery(myconn, sqlStr))
}

run_stat_arb <- function(tick_data, upper_limit, lower_limit, inst_a_type="ls", inst_b_type="l"){
	# inst_a_type = "ls" => strategy can go both long and short on instrument A. "l" => long only.
	# if inst_a_bid - inst_b_ask > upper_limit & inst_a_type = "ls" => sell inst_a & buy inst_b
	# if inst_a_ask - inst_b_bid < lower_limit & (inst_b_type = "ls" or holdings[inst_b] > 0) => sell inst_b & buy inst_a
	inst_a_holding <- 0
	inst_b_holding <- 0
	cash <- 0
	tick <- 1
	txns <- 0
	state <- "START" # state . START -> INST_A_SHORT -> INST_A_LONG. START -> INST_A_LONG -> INST_A_SHORT
	num_ticks <- length(tick_data$TICK_TIME)
	while(tick <= num_ticks){
	
		if(tick_data$TICK_TIME[tick] >= 21900) break
		
		# Test A
		if(tick_data$INST_A_BID[tick] - tick_data$INST_B_ASK[tick] > upper_limit){
			if(state == "START" || state == "INST_A_LONG"){
				inst_a_holding <- inst_a_holding - 1
				inst_b_holding <- inst_b_holding + 1
				print(paste("Going short at", tick, ". Cash Before:", cash))
				cash <- cash + tick_data$INST_A_BID[tick] - tick_data$INST_B_ASK[tick]
				print(paste("Going short. Cash After:", cash))
				state <- "INST_A_SHORT"
				txns <- txns + 1
			}
		}
		
		# Test B
		if(tick_data$INST_A_ASK[tick] - tick_data$INST_B_BID[tick] < lower_limit){
			if(state == "INST_A_SHORT" || (state == "START" && inst_b_type == "ls")){
				inst_a_holding <- inst_a_holding + 1
				inst_b_holding <- inst_b_holding - 1
				print(paste("Going long at", tick, ". Cash Before:", cash))
				cash <- cash - tick_data$INST_A_ASK[tick] + tick_data$INST_B_BID[tick]
				print(paste("Going Long. Cash After:", cash))
				state <- "INST_A_LONG"
				txns <- txns + 1
			}
		}
		
		tick <- tick + 1
		
		# SET STATE
		if(inst_a_holding == 0 && inst_b_holding == 0)state <- "START"
	}
	
	# Square off
	if(inst_a_holding != 0){
		if(state == "INST_A_SHORT"){
			inst_a_holding <- inst_a_holding + 1
			inst_b_holding <- inst_b_holding - 1
			print(paste("Going Long. Cash Before:", cash))
			cash <- cash - tick_data$INST_A_ASK[tick] + tick_data$INST_B_BID[tick]
			print(paste("Going Long. Cash After:", cash))
			txns <- txns + 1
		}
		if(state == "INST_A_LONG"){
			inst_a_holding <- inst_a_holding - 1
			inst_b_holding <- inst_b_holding + 1
			print(paste("Going short. Cash Before:", cash))
			cash <- cash + tick_data$INST_A_BID[tick] - tick_data$INST_B_ASK[tick]
			print(paste("Going short. Cash After:", cash))
			state <- "INST_A_SHORT"
			txns <- txns + 1
		}
	}
	
	print(paste("Cash:", cash, ".\nInst A Holding:", inst_a_holding, ".\nInst B Holding ", inst_b_holding, ".\nTransactions:", txns))
}
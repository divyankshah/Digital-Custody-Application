package com.example.digital_custody_coding_challenge;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitorController {
	  private final EthereumService ethereumService;
	  //adress of vitalik buterin founder of ETH
	  String binanceHotWallet = "0x28C6c06298d514Db089934071355E5743bf21d60";
	    public MonitorController(EthereumService ethereumService) {
	        this.ethereumService = ethereumService;
	    }
	    //For checking if ETH is connected to our local
	    @GetMapping("/status")
	    public String status() throws Exception {
	        return "Connected to: " + ethereumService.getClientVersion();
	    }
	  //Running any ETH address and checking balance in USD
	    @GetMapping("/address/{ethAddress}/balance")
	    public Map<String, String> getBalance(@PathVariable("ethAddress") String ethAddress) throws Exception {
	        BigDecimal ethBalance = ethereumService.getEthBalance(ethAddress);
	        BigDecimal ethPrice = ethereumService.getEthPriceUSD();
	        BigDecimal usdValue = ethBalance.multiply(ethPrice);

	        Map<String, String> response = new HashMap<>();
	        response.put("ethBalance", ethBalance.toPlainString() + " ETH");
	        response.put("usdEquivalent", usdValue.toPlainString() + " USD");
	        return response;
	    }
	        
	    @GetMapping("/transactions")
	    public List<Map<String, String>> getVitalikTxs() throws Exception {
	        long startBlock = 17900000;
	        long endBlock = 17901000;  
	        int chunkSize = 500;
	        return ethereumService.getTransactionsInChunks(binanceHotWallet, startBlock, endBlock,chunkSize);
	    }
	    
	    @GetMapping("/binance/swaps")
	    public List<Map<String, String>> getVitalikSwaps() throws Exception {
	        return ethereumService.getSwapTransactions(17408230L, 17408230L);
	    }

	    @GetMapping("/binance/usd-volume")
	    public Map<String, String> getVitalikUsdVolume() throws Exception {
	        BigDecimal totalUsd = ethereumService.getTotalUSDVolume(17900000, 17900500);
	        return Map.of("total_usd_volume", totalUsd.toPlainString());
	    }

}

package com.example.digital_custody_coding_challenge;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EthereumService {
	private final Web3j web3j;

	public EthereumService() {
		String infuraUrl = "https://mainnet.infura.io/v3/dbebfaa12da24b04b1063ad65baf6d1c";
		this.web3j = Web3j.build(new HttpService(infuraUrl));
	}
	
	//count swap from transactions history
	public List<Map<String, String>> getSwapTransactions(long startBlock, long endBlock) throws Exception {
	   
	    String binanceHotWallet = "0x28c6c06298d514db089934071355e5743bf21d60";
	    int swapCount = 0;
	    Set<String> swapFunctionSignatures = Set.of(
	        "0x7ff36ab5", // swapExactETHForTokens
	        "0x18cbafe5", // swapExactTokensForETH
	        "0x38ed1739", // swapExactTokensForTokens
	        "0x5c11d795", // swapTokensForExactETH
	        "0x4a25d94a", // swapTokensForExactTokens
	        "0xfb3bdb41"  // swapETHForExactTokens
	    );

	    List<Map<String, String>> swaps = new ArrayList<>();

	    for (long blockNum = startBlock; blockNum <= endBlock; blockNum++) {
	        EthBlock block = web3j.ethGetBlockByNumber(
	                DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNum)), true
	        ).send();

	        if (block == null || block.getBlock() == null) {
	            System.out.println("Skipped null block: " + blockNum);
	            continue;
	        }

	        for (EthBlock.TransactionResult<?> txResult : block.getBlock().getTransactions()) {
	            EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();

	            String to = tx.getTo() != null ? tx.getTo().toLowerCase() : "";
	            String input = tx.getInput() != null ? tx.getInput() : "";
	            String functionSig = input.length() >= 10 ? input.substring(0, 10) : "";

	            boolean isFromBinance = binanceHotWallet.equals(tx.getFrom().toLowerCase());
	            boolean isSwapFunction = swapFunctionSignatures.contains(functionSig);

	            // DEBUG LOGGING
	            System.out.println("Tx Hash: " + tx.getHash());
	            System.out.println("From: " + tx.getFrom());
	            System.out.println("To: " + to);
	            System.out.println("FunctionSig: " + functionSig);
	            System.out.println("isFromBinance: " + isFromBinance);
	            System.out.println("isSwapFunction: " + isSwapFunction);
	            System.out.println("-------------");

	            if (isSwapFunction) {    //Use if if (isFromBinance&&isSwapFunction) to detect if it is from isFromBinance
	                Map<String, String> swap = new HashMap<>();
	                swap.put("hash", tx.getHash());
	                swap.put("from", tx.getFrom());
	                swap.put("to", to);
	                swap.put("function_signature", functionSig);
	                swap.put("value_eth", Convert.fromWei(tx.getValue().toString(), Convert.Unit.ETHER).toPlainString());
	                swap.put("block", String.valueOf(blockNum));
	                swapCount++;
	                swaps.add(swap);
	            }
	        }

	        Thread.sleep(200); // Prevent rate limiting
	    }
	    System.out.println("total swap count " + swapCount);
	    return swaps;
	}
	//Get ETH transaction history
	public List<Map<String, String>> getTransactionsInChunks(String address, long startBlock, long endBlock, int chunkSize) throws Exception {
	    List<Map<String, String>> allTxs = new ArrayList<>();

	    for (long chunkStart = startBlock; chunkStart <= endBlock; chunkStart += chunkSize) {
	        long chunkEnd = Math.min(chunkStart + chunkSize - 1, endBlock);

	        System.out.println("Scanning blocks " + chunkStart + " to " + chunkEnd);

	        for (long i = chunkStart; i <= chunkEnd; i++) {
	            EthBlock block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(i)), true).send();

	            for (EthBlock.TransactionResult<?> txResult : block.getBlock().getTransactions()) {
	                EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();

	                if (address.equalsIgnoreCase(tx.getFrom()) || address.equalsIgnoreCase(tx.getTo())) {
	                    Map<String, String> txInfo = new HashMap<>();
	                    txInfo.put("block", String.valueOf(i));
	                    txInfo.put("hash", tx.getHash());
	                    txInfo.put("from", tx.getFrom());
	                    txInfo.put("to", tx.getTo());
	                    txInfo.put("value_eth", Convert.fromWei(tx.getValue().toString(), Convert.Unit.ETHER).toPlainString());
	                    txInfo.put("timestamp", block.getBlock().getTimestamp().toString());

	                    allTxs.add(txInfo);
	                }
	            }

	          
	        }
	    }

	    return allTxs;
	}	
	
	
	public BigDecimal getEthPriceUSD() throws Exception {
	    OkHttpClient client = new OkHttpClient();
	    Request request = new Request.Builder()
	        .url("https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd")
	        .build();

	    Response response = client.newCall(request).execute();
	    String jsonData = response.body().string();

	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode node = mapper.readTree(jsonData);
	    return node.get("ethereum").get("usd").decimalValue();
	}
	public BigDecimal getTotalUSDVolume(long startBlock, long endBlock) throws Exception {
		String binanceHotWallet = "0x28C6c06298d514Db089934071355E5743bf21d60".toLowerCase();
	    BigDecimal ethPrice = getEthPriceUSD();
	    BigDecimal totalVolume = BigDecimal.ZERO;

	    for (long blockNum = startBlock; blockNum <= endBlock; blockNum++) {
	        EthBlock block = web3j.ethGetBlockByNumber(
	                DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNum)), true
	        ).send();

	        for (EthBlock.TransactionResult<?> txResult : block.getBlock().getTransactions()) {
	            EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();
	            String from = tx.getFrom().toLowerCase();
	            String to = tx.getTo() != null ? tx.getTo().toLowerCase() : "";

	            if (binanceHotWallet.equals(from) || binanceHotWallet.equals(to)) {
	                BigDecimal ethValue = Convert.fromWei(tx.getValue().toString(), Convert.Unit.ETHER);
	                totalVolume = totalVolume.add(ethValue.multiply(ethPrice));
	            }
	        }

	       Thread.sleep(300);  // Respect Infura rate limits
	    }

	    return totalVolume;
	}

	public BigDecimal getEthBalance(String address) throws Exception {
		BigInteger wei = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();

		return Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER);
	}

	public String getClientVersion() throws Exception {
		Web3ClientVersion version = web3j.web3ClientVersion().send();
		return version.getWeb3ClientVersion();
	}
}

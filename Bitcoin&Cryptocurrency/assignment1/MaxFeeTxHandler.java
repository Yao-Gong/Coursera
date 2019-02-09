package assignment1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MaxFeeTxHandler {

	private UTXOPool currentPool;
	private HashMap<UTXO, ArrayList<Transaction>> recorder;
	private HashMap<Transaction, Double> txMap;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public MaxFeeTxHandler(UTXOPool utxoPool) {
		
		currentPool = new UTXOPool(utxoPool);
		recorder = new HashMap<>();
		txMap = new HashMap<>();
	}

	private boolean isValidTx(Transaction tx) {
		// Inputs: current, signature, claimed, sum
		double inSum = 0.0;
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		HashSet<UTXO> claimed = new HashSet<UTXO>();
		
		for(int idx = 0; idx < inputs.size(); idx++)
		{
			Transaction.Input in = inputs.get(idx);
			UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			
			if(!currentPool.contains(prevUTXO))
				return false;
			if(claimed.contains(prevUTXO))
				return false;
			Transaction.Output prevOut = currentPool.getTxOutput(prevUTXO);
			if(!Crypto.verifySignature(prevOut.address, tx.getRawDataToSign(idx), in.signature))
				return false;
			
			inSum += prevOut.value;
			claimed.add(prevUTXO);  // prevent double claimed
		}
		
		// Outputs: value. sum
		double outSum = 0.0;
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		
		for(Transaction.Output out : outputs)
		{
			if(out.value < 0)
				return false;
			outSum += out.value;
		}
		
		if(inSum < outSum)
			return false;
		
		txMap.put(tx, (inSum - outSum));
		markUTXO(claimed, tx);
		return true;
	}


	private void markUTXO(HashSet<UTXO> claimed, Transaction tx) {

		for(UTXO utxo : claimed)
		{
			if(recorder.containsKey(utxo))
				recorder.get(utxo).add(tx);
			else
			{
				ArrayList<Transaction> txs = new ArrayList<>();
				txs.add(tx);
				recorder.put(utxo, txs);
			}
		}
		
	}

	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		
		txMap.clear();
		recorder.clear();
		
		ArrayList<Transaction> txs = new ArrayList<>();
		// verify
		for(Transaction tx : possibleTxs)
		{
			if(!isValidTx(tx))
				continue;
			else
			{
				txs.add(tx);
			}
		}
		
		// remove transactions with collision
		Set<Transaction> dirtyTx = cleanRcd();
		txs.removeAll(dirtyTx);

		// analyze dirty transactions
		ArrayList<Transaction> outcome = analyzeTxs();
		
		// update transactions	
		txs.addAll(outcome);
		updateTxs(txs);
		
		return txs.toArray(new Transaction[txs.size()]);
	}

	private ArrayList<Transaction> analyzeTxs() {
		// TODO Auto-generated method stub
		
		return null;
	}

	private void updateTxs(ArrayList<Transaction> txs) {
		
		for(Transaction tx : txs)
		{
			ArrayList<Transaction.Input> inputs = tx.getInputs();
			for(Transaction.Input in : inputs)
			{
				UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
				currentPool.removeUTXO(prevUTXO);
			}
			
			ArrayList<Transaction.Output> outputs = tx.getOutputs();
			for(int idx = 0; idx < outputs.size(); idx++)
			{
				UTXO current = new UTXO(tx.getHash(), idx);
				currentPool.addUTXO(current, outputs.get(idx));
			}
		}
	}

	private Set<Transaction> cleanRcd() {
		
		Set<Transaction> dirty = new HashSet<>();
		for(UTXO utxo : recorder.keySet())
		{
			ArrayList<Transaction> tmp = recorder.get(utxo);
			if(tmp.size()>1)
			{
				dirty.addAll(tmp);
			}
			else
			{
				txMap.remove(tmp.get(0));
				recorder.remove(utxo);
			}
		}
		
		return dirty;
	}
}

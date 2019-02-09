package assignment1;

import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {
	
	private UTXOPool currentPool;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		
		currentPool = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if: (1) all outputs claimed by {@code tx} are in the current
	 *         UTXO pool, (2) the signatures on each input of {@code tx} are
	 *         valid, (3) no UTXO is claimed multiple times by {@code tx}, (4)
	 *         all of {@code tx}s output values are non-negative, and (5) the
	 *         sum of {@code tx}s input values is greater than or equal to the
	 *         sum of its output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
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
		
		return true;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness, returning a
	 * mutually valid array of accepted transactions, and updating the current
	 * UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		
		ArrayList<Transaction> list = new ArrayList<>();
		for(Transaction tx : possibleTxs)
		{
			// verify
			if(!isValidTx(tx))
				continue;
			
			// accept
			list.add(tx);
			
			// update
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
		
		return list.toArray(new Transaction[list.size()]);
	}

}

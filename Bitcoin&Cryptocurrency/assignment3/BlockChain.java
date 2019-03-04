package assignment3;

import java.util.ArrayList;
import java.util.HashMap;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
	public static final int CUT_OFF_AGE = 10;
	private HashMap<Integer, ArrayList<Block>> blockTree;
	private HashMap<ByteArrayWrapper, Integer> blockMap;
	private TransactionPool txPool;
	private UTXOPool utxoPool;
	private int minH, maxH;

	/**
	 * create an empty block chain with just a genesis block. Assume
	 * {@code genesisBlock} is a valid block
	 */
	public BlockChain(Block genesisBlock) {
		// initialize members
		blockTree = new HashMap<>();
		blockMap = new HashMap<>();
		txPool = new TransactionPool();
		utxoPool = new UTXOPool();
		minH = maxH = 1;

		// process genesis block
		ArrayList<Block> genList = new ArrayList<>();
		genList.add(genesisBlock);
		blockTree.put(1, genList);
		blockMap.put(new ByteArrayWrapper(genesisBlock.getHash()), 1);
		ArrayList<Transaction> txs = new ArrayList<>();
		txs.add(genesisBlock.getCoinbase());
		txs.addAll(genesisBlock.getTransactions());
		addUTXOPool(txs);
	}

	/** Get the maximum height block */
	public Block getMaxHeightBlock() {
		return blockTree.get(maxH).get(0);
	}

	/** Get the UTXOPool for mining a new block on top of max height block */
	public UTXOPool getMaxHeightUTXOPool() {
		return utxoPool;
	}

	/** Get the transaction pool to mine a new block */
	public TransactionPool getTransactionPool() {
		return txPool;
	}

	/**
	 * Add {@code block} to the block chain if it is valid. For validity, all
	 * transactions should be valid and block should be at
	 * {@code height > (maxHeight - CUT_OFF_AGE)}.
	 * 
	 * <p>
	 * For example, you can try creating a new block over the genesis block
	 * (block height 2) if the block chain height is {@code <=
	 * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot
	 * create a new block at height 2.
	 * 
	 * @return true if block is successfully added
	 */
	public boolean addBlock(Block block) {
		// IMPLEMENT THIS
		// validate block
		if (block.getPrevBlockHash() == null)
			return false;

		ByteArrayWrapper preHash = new ByteArrayWrapper(block.getPrevBlockHash());
		Integer preGen = blockMap.get(preHash);
		if (preGen == null)
			return false;

		TxHandler txHandler = new TxHandler(utxoPool);
		ArrayList<Transaction> txs = block.getTransactions();
		for (Transaction tx : txs) {
			if (!txHandler.isValidTx(tx))
				return false;
		}
		// receive: update chain, txPool(remove), utxoPool(add)
		receiveBlock(block, preGen + 1);
		return true;
	}

	private void receiveBlock(Block block, int genNum) {
		// update chain
		ByteArrayWrapper curHash = new ByteArrayWrapper(block.getHash());
		if (blockMap.containsKey(curHash))
			return;

		if (genNum > maxH) {
			blockMap.put(curHash, genNum);
			ArrayList<Block> genList = new ArrayList<>();
			genList.add(block);
			blockTree.put(genNum, genList);
			maxH = genNum;
		} else {
			blockMap.put(curHash, genNum);
			blockTree.get(genNum).add(block);
		}

		if (maxH - minH > CUT_OFF_AGE) {
			removeGen(minH);
			minH++;
		}

		ArrayList<Transaction> txs = new ArrayList<>();
		txs.add(block.getCoinbase());
		txs.addAll(block.getTransactions());
		// update txPool(remove)
		removeTxPool(txs);
		// update utxoPool(add)
		addUTXOPool(txs);
	}

	private void removeTxPool(ArrayList<Transaction> txs) {
		// Auto-generated method stub
		for (Transaction tx : txs)
			txPool.removeTransaction(tx.getHash());
	}

	private void removeGen(int height) {
		// remove the generation of minimum height
		ArrayList<Block> cut = blockTree.get(height);
		for (Block block : cut) {
			blockMap.remove(new ByteArrayWrapper(block.getHash()));
		}
		blockTree.remove(height);
	}

	private void addUTXOPool(ArrayList<Transaction> txs) {
		for (Transaction tx : txs) {
			ArrayList<Transaction.Output> outputs = tx.getOutputs();

			for (int idx = 0; idx < outputs.size(); idx++) {
				UTXO current = new UTXO(tx.getHash(), idx);
				utxoPool.addUTXO(current, outputs.get(idx));
			}
		}
	}

	/** Add a transaction to the transaction pool */
	public void addTransaction(Transaction tx) {
		// receive: update txPool(add)
		txPool.addTransaction(tx);
	}

}
package assignment2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
	private int flagRd, currentRd;
	// private double rdFactor;
	private boolean[] followees;
	private HashSet<Transaction> txs;
	private HashMap<Integer, Set<Transaction>> record;

	public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
		this.flagRd = (int) (numRounds * 0.8);
		// this.rdFactor = p_graph * (1.0 - p_malicious) * p_txDistribution;

		currentRd = 0;
		txs = new HashSet<>();
		record = new HashMap<>();
	}

	public void setFollowees(boolean[] followees) {
		this.followees = followees;

		// rdFactor = 1.0 / (rdFactor * (followees.length - 1));
		// if ((int) rdFactor > 1)
		// flagRounds -= (int) rdFactor;
		//
		// flagRounds--;
	}

	public void setPendingTransaction(Set<Transaction> pendingTransactions) {
		txs.addAll(pendingTransactions);
	}

	public Set<Transaction> sendToFollowers() {
		// every round just calls this method once as the beginning procedure
		currentRd++;

		return txs;
	}

	public void receiveFromFollowees(Set<Candidate> candidates) {
		// every round just calls this method once as the ending procedure
		HashMap<Integer, Set<Transaction>> current = new HashMap<>();
		for (Candidate cd : candidates) {
			if (current.containsKey(cd.sender))
				current.get(cd.sender).add(cd.tx);
			else {
				HashSet<Transaction> temp = new HashSet<>();
				temp.add(cd.tx);
				current.put(cd.sender, temp);
			}

			if (!txs.contains(cd.tx))
				txs.add(cd.tx);
		}

		if (currentRd > flagRd)
			receiveStrictly(current);
		else
			receive(current);
	}

	private void receive(HashMap<Integer, Set<Transaction>> current) {
		// ignore nodes which shrink
		for (int i : current.keySet()) {
			if (record.containsKey(i)) {
				if (current.get(i).size() >= record.get(i).size())
					record.get(i).addAll(current.get(i));
				else
					followees[i] = false;
			} else
				record.put(i, current.get(i));
		}
	}

	private void receiveStrictly(HashMap<Integer, Set<Transaction>> current) {
		// ignore nodes which don't update
		for (int i = 0; i < followees.length; i++) {
			if (followees[i]) {
				if (record.containsKey(i)) {
					if (current.containsKey(i) && current.get(i).size() >= record.get(i).size()) {
						record.get(i).addAll(current.get(i));
					} else {
						followees[i] = false;
					}
				} else if (current.containsKey(i))
					record.put(i, current.get(i));
			}
		}
	}

}

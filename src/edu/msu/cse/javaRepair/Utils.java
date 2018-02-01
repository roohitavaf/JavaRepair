package edu.msu.cse.javaRepair;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import com.sun.prism.impl.BaseMesh.FaceMembers;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.BDD.BDDIterator;
/**
 * This class provide some utility functions used by the main Repairer class. 
 * @author roohitavaf
 *
 */
public class Utils {
	/**
	 * Remove all primed variables from the given BDD. 
	 * @param bdd The given BDD. 
	 * @return The resulted BDD.
	 */
	public static BDD removeNextState(BDD bdd) {
		BDDFactory factory = bdd.getFactory();
		BDD result = factory.one().and(bdd);
		for (int i = 0; i < factory.numberOfDomains() / 2; i++) {
			result = result.exist(factory.getDomain(getPrimeVInx(i)).set());
		}
		return result;
	}

	/**
	 * Make all unprimed variables primed. 
	 * @param bdd The given BDD
	 * @return The resulted BDD 
	 */
	public static BDD prime(BDD bdd) {
		BDDFactory factory = bdd.getFactory();
		BDD result = factory.one().and(bdd);

		for (int i = 0; i < factory.numberOfDomains() / 2; i++) {
			BDDPairing pair = factory.makePair(factory.getDomain(getVInx(i)), factory.getDomain(getPrimeVInx(i)));
			result = result.replace(pair);
		}
		return result;
	}

	/**
	 * Make all primed variables unprimed. 
	 * @param bdd The given BDD
	 * @return The resulted BDD
	 */
	public static BDD unprime(BDD bdd) {
		BDDFactory factory = bdd.getFactory();
		BDD result = factory.one().and(bdd);
		for (int i = 0; i < factory.numberOfDomains() / 2; i++) {
			result = result.exist(factory.getDomain(getVInx(i)).set());
		}

		// replacing prime variables with unprimed variables
		for (int i = 0; i < factory.numberOfDomains() / 2; i++) {
			BDDPairing pair = factory.makePair(factory.getDomain(getPrimeVInx(i)), factory.getDomain(getVInx(i)));
			result = result.replace(pair);
		}
		return result;
	}

	/**
	 * Print the given BDD.
	 * @param bdd
	 */
	public static void printBDD(BDD bdd) {
		BDDFactory factory = bdd.getFactory();
		int domainNum = factory.numberOfDomains();

		BDD var = factory.one();

		for (int i = 0; i < domainNum; i++) {
			var = var.and(factory.getDomain(getVInx(i)).set());
		}
		BDDIterator bddIteartor = bdd.iterator(var);
		while (bddIteartor.hasNext()) {
			BDD nextBDD = (BDD) bddIteartor.next();
			for (int i = 0; i < domainNum; i++) {
				BigInteger value = nextBDD.scanVar(factory.getDomain(getVInx(i)));
				System.out.print(value);
			}
			System.out.println();
		}
	}

	/**
	 * Give the index of the unprimed variable in the BDD. 
	 * @param index
	 * @return
	 */
	public static int getVInx(int index) {
		return index * 2;
	}

	/**
	 * Give the index of the primed variable in the BDD.
	 * @param index
	 * @return
	 */
	public static int getPrimeVInx(int index) {
		return getVInx(index) + 1;
	}

	/**
	 * Return a BDD that requires two given bits to be equal.
	 * @param factory
	 * @param firstBit
	 * @param secondBit
	 * @return The resulted BDD. 
	 */
	public static BDD equalBits(BDDFactory factory, int firstBit, int secondBit) {
		BDD bdd1 = factory.ithVar(firstBit).and(factory.ithVar(secondBit));
		BDD bdd2 = factory.nithVar(firstBit).and(factory.nithVar(secondBit));
		return bdd1.or(bdd2);
	}

	/**
	 * Find the index of a bit of a domain in the BDD. 
	 * @param factory
	 * @param domain
	 * @param bit
	 * @return
	 */
	public static int findBitIndex(BDDFactory factory, int domain, int bit) {
		int n = factory.numberOfDomains();
		return bit * n + domain;
	}

	/**
	 * Return a BDD that required one domain be smaller than another domain. 
	 * @param factory
	 * @param smallerDomain 
	 * @param largerDomain
	 * @return
	 */
	public static BDD lessThan(BDDFactory factory, int smallerDomain, int largerDomain) {
		BDD result = factory.zero();
		int size = factory.getDomain(0).size().intValue();
		int numOfBits = (int) (Math.log(size) / Math.log(2));
		for (int i = numOfBits - 1; i >= 0; i--) {
			BDD newbdd = factory.one();
			for (int j = numOfBits - 1; j > i; j--) {
				int index1 = findBitIndex(factory, smallerDomain, j);
				int index2 = findBitIndex(factory, largerDomain, j);
				newbdd = newbdd.and(equalBits(factory, index1, index2));
			}
			int index1 = findBitIndex(factory, smallerDomain, i);
			int index2 = findBitIndex(factory, largerDomain, i);
			newbdd = newbdd.and(factory.nithVar(index1)).and(factory.ithVar(index2));
			result = result.or(newbdd);
		}
		return result;
	}

	/**
	 * Return a BDD that requires second given domain be the first domain + given value X. 
	 * @param factory
	 * @param firstDomain
	 * @param secondDomain
	 * @param x
	 * @return
	 */
	public static BDD secondDomainIsFirstDomainPlusX(BDDFactory factory, int firstDomain, int secondDomain, int x) {
		int size = factory.getDomain(firstDomain).size().intValue();
		if (x >= size)
			return factory.zero();
		int numOfBits = (int) (Math.log(size) / Math.log(2));
		return secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, 0, 0);
	}

	/**
	 * Return a BDD that requires the second given domain is at most the first domain + given value x. 
	 * @param factory
	 * @param firstDomain
	 * @param secondDomain
	 * @param x
	 * @return
	 */
	public static BDD secondDomainIsAtMostFirstDomainPlusX(BDDFactory factory, int firstDomain, int secondDomain, int x) {
		BDD bdd = factory.zero();
		for (int i = 0; i <= x; i++) {
			bdd = bdd.or(secondDomainIsFirstDomainPlusX(factory, firstDomain, secondDomain, i));
		}
		return bdd;
	}

	//helper 
	public static BDD secondDomainIsFirstDomainPlusXHelper(BDDFactory factory, int firstDomain, int secondDomain, int numOfBits, int x, int bitLocation, int carry) {
		int bitLocationOfFirstDomain = findBitIndex(factory, firstDomain, bitLocation);
		int bitLocationOfSecondDomain = findBitIndex(factory, secondDomain, bitLocation);
		int bitValueOfX = getBitOfInt(x, bitLocation);
		if (carry == 0) {
			if (bitValueOfX == 0) {
				if (bitLocation < numOfBits - 1) {
					BDD bdd1 = factory.ithVar(bitLocationOfFirstDomain).and(factory.ithVar(bitLocationOfSecondDomain));
					bdd1 = bdd1.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 0));

					BDD bdd2 = factory.nithVar(bitLocationOfFirstDomain).and(factory.nithVar(bitLocationOfSecondDomain));
					bdd2 = bdd2.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 0));

					return bdd1.or(bdd2);
				} else {
					//it is the last bit, so we can't continue anymore.
					BDD bdd1 = factory.ithVar(bitLocationOfFirstDomain).and(factory.ithVar(bitLocationOfSecondDomain));
					BDD bdd2 = factory.nithVar(bitLocationOfFirstDomain).and(factory.nithVar(bitLocationOfSecondDomain));
					return bdd1.or(bdd2);
				}
			} else { //b=1
				if (bitLocation < numOfBits - 1) {
					BDD bdd1 = factory.ithVar(bitLocationOfFirstDomain).and(factory.nithVar(bitLocationOfSecondDomain));
					bdd1 = bdd1.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 1));

					BDD bdd2 = factory.nithVar(bitLocationOfFirstDomain).and(factory.ithVar(bitLocationOfSecondDomain));
					bdd2 = bdd2.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 0));

					return bdd1.or(bdd2);
				} else {
					BDD bdd1 = factory.nithVar(bitLocationOfFirstDomain).and(factory.ithVar(bitLocationOfSecondDomain));
					return bdd1;
				}
			}

		} else {// carry = 1
			if (bitValueOfX == 0) {
				if (bitLocation < numOfBits - 1) {
					BDD bdd1 = factory.ithVar(bitLocationOfFirstDomain).and(factory.nithVar(bitLocationOfSecondDomain));
					bdd1 = bdd1.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 1));

					BDD bdd2 = factory.nithVar(bitLocationOfFirstDomain).and(factory.ithVar(bitLocationOfSecondDomain));
					bdd2 = bdd2.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 0));

					return bdd1.or(bdd2);
				} else {
					//it is the last bit, so we can't continue anymore.
					BDD bdd2 = factory.nithVar(bitLocationOfFirstDomain).and(factory.ithVar(bitLocationOfSecondDomain));
					return bdd2;
				}
			} else { //b=1
				if (bitLocation < numOfBits - 1) {
					BDD bdd1 = factory.ithVar(bitLocationOfFirstDomain).and(factory.ithVar(bitLocationOfSecondDomain));
					bdd1 = bdd1.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 1));

					BDD bdd2 = factory.nithVar(bitLocationOfFirstDomain).and(factory.nithVar(bitLocationOfSecondDomain));
					bdd2 = bdd2.and(secondDomainIsFirstDomainPlusXHelper(factory, firstDomain, secondDomain, numOfBits, x, bitLocation + 1, 1));

					return bdd1.or(bdd2);
				} else {
					return factory.zero();
				}
			}
		}
	}

	public static int getBitOfInt(int x, int bitLocation) {
		return (x >> bitLocation) & 1;
	}

	/**
	 * Return a BDD that requires the second domain to be the frist domain + 1. 
	 * @param factory
	 * @param smallerDomain
	 * @param largerDomain
	 * @return
	 */
	public static BDD oneDomainIsLargerByOne(BDDFactory factory, int smallerDomain, int largerDomain) {
		return secondDomainIsFirstDomainPlusX(factory, smallerDomain, largerDomain, 1);
	}

	/**
	 * Return a BDD that requires two domain to be equal. 
	 * @param factory
	 * @param domain1
	 * @param domian2
	 * @return
	 */
	public static BDD twoDomainsAreEqual(BDDFactory factory, int domain1, int domian2) {
		return secondDomainIsFirstDomainPlusX(factory, domain1, domian2, 0);
	}

	
	public static int rightSize(int desiredSize) {
		int log2 = new Double(Math.floor(Math.log(desiredSize) / Math.log(2))).intValue();
		if (Math.pow(2, log2) == desiredSize)
			return desiredSize;
		else
			return (new Double(Math.pow(2, log2 + 1)).intValue());

	}

	public static void writeToFile(String fileName, String content) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(fileName);
		writer.print(content);
		writer.close();
	}

	public static void appendLineToFile(String fileName, String line) throws IOException {
		BufferedWriter output = new BufferedWriter(new FileWriter(fileName, true));
		output.newLine();
		output.append(line);
		output.close();
	}

	public static BDD reach(BDD startStates, BDD program, BDD envBDD, BDD faultBDD) {
		BDD statesReachedByEnvironment = Utils.unprime(startStates.and(envBDD));
		BDD statesReachedByOther = Utils.unprime(startStates.and(program.or(faultBDD)));

		BDD result = startStates;
		if (!statesReachedByEnvironment.and(startStates.not()).isZero())
			result = result.or(reachNoEnv(statesReachedByEnvironment.or(startStates), program, envBDD, faultBDD));
		if (!statesReachedByOther.and(startStates.not()).isZero())
			result = result.or(reach(statesReachedByOther.or(startStates), program, envBDD, faultBDD));
		return result;
	}


	public static BDD reachNoEnv(BDD startStates, BDD program, BDD envBDD, BDD faultBDD) {
		BDD statesReachedByEnvironment = Utils.unprime(startStates.and(Utils.removeNextState(program).not()).and(envBDD));
		BDD statesReachedByOther = Utils.unprime(startStates.and(program.or(faultBDD)));
		BDD result = startStates;
		if (!statesReachedByEnvironment.and(startStates.not()).isZero())
			result = result.or(reachNoEnv(statesReachedByEnvironment.or(startStates), program, envBDD, faultBDD));
		if (!statesReachedByOther.and(startStates.not()).isZero())
			result = result.or(reach(statesReachedByOther.or(startStates), program, envBDD, faultBDD));
		return result;
	}

}

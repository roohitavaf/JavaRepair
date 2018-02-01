package edu.msu.cse.javaRepair.examples.stabilization;

import edu.msu.cse.javaRepair.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.rmi.CORBA.Util;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import edu.msu.cse.javaRepair.*;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class Shpherding_1D_stabilization {
	static int numOfCells;

	// 0
	static int fc;

	// 1
	static int sc;

	static int sc_goal;

	static int nodeNum;
	static int cashSize;

	//experiment parameters
	static final int d_max = 100;
	static final int n_max = 1;
	static final String output = "output_1D_stabilization_100.csv";
	static final String output_stateSpace = "output_1D_stabilization_stateSpace.csv";

	public static void main(String[] args) throws IOException {
		readConfigFile(args[0]);
		int steps = new Integer(args[1]);
		//visualExample(steps);
		experiment(d_max, n_max, output);
	}

	public static void visualExample(int steps) {
		BDDFactory factory = createFactory();
		BDD invBDD = factory.getDomain(Utils.getVInx(1)).ithVar(sc_goal);
		BDD spanBDD = createSpan(factory);
		BDD progBDD = factory.zero();

		BDD envBDD = createEnvironmentBDD(factory);
		BDD resBDD = createProgramRestrictionBDD(factory);
		resBDD.orWith(extraRestrictions(factory, spanBDD));

		long start = System.nanoTime();
		BDD repairedProgram = Repairer.addStabilization(progBDD, envBDD, invBDD, resBDD, spanBDD);

		long end = System.nanoTime();
		double time = (end - start) / Math.pow(10, 9);

		System.out.println("Initial state: ");
		printBoard();

		System.out.println("---------Simulation--------");
		for (int i = 0; i < steps; i++) {
			if (sc == sc_goal)
				break;
			BDD currentBDD = getCurrentBDD(factory);

			BDD nextState;
			int nextValue;

			System.out.println("Program moves:");

			BDD possibleNextState = Utils.unprime(currentBDD.and(repairedProgram)).satOne();
			if (!possibleNextState.isZero()) {
				nextState = Utils.unprime(currentBDD.and(repairedProgram)).satOne();
			} else
				nextState = currentBDD;
			System.out.println("nextState after probram moves:");
			nextState.printSetWithDomains();

			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(0))).intValue();
			if (nextValue != -1)
				fc = nextValue;
			else
				System.out.println("Problem reading fc value");

			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(1))).intValue();
			if (nextValue != -1)
				sc = nextValue;

			printBoard();
			currentBDD = getCurrentBDD(factory);

			System.out.println("Environment moves:");
			possibleNextState = Utils.unprime(currentBDD.and(envBDD)).satOne();
			if (!possibleNextState.isZero()) {
				nextState = possibleNextState;
			} else
				System.out.println("Environment has nothing to do.");
			System.out.println("nextState after environment moves:");
			nextState.printSetWithDomains();

			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(1))).intValue();
			if (nextValue != -1)
				sc = nextValue;
			else
				System.out.println("Problem reading sc value");
			printBoard();

			System.out.println("-----------------");
		}
	}

	public static void experiment(int d_max, int n_max, String output) throws IOException {
		// For final experiments
		Utils.writeToFile(output, "dimension, repair time (s)");
		for (int d = 100; d <= d_max; d+=10) {

			int dimension = d;
			numOfCells = d;
			if (d <= 16) {
				nodeNum = 2000000;
				cashSize = 200000;
			} else if (d <= 32) {
				nodeNum = 2000000;
				cashSize = 200000;
			}
			sc_goal = dimension - 1;

			double totalTime = 0;

			for (int n = 0; n < n_max; n++) {
				BDDFactory factory = createFactory();
				BDD invBDD = factory.getDomain(Utils.getVInx(1)).ithVar(sc_goal);
				BDD spanBDD = createSpan(factory);
				BDD progBDD = factory.zero();

				BDD envBDD = createEnvironmentBDD(factory);
				BDD resBDD = createProgramRestrictionBDD(factory);
				resBDD.orWith(extraRestrictions(factory, spanBDD));

				long start = System.nanoTime();
				BDD repairedProgram = Repairer.addStabilization(progBDD, envBDD, invBDD, resBDD, spanBDD);
				long end = System.nanoTime();
				double time = (end - start) / Math.pow(10, 9);
				totalTime += time;

			}
			Utils.appendLineToFile(output, d + "," + totalTime / n_max);
			System.out.println(d + " is done.");
		}
	}

	private static BDD createSpan(BDDFactory factory) {
		int size = factory.getDomain(0).size().intValue();
		BDD bdd = factory.zero();
		for (int i = numOfCells; i < size; i++) {
			bdd.orWith(factory.getDomain(Utils.getVInx(0)).ithVar(i));
			bdd.orWith(factory.getDomain(Utils.getVInx(1)).ithVar(i));
		}
		return bdd.not();
	}

	static BDDFactory createFactory() {

		int rightSize = Utils.rightSize(numOfCells);

		int n = (int) Math.pow(2, numOfCells / 2) * 1000000;
		int c = (int) Math.pow(2, numOfCells / 2) * 100000;

		BDDFactory factory = JFactory.init(nodeNum, cashSize);
		int[] domainSizes = new int[4];
		Arrays.fill(domainSizes, rightSize);
		factory.extDomain(domainSizes);

		factory.getDomain(Utils.getVInx(0)).setName("fc");
		factory.getDomain(Utils.getPrimeVInx(0)).setName("fc'");
		factory.getDomain(Utils.getVInx(1)).setName("sc");
		factory.getDomain(Utils.getPrimeVInx(1)).setName("sc'");

		return factory;
	}

	static void readConfigFile(String configFile) throws IOException {
		File file = new File(configFile);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		String configContent = (new String(data, "UTF-8")).trim();

		numOfCells = new Integer(findValueInAString(configContent, "dimension"));

		fc = new Integer(findValueInAString(configContent, "fc"));

		sc = new Integer(findValueInAString(configContent, "sc"));

		sc_goal = numOfCells - 1;

		nodeNum = new Integer(findValueInAString(configContent, "nodeNum"));
		cashSize = new Integer(findValueInAString(configContent, "cashSize"));
	}

	public static String findValueInAString(String source, String key) {
		int beginIndex = source.indexOf(key + ":");
		if (beginIndex == -1)
			return "";
		int endIndex = source.indexOf(";", beginIndex);
		String value = source.substring(beginIndex + key.length() + 1, endIndex).trim();
		return value;
	}

	static BDD createEnvironmentBDD(BDDFactory factory) {
		// farmer < sheep
		BDD bdd = sheepMoveLessThan(factory, 0, 1);
		// farmer > sheep
		BDD bdd2 = sheepMoveGreaterThan(factory, 0, 1);
		// farmer = sheep
		BDD bdd3 = sheepMoveEqual(factory, 0, 1);

		//self-loop 
		BDD selfLoop = (factory.getDomain(Utils.getVInx(1)).ithVar(sc_goal));
		for (int i = 0; i < 2; i++) {
			selfLoop = selfLoop.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}
		return bdd.or(bdd2).or(bdd3).or(selfLoop);
	}

	// n1 = 0, s = 1
	static BDD sheepMoveEqual(BDDFactory factory, int n1, int s) {
		BDD bdd = factory.one();

		// when farmer and sheep are in the same location
		bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(n1), Utils.getVInx(s)));

		// Case 1: where sheep going right if it is not in the rightmost
		// location
		BDD goRightBDD = (factory.getDomain(Utils.getVInx(s)).ithVar(numOfCells - 1).not());
		goRightBDD = goRightBDD.and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(s), Utils.getPrimeVInx(s)));
		goRightBDD = goRightBDD.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(0), Utils.getPrimeVInx(0)));

		// Case2: e where sheep goes left if it is not in the leftmost location
		BDD goLeftBDD = (factory.getDomain(Utils.getVInx(s)).ithVar(0).not());
		goLeftBDD = goLeftBDD.and(Utils.oneDomainIsLargerByOne(factory, Utils.getPrimeVInx(s), Utils.getVInx(s)));
		goLeftBDD = goLeftBDD.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(0), Utils.getPrimeVInx(0)));

		// Sheep can do case 1 or 2
		bdd = bdd.and((goRightBDD).or(goLeftBDD));
		return bdd;

	}

	// n1 = 0, s = 1
	static BDD sheepMoveLessThan(BDDFactory factory, int n1, int s) {
		BDD bdd = factory.one();

		// when farmer < sheep
		bdd = bdd.and(Utils.lessThan(factory, Utils.getVInx(n1), Utils.getVInx(s)));

		// and sheep is not at the rightmost location
		bdd = bdd.and(factory.getDomain(Utils.getVInx(s)).ithVar(numOfCells - 1).not());

		// Sheep moves right
		bdd = bdd.and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(s), Utils.getPrimeVInx(s)));

		// Sheep cannot move the farmer, so farmer is in the same location
		bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(0), Utils.getPrimeVInx(0)));

		return bdd;
	}

	// n1 = 0, s = 1
	static BDD sheepMoveGreaterThan(BDDFactory factory, int n1, int s) {
		BDD bdd = factory.one();

		// when sheep < farmer
		bdd = bdd.and(Utils.lessThan(factory, Utils.getVInx(s), Utils.getVInx(n1)));

		// and sheep is not at the leftmost location
		bdd = bdd.and(factory.getDomain(Utils.getVInx(s)).ithVar(0).not());

		// sheep moves left
		bdd = bdd.and(Utils.oneDomainIsLargerByOne(factory, Utils.getPrimeVInx(s), Utils.getVInx(s)));

		// sheep cannot move the farmer
		bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(0), Utils.getPrimeVInx(0)));

		return bdd;
	}

	static BDD extraRestrictions(BDDFactory factory, BDD spanBDD) {
		BDD outOfSpan = spanBDD.not();
		BDD bdd = Utils.prime(outOfSpan);
		// Progrma cannot go out of span
		bdd.orWith(outOfSpan);
		return bdd;

	}

	static BDD createProgramRestrictionBDD(BDDFactory factory) {
		BDD bdd = programMove(factory, 0);
		return bdd.not();

	}

	// p = 0
	static BDD programMove(BDDFactory factory, int p) {

		// Program can go right
		BDD bdd = (factory.getDomain(Utils.getVInx(p)).ithVar(numOfCells - 1).not().and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(p), Utils.getPrimeVInx(p))));

		// Program can go left
		bdd = bdd.or(factory.getDomain(Utils.getVInx(p)).ithVar(0).not().and(Utils.oneDomainIsLargerByOne(factory, Utils.getPrimeVInx(p), Utils.getVInx(p))));

		// Program cannot change the location of the sheep
		bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(1), Utils.getPrimeVInx(1)));
		return bdd;
	}

	static BDD getCurrentBDD(BDDFactory factory) {
		BDD bdd = factory.getDomain(Utils.getVInx(0)).ithVar(fc);
		bdd = bdd.and(factory.getDomain(Utils.getVInx(1)).ithVar(sc));
		return bdd;
	}

	public static void printBoard() {
		for (int r = -1; r < 1; r++) {
			for (int c = 0; c <= numOfCells; c++) {
				if (r == -1 && c != numOfCells)
					System.out.print(" _");
				else if (c == numOfCells && r != -1)
					System.out.print("|");
				else if (c == fc)
					System.out.print("|F̲");
				else if (c == sc)
					System.out.print("|S̲");
				else if (c == sc_goal)
					System.out.print("|G̲");
				else if (c != numOfCells)
					System.out.print("|_");
			}
			System.out.println();
		}

	}

}

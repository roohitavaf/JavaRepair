package edu.msu.cse.javaRepair.examples.failsafe;

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

public class Shpherding_2D_Failsafe_arbitraryFault {
	static int dimensionSize;
	//0
	static int fr;
	//1
	static int fc;

	//2
	static int sr;
	//3
	static int sc;

	static int sr_goal;
	static int sc_goal;

	static int nodeNum;
	static int cashSize;

	//experiment parameters
	static final int d_max = 40;
	static final int n_max = 1;
	static final String output = "output_2D_failsafe_arbitraryFault_30_40.csv";
	static final String output_stateSpace = "output_2D_failsafe_arbitraryFault_stateSpace_30_40.csv";

	public static void main(String[] args) throws IOException {
		System.out.println("Start");
		readConfigFile(args[0]);
		int steps = new Integer(args[1]);
		//visualExample(steps);
		//experiment(d_max, n_max, output);
		stateSpace(d_max, output_stateSpace);
	}

	public static void stateSpace(int d_max, String output) throws IOException {
		//For final experiments
		Utils.writeToFile(output, "size, state sapce size");
		for (int d = 30; d <= d_max; d+=10) {
			dimensionSize = d;
			if (d <= 16) {
				nodeNum = 2000000;
				cashSize = 200000;
			} else if (d <= 32) {
				nodeNum = 10000000;
				cashSize = 1000000;
			}
			sr_goal = 0;
			sc_goal = dimensionSize - 1;
			double totalTime = 0;

			BDDFactory factory = createFactory();
			BDD invBDD = factory.getDomain(Utils.getVInx(0)).ithVar(sr_goal).and(factory.getDomain(Utils.getVInx(1)).ithVar(sc_goal)
					.and(factory.getDomain(Utils.getVInx(2)).ithVar(sr_goal)).and(factory.getDomain(Utils.getVInx(3)).ithVar(sc_goal)));
			BDD spanBDD = createSpan(factory);
			BDD progBDD = createProgramBDD(factory);

			BDD envBDD = createEnvironmentBDD(factory);
			BDD resBDD = createProgramRestrictionBDD(factory);
			resBDD.orWith(extraRestrictions(factory, spanBDD));

			BDD faultsBDD = createFaultBDD(factory, spanBDD, invBDD);
			BDD badTransitionsBDD = createBadTransitionsBDD(factory, invBDD);

			BDD[] result = Repairer.addFailsafe(invBDD, progBDD, envBDD, faultsBDD, resBDD, badTransitionsBDD, false);
			BDD reachableState = Utils.reach(invBDD, result[1], envBDD, faultsBDD);
			double stateSpaceSize = reachableState.satCount();
			Utils.appendLineToFile(output, d + "," + stateSpaceSize);
			System.out.println(d + " = " + stateSpaceSize );
		}
	}


	
	public static void experiment(int d_max, int n_max, String output) throws IOException {
		//For final experiments
		Utils.writeToFile(output, "dimension, repair time (s)");
		for (int d = 30; d <= d_max; d+=10) {
			dimensionSize = d;
			if (d <= 16) {
				nodeNum = 2000000;
				cashSize = 200000;
			} else if (d <= 32) {
				nodeNum = 10000000;
				cashSize = 1000000;
			}
			sr_goal = 0;
			sc_goal = dimensionSize - 1;
			double totalTime = 0;

			for (int n = 0; n < n_max; n++) {
				BDDFactory factory = createFactory();
				BDD invBDD = factory.getDomain(Utils.getVInx(2)).ithVar(sr_goal).and(factory.getDomain(Utils.getVInx(3)).ithVar(sc_goal));
				BDD spanBDD = createSpan(factory);
				BDD progBDD = createProgramBDD(factory);

				BDD envBDD = createEnvironmentBDD(factory);
				BDD resBDD = createProgramRestrictionBDD(factory);
				resBDD.orWith(extraRestrictions(factory, spanBDD));

				BDD faultsBDD = createFaultBDD(factory, spanBDD, invBDD);
				BDD badTransitionsBDD = createBadTransitionsBDD(factory, invBDD);

				long start = System.nanoTime();
				BDD[] result = Repairer.addFailsafe(invBDD, progBDD, envBDD, faultsBDD, resBDD, badTransitionsBDD, false);
				long end = System.nanoTime();
				double time = (end - start) / Math.pow(10, 9);
				//Utils.appendLineToFile(output, d + "," + time);
				totalTime += time;
			}
			Utils.appendLineToFile(output, d + "," + totalTime / n_max);
			System.out.println(d + " is done.");
		}
	}

	public static void visualExample(int steps) {
		//For visual test...
		BDDFactory factory = createFactory();
		BDD invBDD = factory.getDomain(Utils.getVInx(2)).ithVar(sr_goal).and(factory.getDomain(Utils.getVInx(3)).ithVar(sc_goal));
		BDD spanBDD = createSpan(factory);
		BDD progBDD = createProgramBDD(factory);

		BDD envBDD = createEnvironmentBDD(factory);
		BDD resBDD = createProgramRestrictionBDD(factory);
		resBDD.orWith(extraRestrictions(factory, spanBDD));

		BDD faultsBDD = createFaultBDD(factory, spanBDD, invBDD);
		BDD badTransitionsBDD = createBadTransitionsBDD(factory, invBDD);

		long start = System.nanoTime();
		BDD[] result = Repairer.addFailsafe(invBDD, progBDD, envBDD, faultsBDD, resBDD, badTransitionsBDD, true);
		if (result == null) {
			System.exit(0);
		}
		BDD repairedProgram = result[0];

		long end = System.nanoTime();
		double time = (end - start) / Math.pow(10, 9);

		printBoard();
		System.out.println("-----------------");
		for (int i = 0; i < steps; i++) {
			if (sr == sr_goal && sc == sc_goal)
				break;
			BDD currentBDD = getCurrentBDD(factory);

			BDD nextState;
			int nextValue;

			nextState = Utils.unprime(currentBDD.and(repairedProgram)).satOne();
			System.out.println("Program moves:");
			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(0))).intValue();
			if (nextValue != -1)
				fr = nextValue;
			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(1))).intValue();
			if (nextValue != -1)
				fc = nextValue;

			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(2))).intValue();
			if (nextValue != -1)
				sr = nextValue;
			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(3))).intValue();
			if (nextValue != -1)
				sc = nextValue;

			printBoard();
			System.out.println("-----------------");
			currentBDD = getCurrentBDD(factory);

			System.out.println("Environment moves:");

			nextState = Utils.unprime(currentBDD.and(envBDD)).satOne();

			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(2))).intValue();
			if (nextValue != -1)
				sr = nextValue;
			nextValue = nextState.scanVar(factory.getDomain(Utils.getVInx(3))).intValue();
			if (nextValue != -1)
				sc = nextValue;
			printBoard();

			System.out.println("-----------------");

		}
	}

	private static BDD createBadTransitionsBDD(BDDFactory factory, BDD invBDD) {
		BDD equal = Utils.twoDomainsAreEqual(factory, Utils.getVInx(0), Utils.getVInx(2)).and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(1), Utils.getVInx(3)));

		BDD ColumnIsGreaterByOne = Utils.twoDomainsAreEqual(factory, Utils.getVInx(0), Utils.getVInx(2)).and((Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(1), Utils.getVInx(3))).or(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(3), Utils.getVInx(1))));
		BDD RowIsGreaterByOne = Utils.twoDomainsAreEqual(factory, Utils.getVInx(1), Utils.getVInx(3)).and((Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(0), Utils.getVInx(2))).or(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(2), Utils.getVInx(0))));
		BDD goodStates = equal.or(ColumnIsGreaterByOne).or(RowIsGreaterByOne);
		//bad transitions are states that goes from good states to bad states. Transitions that goes from bad states to bad states are fine in this version. 
		BDD badTransitions = goodStates.and(invBDD.not()).and(Utils.prime(goodStates.not()));
		return badTransitions;
	}

	private static BDD createFaultBDD(BDDFactory factory, BDD spanBDD, BDD invBDD) {
		BDD bdd = invBDD.and(Utils.prime(spanBDD));//.and(Utils.twoDomainsAreEqual(factory, Utils.getPrimeVInx(0), Utils.getPrimeVInx(2))
		//.and(Utils.twoDomainsAreEqual(factory, Utils.getPrimeVInx(1), Utils.getPrimeVInx(3)))
		//);
		return bdd;
	}

	private static BDD createProgramBDD(BDDFactory factory) {
		return factory.zero();
	}

	private static BDD createSpan(BDDFactory factory) {
		int size = factory.getDomain(0).size().intValue();
		BDD bdd = factory.zero();
		for (int i = dimensionSize; i < size; i++) {
			bdd.orWith(factory.getDomain(Utils.getVInx(0)).ithVar(i));
			bdd.orWith(factory.getDomain(Utils.getVInx(1)).ithVar(i));
			bdd.orWith(factory.getDomain(Utils.getVInx(2)).ithVar(i));
			bdd.orWith(factory.getDomain(Utils.getVInx(3)).ithVar(i));
		}
		return bdd.not();
	}

	static BDDFactory createFactory() {

		int rightSize = Utils.rightSize(dimensionSize);
		//System.out.println("Right size= " + rightSize);

		int n = (int) Math.pow(2, dimensionSize / 2) * 1000000;
		int c = (int) Math.pow(2, dimensionSize / 2) * 100000;

		//System.out.println("Selected nodeNum= " + n);
		//System.out.println("Selected cashSize= " + c);

		BDDFactory factory = JFactory.init(nodeNum, cashSize);
		int[] domainSizes = new int[8];
		Arrays.fill(domainSizes, rightSize);
		factory.extDomain(domainSizes);

		factory.getDomain(Utils.getVInx(0)).setName("fr");
		factory.getDomain(Utils.getPrimeVInx(0)).setName("fr'");
		factory.getDomain(Utils.getVInx(1)).setName("fc");
		factory.getDomain(Utils.getPrimeVInx(1)).setName("fc'");

		factory.getDomain(Utils.getVInx(2)).setName("sr");
		factory.getDomain(Utils.getPrimeVInx(2)).setName("sr'");
		factory.getDomain(Utils.getVInx(3)).setName("sc");
		factory.getDomain(Utils.getPrimeVInx(3)).setName("sc'");

		return factory;
	}

	static void readConfigFile(String configFile) throws IOException {
		File file = new File(configFile);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		String configContent = (new String(data, "UTF-8")).trim();

		dimensionSize = new Integer(findValueInAString(configContent, "dimension"));

		fr = new Integer(findValueInAString(configContent, "fr"));
		fc = new Integer(findValueInAString(configContent, "fc"));

		sr = new Integer(findValueInAString(configContent, "sr"));
		sc = new Integer(findValueInAString(configContent, "sc"));

		sr_goal = 0;//new Integer(findValueInAString(configContent, "sr_goal"));
		sc_goal = dimensionSize - 1;//new Integer(findValueInAString(configContent, "sc_goal"));

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
		BDD bdd = sheepMoveLessThan(factory, 0, 2);
		bdd = bdd.or(sheepMoveLessThan(factory, 1, 3));

		BDD bdd2 = sheepMoveGreaterThan(factory, 0, 2);
		bdd2 = bdd2.or(sheepMoveLessThan(factory, 1, 3));

		BDD bdd3 = sheepMoveEqual(factory, 0, 2);
		bdd3 = bdd3.or(sheepMoveEqual(factory, 1, 3));

		BDD selfLoop = factory.getDomain(Utils.getVInx(2)).ithVar(sr_goal).and(factory.getDomain(Utils.getVInx(3)).ithVar(sc_goal));
		for (int i = 0; i < 4; i++) {
			selfLoop = selfLoop.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}

		BDD invaraint = factory.getDomain(Utils.getVInx(2)).ithVar(sr_goal).and(factory.getDomain(Utils.getVInx(3)).ithVar(sc_goal));

		return ((bdd.or(bdd2).or(bdd3)).and(invaraint.not())).or(selfLoop);
	}

	static BDD sheepMoveEqual(BDDFactory factory, int n1, int s) {
		BDD bdd = factory.one();
		bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(n1), Utils.getVInx(s)));

		//goind right on equal
		BDD goRightBDD = (factory.getDomain(Utils.getVInx(s)).ithVar(dimensionSize - 1).not());
		goRightBDD = goRightBDD.and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(s), Utils.getPrimeVInx(s)));
		for (int i = 0; i < 4; i++) {
			if (i == s)
				continue;
			goRightBDD = goRightBDD.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}

		//going left of equal
		BDD goLeftBDD = (factory.getDomain(Utils.getVInx(s)).ithVar(0).not());
		goLeftBDD = goLeftBDD.and(Utils.oneDomainIsLargerByOne(factory, Utils.getPrimeVInx(s), Utils.getVInx(s)));

		for (int i = 0; i < 4; i++) {
			if (i == s)
				continue;
			goLeftBDD = goLeftBDD.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}

		bdd = bdd.and((goRightBDD).or(goLeftBDD));
		return bdd;

	}

	static BDD sheepMoveLessThan(BDDFactory factory, int n1, int s) {
		BDD bdd = factory.one();
		bdd = bdd.and(Utils.lessThan(factory, Utils.getVInx(n1), Utils.getVInx(s)));

		bdd = bdd.and(factory.getDomain(Utils.getVInx(s)).ithVar(dimensionSize - 1).not());
		bdd = bdd.and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(s), Utils.getPrimeVInx(s)));
		for (int i = 0; i < 4; i++) {
			if (i == s)
				continue;
			bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}
		return bdd;
	}

	static BDD sheepMoveGreaterThan(BDDFactory factory, int n1, int s) {
		BDD bdd = factory.one();
		bdd = bdd.and(Utils.lessThan(factory, Utils.getVInx(s), Utils.getVInx(n1)));

		bdd = bdd.and(factory.getDomain(Utils.getVInx(s)).ithVar(0).not());
		bdd = bdd.and(Utils.oneDomainIsLargerByOne(factory, Utils.getPrimeVInx(s), Utils.getVInx(s)));

		for (int i = 0; i < 4; i++) {
			if (i == s)
				continue;
			bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}

		return bdd;
	}

	static BDD extraRestrictions(BDDFactory factory, BDD spanBDD) {
		BDD outOfSpan = spanBDD.not();
		BDD bdd = Utils.prime(outOfSpan);
		bdd.orWith(outOfSpan);
		return bdd;

	}

	static BDD createProgramRestrictionBDD(BDDFactory factory) {
		BDD bdd = programMove(factory, 0);
		bdd = bdd.or(programMove(factory, 1));

		BDD selfLoops = factory.one();
		for (int i = 0; i < 4; i++) {
			selfLoops = selfLoops.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}

		bdd.orWith(selfLoops);
		return bdd.not();

	}

	static BDD programMove(BDDFactory factory, int p) {

		BDD bdd = (factory.getDomain(Utils.getVInx(p)).ithVar(dimensionSize - 1).not().and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(p), Utils.getPrimeVInx(p))));
		bdd = bdd.or(factory.getDomain(Utils.getVInx(p)).ithVar(0).not().and(Utils.oneDomainIsLargerByOne(factory, Utils.getPrimeVInx(p), Utils.getVInx(p))));
		for (int i = 0; i < 4; i++) {
			if (i == p)
				continue;
			bdd = bdd.and(Utils.twoDomainsAreEqual(factory, Utils.getVInx(i), Utils.getPrimeVInx(i)));
		}

		return bdd;
	}

	static BDD getCurrentBDD(BDDFactory factory) {
		BDD bdd = factory.getDomain(Utils.getVInx(0)).ithVar(fr).and(factory.getDomain(Utils.getVInx(1)).ithVar(fc));
		bdd = bdd.and(factory.getDomain(Utils.getVInx(2)).ithVar(sr).and(factory.getDomain(Utils.getVInx(3)).ithVar(sc)));
		return bdd;
	}

	public static void printBoard() {
		for (int r = -1; r < dimensionSize; r++) {
			for (int c = 0; c <= dimensionSize; c++) {
				if (r == -1 && c != dimensionSize)
					System.out.print(" _");
				else if (c == dimensionSize && r != -1)
					System.out.print("|");
				else if (r == fr && c == fc)
					System.out.print("|F̲");
				else if (r == sr && c == sc)
					System.out.print("|S̲");
				else if (r == sr_goal && c == sc_goal)
					System.out.print("|G̲");
				else if (c != dimensionSize)
					System.out.print("|_");
			}
			System.out.println();
		}

	}

}

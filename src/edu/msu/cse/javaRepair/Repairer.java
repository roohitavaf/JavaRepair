package edu.msu.cse.javaRepair;


import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
/**
 * This is the main calss that includes the algorithms of the paper. 
 * 
 * The following algorithms are tried to be exactly like the ones in paper. 
 * I have added comments to show which part in the code is which line in the paper. 
 * Also, _1 is equal to the prime in the paper. Thus, for example Delta_p_1 is 
 * equal to \delta_p' in the paper. 
 * @author roohitavaf
 *
 */
public class Repairer {

	/**
	 * Add stabilization to the given program. 
	 * @param Delta_p The BDD representing the given program. 
	 * @param Delta_e The BDD representing the environment. 
	 * @param S The The BDD representing the invariant. 
	 * @param Delta_r The BDD representing the set of program restrictions. 
	 * @param span The BDD representing the relevant states. Any state outside this BDD are not considered in the repair. 
	 * @return The BDD of the revised program, or </br> null  if the repair is impossible. 
	 */
	public static BDD addStabilization(/* We don't need S_p */ BDD Delta_p, BDD Delta_e, BDD S, BDD Delta_r, BDD span) {
		BDDFactory factroy = Delta_p.getFactory();
		BDD Delta_p_1 = Delta_p.and(S); // Line 1
		BDD R = factroy.one().and(S); // Line 2
		BDD Rp = factroy.zero();// Line 3

		BDD R_p;
		do // Line 4
		{
			R_p = factroy.one().and(R); // Line 5
			BDD Rp_1 = Utils.removeNextState(Delta_r.not().and(Utils.prime(R))).and((R.or(Rp)).not()); // Line
																										// 6
			Rp = Rp.or(Rp_1); // Line 7
			Delta_p_1 = Delta_p_1.or(Delta_r.not().and(Rp_1).and(Utils.prime(R))); // Lines
																					// 8-10

			BDD s0 = (R).not(); // Line 11
			BDD s2 = (R.or(Rp)).not(); // Line 11
			s0 = s0.and(Utils.removeNextState(Delta_e.and(Utils.prime(s2).and(s0)).not())); // Line
																							// 11
			BDD s1 = (R.or(Rp)); // Line 11
			s0 = s0.and(Utils.removeNextState(Delta_e.and(Utils.prime(s1)).and(s0)).or(Rp)); // Line
																								// 11
			R = R.or(s0); // Line 12-13
		} while (!R_p.equals(R)); // Line 14
		// adding irrelevant states to R.
		R = R.or(span.not()); // No in the algorithm of the paper. We need it to
								// deal with BDD of sizes other than powers of
								// 2.
		if (!R.isOne()) { // Line 15
			return null; //Line 16

		} // Line 17
		return Delta_p_1; // Line 18-19
	}

	/**
	 * Add failsafe fault-tolerance to the given program. 
	 * @param Delta_p The BDD representing the given program. 
	 * @param Delta_e The BDD representing the environment. 
	 * @param S The BDD representing the invariant. 
	 * @param Delta_b The BDD representing the set of bad transitions. 
	 * @param Delta_r The BDD representing the set of program restrictions. 
	 * @param f The BDD representing the set of fault restrictions. 
	 * @param verbose if set to true, the function prints messages during the repair. 
	 * @return An array of BDDs. The first BDD represents the invariant of the revised program. 
	 * 		   The second BDD represents the transitions of the revised program, or </br> null if the repair is impossible.
	 */
	public static BDD[] addFailsafe(BDD Delta_p, BDD Delta_e, BDD S, BDD Delta_b, BDD Delta_r, BDD f, boolean verbose) {
		BDDFactory factroy = Delta_p.getFactory();
		BDD ms1 = Utils.removeNextState(f.and(Delta_b)); // Line 1

		if (verbose) {
			System.out.println("ms1: ");
			ms1.printSetWithDomains();
		}

		BDD ms2 = ms1.or(Utils.removeNextState(Delta_e.and(Delta_b))); // Line 2

		if (verbose) {
			System.out.println("ms2: ");
			ms2.printSetWithDomains();
		}

		BDD mt = Utils.prime(ms2).or(Delta_b).or(Delta_r); // Line 3

		if (verbose) {
			System.out.println("mt: ");
			if (mt.isOne())
				System.out.println("mt is one");
			mt.printSetWithDomains();
		}

		BDD ms1_1;
		BDD ms2_1;
		do { // Line 4

			if (verbose) {
				System.out.println("New iteration----------------------");
			}
			ms1_1 = factroy.one().and(ms1); // Line 5
			ms2_1 = factroy.one().and(ms2); // Line 6

			BDD fToMs2 = Utils.prime(ms2).and(f); // Line 7
			BDD eToMs1 = Utils.prime(ms1).and(Delta_e); // Line 7
			BDD eAndBad = Delta_e.and(Delta_b); // Line 7
			// States that do not have any transition that is not in mt. All of
			// their transitions are in mt. It is a little tricky :)
			BDD allInMt = Utils.removeNextState(mt.not()).not(); // Line 7

			BDD eToMs1_or_eAndBad_and_allInMt = (eToMs1.or(eAndBad)).and(allInMt);
			BDD newToMs1 = Utils.removeNextState(fToMs2.or(eToMs1_or_eAndBad_and_allInMt));

			if (verbose) {
				System.out.println("fToMs2: ");
				fToMs2.printSetWithDomains();

				System.out.println("eToMs1: ");
				eToMs1.printSetWithDomains();

				System.out.println("eAndBad: ");
				eAndBad.printSetWithDomains();

				System.out.println("allInMt: ");
				allInMt.printSetWithDomains();

				System.out.println("eToMs1_or_eAndBad_and_allInMt: ");
				eToMs1_or_eAndBad_and_allInMt.printSetWithDomains();

				System.out.println("newToMs1: ");
				newToMs1.printSetWithDomains();
			}

			ms1 = ms1.or(newToMs1); // Line 7
			if (verbose) {
				System.out.println("new ms1: ");
				ms1.printSetWithDomains();
			}

			eToMs1 = Utils.prime(ms1).and(Delta_e); // Line 8
			BDD newToMs2 = ms1.or(Utils.removeNextState(eToMs1)); // Line 8
			ms2 = ms2.or(newToMs2); // Line 8

			if (verbose) {
				System.out.println("new ms2: ");
				ms2.printSetWithDomains();
			}

			mt = Utils.prime(ms2).or(mt); // Line 9
			if (verbose) {
				System.out.println("mt: ");
				mt.printSetWithDomains();
			}

		} while (!ms1_1.equals(ms1) || !ms2_1.equals(ms2)); // Line 10
		BDD Delta_p_1 = Delta_p.and(S).and(mt.not()); // Line 11

		BDD[] resultOfClousreAndDeadlocks = ClosureAndDeadlocks(S.and(ms2.not()), Delta_p_1, Delta_e, verbose); // Line
																												// 12
		BDD S_1 = resultOfClousreAndDeadlocks[0]; // Line 12
		Delta_p_1 = resultOfClousreAndDeadlocks[1]; // Line 12

		BDD S_2;
		do { // Line 13
			if (S_1.isZero()) { // Line 14
				if (verbose)
					System.out.println("Not-found");
				return null; // Line 15
			} // Line 16
			S_2 = S_1; // Line 17
			BDD ms3 = Utils.removeNextState(Utils.removeNextState(Delta_e).and(Delta_p))
					.and(Utils.removeNextState(Delta_p_1).not()); // Line 18
			BDD ms4 = Utils.removeNextState(Utils.prime(ms3).and(Delta_e)); // Line
																			// 19

			resultOfClousreAndDeadlocks = ClosureAndDeadlocks(S_1.and(ms4.not()), Delta_p_1, Delta_e, verbose); // Line
																												// 20
			S_1 = resultOfClousreAndDeadlocks[0]; // Line 20
			Delta_p_1 = resultOfClousreAndDeadlocks[1]; // Line 20

		} while (!S_2.equals(S_1)); // Line 21

		Delta_p_1 = (S_1.not().or(Delta_p_1)).and(mt.not()); // Line 22
		BDD[] result = new BDD[2];
		result[0] = S_1;
		result[1] = Delta_p_1;
		return result; // Line 23
	}

	/**
	 * The make sure of the closure and deadlock freedom. 
	 * @param S The BDD representing the invariant to check the closure for it. 
	 * @param Delta_p The BDD representing the program transitions. 
	 * @param Delta_e The BDD representing the environment transitions. 
	 * @param verbose if set to true, the function prints messages
	 * @return An array of BDDs. The first BDD represents the revised invariant. 
	 * The second BDD represents the revised set of program transitions.
	 */
	static BDD[] ClosureAndDeadlocks(BDD S, BDD Delta_p, BDD Delta_e, boolean verbose) { // Line
																							// 24
		BDD S_1;
		BDD Delta_p_1;
		BDDFactory factory = S.getFactory();
		if (verbose) {
			System.out.println("S before remove_deadlock: ");
			S.printSetWithDomains();
		}
		do { // Line 25
			S_1 = factory.one().and(S); // Line 26
			Delta_p_1 = factory.one().and(Delta_p); // Line 27

			S = S.and(Utils.removeNextState(Delta_p.or(Delta_e))); // Line 28
			if (verbose){
				System.out.println("S after Line 28:");
				S.printSetWithDomains();
			}
			S = S.and(Utils.removeNextState(Delta_e.and(S).and(Utils.prime(S.not()))).not()); // Line
			if (verbose){
				System.out.println("S after Line 29:");
				S.printSetWithDomains();
			}																				// 29
			Delta_p = Delta_p.and((S.and(Utils.prime(S.not()))).not()); // Line
																		// 30
		} while (!(S_1.equals(S) && Delta_p_1.equals(Delta_p))); // Line 31
		
		BDD[] result = new BDD[2];
		result[0] = S;
		result[1] = Delta_p;

		return result; // Line 32
	}

	/**
	 * 
	* Add masking fault-tolerance to the given program. 
	 * @param Delta_p The BDD representing the given program. 
	 * @param Delta_e The BDD representing the environment. 
	 * @param S The BDD representing the invariant. 
	 * @param Delta_b The BDD representing the set of bad transitions. 
	 * @param Delta_r The BDD representing the set of program restrictions. 
	 * @param f The BDD representing the set of fault restrictions. 
	 * @param verbose if set to true, the function prints messages during the repair. 
	 * @return An array of BDDs. The first BDD represents the invariant of the revised program. 
	 * 		   The second BDD represents the transitions of the revised program, or </br> null if the repair is impossible.
	 */
	public static BDD[] addMasking(BDD Delta_p, BDD Delta_e, BDD S, BDD Delta_b, BDD Delta_r, BDD f, boolean verbose) {
		BDDFactory factory = Delta_p.getFactory();
		BDD S_1 = factory.one().and(S); // Line 1
		BDD Delta_p_1 = S.and(Delta_p); // Line 2
		BDD ms1 = Utils.removeNextState(f.and(Delta_b)); // Line 3
		if (verbose) {
			System.out.println("ms1: ");
			ms1.printSetWithDomains();
		}

		BDD ms2 = ms1.or(Utils.removeNextState(Delta_e.and(Delta_b))); // Line 4
		if (verbose) {
			System.out.println("ms2: ");
			(Delta_e.and(Delta_b)).printSetWithDomains();

			System.out.println("B&Delta_e:");
			ms2.printSetWithDomains();
		}

		BDD mt = Utils.prime(ms2).or(Delta_b).or(Delta_r); // Line 5
		
		BDD ms1_1;
		BDD ms2_1;
		BDD S_2;
		
		// Overall loop
		do { // Line 6

			if (verbose) {
				System.out.println("New Overall iteration----------------------");
			}
			ms1_1 = factory.one().and(ms1); // Line 7
			ms2_1 = factory.one().and(ms2); // Line 8
			BDD R = factory.one().and(S_1); // Line 9
			S_2 = factory.one().and(S_1); // Line 10

			BDD R_p = factory.zero(); // Line 11
			BDD R_1;
			do { // Line 12
				R_1 = R; // Line 13
				BDD notMtsGoToR = mt.not().and(Utils.prime(R)); // Line 14
				BDD canGoToR = Utils.removeNextState(notMtsGoToR); // Line 14
				BDD R_p_1 = R.not().and(canGoToR); // Line 14
				R_p = R_p.or(R_p_1); // Line 15

				BDD newTransitionsToProgram = R_p_1.and(notMtsGoToR); // Lines
																		// 16-18
				Delta_p_1 = Delta_p_1.or(newTransitionsToProgram); // Line 16-18

				BDD stateGoToMS1ByE = Utils.removeNextState(Utils.prime(ms1).and(Delta_e)); // Line
																							// 19
				BDD statesGoToNotROrRpWithE = Utils.removeNextState(Utils.prime((R.or(R_p)).not()).and(Delta_e)); // Line
																													// 19
				BDD statesGoToRoRRpwithE = Utils.removeNextState(Utils.prime((R.or(R_p)).and(ms1.not())).and(Delta_e.and(Delta_b.not()))); // Line
																											// 19
				BDD statesHavingEandB = Utils.removeNextState(Delta_e.and(Delta_b));
				R = R.or((statesHavingEandB.or(statesGoToNotROrRpWithE).or(stateGoToMS1ByE)).not().and(statesGoToRoRRpwithE.or(R_p))); // Line																											// 20-21
				
			} while (!(R_1.equals(R))); // Line 22
			
			if (verbose){
				System.out.print("Final R: "); 
				R.printSetWithDomains();
				System.out.print("Final R_p: "); 
				R_p.printSetWithDomains();
			}
			ms1 = ms1.or((R.or(R_p)).not()); // Line 23
			ms2 = ms2.or(R.not()); // Line 24

			if (verbose) {
				System.out.println("After convergence part");
				System.out.println("new ms1: ");
				ms1.printSetWithDomains();

				System.out.println("new ms2: ");
				ms2.printSetWithDomains();
			}

			
			BDD ms1_2;
			BDD ms2_2;

			do { // Line 25
				ms1_2 = factory.one().and(ms1); // Line 26
				ms2_2 = factory.one().and(ms2); // Line 27

				BDD fToMs2 = Utils.prime(ms2).and(f); // Line 28
				BDD eToMs1 = Utils.prime(ms1).and(Delta_e); // Line 28
				BDD eAndBad = Delta_e.and(Delta_b); // Line 28
				BDD deadlocks = Utils.removeNextState(Delta_p_1).not(); // Line
																		// 28
				BDD eToMs1_or_eAndBad_and_deadlock = Utils.removeNextState((eToMs1.or(eAndBad))).and(deadlocks); // Line
																							// 28
				BDD newToMs1 = Utils.removeNextState(fToMs2).or(eToMs1_or_eAndBad_and_deadlock); // Line
																									// 28
					
				
				if (verbose) {
					System.out.println("fToMs2: ");
					fToMs2.printSetWithDomains();

					System.out.println("eToMs1: ");
					eToMs1.printSetWithDomains();

					System.out.println("eAndBad: ");
					eAndBad.printSetWithDomains();

					System.out.println("deadlocks: ");
					deadlocks.printSetWithDomains();

					System.out.println("eToMs1_or_eAndBad_and_deadlock: ");
					eToMs1_or_eAndBad_and_deadlock.printSetWithDomains();

					System.out.println("newToMs1: ");
					newToMs1.printSetWithDomains();
				}

				ms1 = ms1.or(newToMs1); // Line 28
				if (verbose) {
					System.out.println("new ms1: ");
					ms1.printSetWithDomains();
				}

				eToMs1 = Utils.prime(ms1).and(Delta_e); // Line 29
				
				if (verbose){
					System.out.println("eToMS1:");
					eToMs1.printSetWithDomains();
				}
				BDD newToMs2 = ms1.or(Utils.removeNextState(eToMs1)); // Line 29
				ms2 = ms2.or(newToMs2); // Line 29
				if (verbose) {
					System.out.println("new ms2: ");
					ms2.printSetWithDomains();
				}

				mt = Utils.prime(ms2).or(mt); // Line 30
			} while (!(ms1_2.equals(ms1) && ms2_2.equals(ms2))); // Line 31

			Delta_p_1 = Delta_p_1.and(mt.not()); // Line 32
			BDD[] resultOfClousreAndDeadlock = ClosureAndDeadlocks(S_1.and(ms2.not()), Delta_p_1, Delta_e, verbose); // Line
																														// 33

			S_1 = resultOfClousreAndDeadlock[0]; // Line 33
			Delta_p_1 = resultOfClousreAndDeadlock[1]; // Line 33

			BDD S_3;
			do { // Line 34
				if (S_1.isZero()) { // Line 35
					System.out.println("Adding masking failed, Not-found");
					return null; // Line 36
				} // Line 37
				S_3 = S_1; // Line 38
				BDD ms3 = Utils.removeNextState(Utils.removeNextState(Delta_e).and(Delta_p))
						.and(Utils.removeNextState(Delta_p_1).not()); // Line 39
				BDD ms4 = Utils.removeNextState(Utils.prime(ms3).and(Delta_e)); // Line
				if (verbose){
					System.out.print("ms4:");
					ms4.printSetWithDomains();
				}
				BDD[] resultOfClousreAndDeadlocks = ClosureAndDeadlocks(S_1.and(ms4.not()), Delta_p_1, Delta_e,
						verbose); // Line 41
				S_1 = resultOfClousreAndDeadlocks[0]; // Line 41
				Delta_p_1 = resultOfClousreAndDeadlocks[1]; // Line 41

			} while (!S_3.equals(S_1)); // Line 42
			// end of overall loop
		} while (!(S_2.equals(S_1) && ms1_1.equals(ms1) && ms2_1.equals(ms2))); // Line
																				// 43

		BDD[] results = new BDD[2];
		results[0] = S_1;
		results[1] = Delta_p_1;

		return results; // Line 44
	}
}

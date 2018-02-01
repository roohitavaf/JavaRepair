package other;

import java.util.Arrays;

import edu.msu.cse.javaRepair.Repairer;
import edu.msu.cse.javaRepair.Utils;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class FailsafeTest {

	static int size = 4;
	public static void main(String[] args) {
		
		BDDFactory factory = createFactory();
		BDD invaraintBDD = createInvariantBDD(factory);
		System.out.println("invaraintBDD: ");
		invaraintBDD.printSetWithDomains();
		
		BDD programBDD = createProgramBDD(factory);
		System.out.println("programBDD: ");
		programBDD.printSetWithDomains();
		
		BDD environmentBDD = createEnvironmentBDD(factory);
		System.out.println("environmentBDD: ");
		environmentBDD.printSetWithDomains();
		
		BDD faultsBDD =  createFaultsBDD(factory);
		System.out.println("faultsBDD: ");
		faultsBDD.printSetWithDomains();
		
		
		BDD programRestrictionBDD = createProgramRestrictionBDD(factory);
		System.out.println("programRestrictionBDD.not(): ");
		programRestrictionBDD.not().printSetWithDomains();
		
		BDD badTransitionsBDD = createBadTransitionsBDD(factory);
		System.out.println("badTransitionsBDD: ");
		badTransitionsBDD.printSetWithDomains();
		
		BDD[] result = Repairer.addFailsafe(invaraintBDD, programBDD, environmentBDD, faultsBDD, programRestrictionBDD, badTransitionsBDD, true);
		
		if (result != null){ 
			BDD revisedProgram = result[0];
			BDD revisedInvariant = result[1]; 
			
			revisedProgram.printSetWithDomains();
			revisedInvariant.printSetWithDomains();
		}
	}

	private static BDD createBadTransitionsBDD(BDDFactory factory) {
		BDD bdd = (factory.getDomain(Utils.getVInx(0)).ithVar(size-2).and(factory.getDomain(Utils.getPrimeVInx(0)).ithVar(size-1)));
		return bdd;
	}

	private static BDD createProgramRestrictionBDD(BDDFactory factory) {
		BDD bdd = (factory.getDomain(Utils.getVInx(0)).ithVar(size-1).not().and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(0), Utils.getPrimeVInx(0))));
		bdd = bdd.or(factory.getDomain(Utils.getVInx(0)).ithVar(0).not().and(Utils.oneDomainIsLargerByOne(factory, Utils.getPrimeVInx(0), Utils.getVInx(0))));
		bdd = bdd.or(factory.getDomain(Utils.getVInx(0)).ithVar(0).and(factory.getDomain(Utils.getPrimeVInx(0)).ithVar(0)));
		return bdd.not();
	}

	private static BDD createFaultsBDD(BDDFactory factory) {
		BDD bdd = (factory.getDomain(Utils.getVInx(0)).ithVar(0).and(factory.getDomain(Utils.getPrimeVInx(0)).ithVar(1)));
		return bdd;
	}

	private static BDD createEnvironmentBDD(BDDFactory factory) {		
		BDD bdd = (factory.getDomain(Utils.getVInx(0)).ithVar(size - 1).not()).and(factory.getDomain(Utils.getVInx(0)).ithVar(0).not());
		bdd = bdd.and(Utils.oneDomainIsLargerByOne(factory, Utils.getVInx(0), Utils.getPrimeVInx(0)));
		return bdd;
	}

	private static BDD createProgramBDD(BDDFactory factory) {
		return factory.getDomain(Utils.getVInx(0)).ithVar(0).and(factory.getDomain(Utils.getPrimeVInx(0)).ithVar(0));
	}

	private static BDD createInvariantBDD(BDDFactory factory) {
		return factory.getDomain(Utils.getVInx(0)).ithVar(0);
	}

	private static BDDFactory createFactory() {
		BDDFactory factory = JFactory.init(2000000, 200000);
		int[] domainSizes = new int[2];
		Arrays.fill(domainSizes, size);
		factory.extDomain(domainSizes);
		
		factory.getDomain(0).setName("x");
		factory.getDomain(1).setName("x'");
		
		return factory;
	}

}

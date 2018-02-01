package other;

import java.util.Arrays;

import edu.msu.cse.javaRepair.Utils;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class JavaBDDTest {

	public static void main (String args[]){
		BDDFactory factory =JFactory.init(2000000, 200000);
		
		int[] domainSizes = new int[4];
		Arrays.fill(domainSizes, 4);
		factory.extDomain(domainSizes);
		
		BDD bdd = Utils.secondDomainIsAtMostFirstDomainPlusX(factory, 0, 1, 2);
		bdd.printSetWithDomains();
		
		BDD fbdd = factory.zero();
		

	}
}

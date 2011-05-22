package de.tudresden.inf.lat.gel;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;

import de.tudresden.inf.lat.jcel.core.axiom.complex.ComplexIntegerAxiom;
import de.tudresden.inf.lat.jcel.core.datatype.*;
import de.tudresden.inf.lat.jcel.core.graph.IntegerBinaryRelation;

/**
 * The LcsProcessor extends the CelProcessor by additionally computing the least common subsumer of the provided input concepts
 * in the post processing step.
 * 
 * @author Andreas Ecke
 */
public class LcsProcessor extends de.tudresden.inf.lat.jcel.core.algorithm.cel.CelProcessor {
	private int[] concepts;
	private int k;
	private IntegerClassExpression lcs = null;
	private boolean simplify;
	
	/**
	 * Creates a new LcsProcessor that will compute the least common subsumer of the provided input concepts for the given set of axioms.
	 * @param axioms the translated axioms of the input ontology including axioms for the input concepts 
	 * @param concepts input concept description
	 * @param k role-depth bound
	 */
	LcsProcessor(Set<ComplexIntegerAxiom> axioms, int[] concepts, int k, boolean simplify) {
		super(axioms);	
		this.concepts = concepts;
		this.k = k;
		this.simplify = simplify;
	}
	
	
	/**
	 * The classification is done, so we can compute the least common subsumer now.
	 */
	protected void postProcess() {
		boolean csize = false;
		// create a new minimizer
		Minimizer m = new Minimizer(this.getIdGenerator(), this.getClassGraph(), this.getObjectPropertyGraph(), getRelation());
		
		// compute the least common subsumer
		Date start = new Date();
		lcs = kLcsRecursive(concepts, k);
		System.out.println("Construction of lcs: " + ((new Date()).getTime() - start.getTime()) + "ms");
		
		// remove temporary names and simplify the result
		start = new Date();
		lcs = m.removeTemporaryNames(lcs);
		if (simplify) {
			if (csize) {
				System.out.println("size before simplification: " + size(lcs));
			}
			lcs = m.minimize(lcs);
			if (csize) {
				System.out.println("size after simplification: " + size(lcs));
			}
			System.out.println("Minimization: " + ((new Date()).getTime() - start.getTime()) + "ms");
		}
		
		// now run the CelProcessor postProcess, which will clean up
		super.postProcess();
	}
	
	// computes the size of a concept description as the number of concept and role names
	private int size(IntegerClassExpression e) {
		if (e instanceof IntegerClass) {
			return 1;
		} else if (e instanceof IntegerObjectIntersectionOf) {
			int i = 0;
			for (IntegerClassExpression e1 : ((IntegerObjectIntersectionOf) e).getOperands()) {
				i += size(e1);
			}
			return i;
		} else {
			return 1 + size(((IntegerObjectSomeValuesFrom)e).getFiller());
		}
	}

	/**
	 * Recursively computes the least common subsumer for the given concept names.
	 * @param concepts concept names
	 * @param k role-depth bound
	 * @return least common subsumer for the concepts
	 */
	private IntegerClassExpression kLcsRecursive(int[] concepts, int k) {
		// test if any input concept subsumes all other concepts
		for (int i : concepts) {
			boolean t = true;
			for (int j : concepts) {
				if (i != j && !super.getClassGraph().getSubsumers(j).contains(i)) {
					t = false;
					break;
				}
			}
			// then return it
			if (t) return new IntegerClass(i);
		}
		
		// compute common names in input concepts
		Collection<Integer> commonNames = new HashSet<Integer>(super.getClassGraph().getSubsumers(concepts[0]));
		for (int i : concepts) {
			if (i != concepts[0]) {
				commonNames.retainAll(super.getClassGraph().getSubsumers(i));
			}
		}
		
		// remove all names that are proper subsumers of other names in the set - simplifies the result a bit
		/*Collection<Integer> cn = new HashSet<Integer>(commonNames);
		for (Integer i : commonNames) {
			if (!cn.contains(i)) continue;
			for (Integer j : super.getClassGraph().getSubsumers(i)) {
				if (i != j && cn.contains(j)) {
					cn.remove(j);
				}
			}
		}*/
		
		// add common names to the intersectionSet
		Set<IntegerClassExpression> intersectionSet = new HashSet<IntegerClassExpression>();
		for (Integer i : commonNames) {
			intersectionSet.add(new IntegerClass(i));
		}
		if (k==0) {
			// if the role-depth bound is 0, return conjunction of the common names
			if (intersectionSet.size() == 1) {
				return intersectionSet.iterator().next();
			}
			return new IntegerObjectIntersectionOf(intersectionSet);
		} else {
			// else compute the r-successor product for each relation r, add everything to the intersectionSet
			for (Integer relation : super.getRelationIdSet()) {
				if (relation > super.getIdGenerator().getFirstObjectPropertyId() || relation < 2) continue;
				intersectionSet.addAll(product(relation, k-1, concepts, 0, null));
			}
			// return the conjunction of all concepts in the intersectionSet
			if (intersectionSet.size() == 1) {
				return intersectionSet.iterator().next();
			} else {
				return new IntegerObjectIntersectionOf(intersectionSet);
			}
		}
	}
	
	/**
	 * Computes the product of r-successors of concepts and calls kLcsRecursive for each tuple.
	 * @param relation relation r
	 * @param newk role-depth bound for the calls to kLcsRecursive
	 * @param oldconcepts concept names for which the product is computed
	 * @param index index of the concept to process next - must initially be 0
	 * @param newconcepts r-successors for the already processed concepts - must initially be null or int[oldconcepts.length]
	 * @return
	 */
	private HashSet<IntegerClassExpression> product(int relation, int newk, int[] oldconcepts, int index, int[] newconcepts) {
		if (newconcepts == null) {
			newconcepts = new int[oldconcepts.length];
		}
		HashSet<IntegerClassExpression> prod = new HashSet<IntegerClassExpression>();
		if (index >= oldconcepts.length) {
			// processed all concepts and got on tuple of the product set
			// call kLcsRecursive with these new concepts and return the result (wrapped in \exists r...)
			prod.add(new IntegerObjectSomeValuesFrom(new IntegerObjectProperty(relation), kLcsRecursive(newconcepts, newk)));
		} else {
			// process the next concept - recursively call product for for each r-successor of that concept
			for (Integer j : super.getRelation(relation).getByFirst(oldconcepts[index])) {
				newconcepts[index] = j;
				prod.addAll(product(relation, newk, oldconcepts, index+1, newconcepts));
			}
		}
		return prod;
	}

	/**
	 * Returns the computed least common subsumer.
	 * @return least common subsumer - or null if it is not done yet
	 */
	public IntegerClassExpression getLcs() {
		return lcs;
	}

	private Map<Integer, IntegerBinaryRelation> getRelation() {
		Map<Integer, IntegerBinaryRelation> m = new TreeMap<Integer, IntegerBinaryRelation>();
		for (Integer relation : super.getRelationIdSet()) {
			if (relation > super.getIdGenerator().getFirstObjectPropertyId() || relation < 2) continue;
				m.put(relation, super.getRelation(relation));
		}
		return m;
	}
}

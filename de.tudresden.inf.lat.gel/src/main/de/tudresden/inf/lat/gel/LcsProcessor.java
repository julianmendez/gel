package de.tudresden.inf.lat.gel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;

import de.tudresden.inf.lat.jcel.ontology.axiom.complex.ComplexIntegerAxiom;
import de.tudresden.inf.lat.jcel.ontology.axiom.extension.IntegerOntologyObjectFactory;
import de.tudresden.inf.lat.jcel.ontology.datatype.*;
import de.tudresden.inf.lat.jcel.core.graph.IntegerBinaryRelation;

/**
 * The LcsProcessor extends the CelProcessor by additionally computing the least common subsumer of the provided input concepts
 * in the post processing step.
 * 
 * @author Andreas Ecke
 */
public class LcsProcessor extends de.tudresden.inf.lat.jcel.core.algorithm.cel.CelProcessor {
	private int a, b;
	private int k;
	private IntegerClassExpression lcs = null;
	private boolean simplify;
	private IntegerOntologyObjectFactory factory;
	private boolean opti1, opti2;
	
	/**
	 * Creates a new LcsProcessor that will compute the least common subsumer of the provided input concepts for the given set of axioms.
	 * @param axioms the translated axioms of the input ontology including axioms for the input concepts 
	 * @param concepts input concept description
	 * @param k role-depth bound
	 */
	LcsProcessor(Set<ComplexIntegerAxiom> axioms, int a, int b, int k, boolean simplify, boolean opti1, boolean opti2, IntegerOntologyObjectFactory factory) {
		super(axioms, factory);
		this.a = a;
		this.b = b;
		this.k = k;
		this.simplify = simplify;
		this.factory = factory;
		this.opti1 = opti1;
		this.opti2 = opti2;
	}
	
	
	/**
	 * The classification is done, so we can compute the least common subsumer now.
	 */
	protected void postProcess() {
		boolean csize = true;
		// create a new minimizer
		Minimizer m = new Minimizer(this.getIdGenerator(), this.getClassGraph(), this.getObjectPropertyGraph(), getRelation(), this.getExtendedOntology(), factory);

		final DecimalFormat df = new DecimalFormat( "0.0" );
		// compute the least common subsumer
		long start = System.nanoTime();
		lcs = kLcsRecursive(a, b, k);
		System.out.println("Construction of lcs: " + df.format((System.nanoTime() - start) / 1000000.0) + "ms");
		
		// remove temporary names and simplify the result
		start = System.nanoTime();
		lcs = m.removeTemporaryNames(lcs);
		if (simplify) {
			if (csize) {
				System.out.println("size before simplification: " + size(lcs));
			}
			lcs = m.minimize(lcs);
			if (csize) {
				System.out.println("size after simplification: " + size(lcs));
			}
			System.out.println("Minimization: " + df.format((System.nanoTime() - start) / 1000000.0) + "ms");
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
	private IntegerClassExpression kLcsRecursive(int a, int b, int k) {
		// test if any input concept subsumes all other concepts
		if (opti1) {
			if (super.getClassGraph().getSubsumers(a).contains(b) && !this.getIdGenerator().isAuxiliary(b)) {
				return factory.getDataTypeFactory().createClass(b);
			}
			if (super.getClassGraph().getSubsumers(b).contains(a) && !this.getIdGenerator().isAuxiliary(a)) {
				return factory.getDataTypeFactory().createClass(a);
			}
		}
		
		// compute common names in input concepts
		Collection<Integer> commonNames = new HashSet<Integer>(super.getClassGraph().getSubsumers(a));
		commonNames.retainAll(super.getClassGraph().getSubsumers(b));
		
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
			intersectionSet.add(factory.getDataTypeFactory().createClass(i));
		}
		if (k==0) {
			// if the role-depth bound is 0, return conjunction of the common names
			if (intersectionSet.size() == 1) {
				return intersectionSet.iterator().next();
			}
			return factory.getDataTypeFactory().createObjectIntersectionOf(intersectionSet);
		} else {
			if (opti2) {
				Set<Successor> sa = new HashSet<Successor>();
				Set<Successor> sb = new HashSet<Successor>();
				for (Integer relation : super.getRelationIdSet()) {
					if (super.getIdGenerator().isAuxiliary(relation) || relation < IntegerEntityManager.firstUsableIdentifier) continue;
					for (Integer i : super.getRelation(relation).getByFirst(a)) {
						sa.add(new Successor(relation, i));
					}
					for (Integer j : super.getRelation(relation).getByFirst(b)) {
						sb.add(new Successor(relation, j));
					}
				}
				sa = removeRedundant(sa);
				sb = removeRedundant(sb);
				for (Successor s1 : sa) {
					for (Successor s2 : sb) {
						Set<Integer> minimalCommonRelations = findMinComRel(s1.relation, s2.relation);
						if (minimalCommonRelations.size() == 0) continue;
						for (Integer relation : minimalCommonRelations) {
							intersectionSet.add(factory.getDataTypeFactory().createObjectSomeValuesFrom(factory.getDataTypeFactory().createObjectProperty(relation), kLcsRecursive(s1.concept, s2.concept, k-1)));
						}
					}
				}
			} else {
				// else compute the r-successor product for each relation r, add everything to the intersectionSet
				for (Integer relation : super.getRelationIdSet()) {
					if (super.getIdGenerator().isAuxiliary(relation) || relation < IntegerEntityManager.firstUsableIdentifier) continue;
					for (Integer i : super.getRelation(relation).getByFirst(a)) {
						for (Integer j : super.getRelation(relation).getByFirst(b)) {
							intersectionSet.add(factory.getDataTypeFactory().createObjectSomeValuesFrom(factory.getDataTypeFactory().createObjectProperty(relation), kLcsRecursive(i, j, k-1)));
						}
					}
					//intersectionSet.addAll(product(relation, k-1, concepts, 0, null));
				}
			}
			// return the conjunction of all concepts in the intersectionSet
			if (intersectionSet.size() == 1) {
				return intersectionSet.iterator().next();
			} else {
				return factory.getDataTypeFactory().createObjectIntersectionOf(intersectionSet);
			}
		}
	}
	
	private Set<Successor> removeRedundant(Set<Successor> successors) {
		Set<Successor> sl = new HashSet<Successor>(successors);
		for (Successor s1 : successors) {
			if (!sl.contains(s1)) continue;
			for (Successor s2 : successors) {
				if (s1 != s2 && sl.contains(s2) && 
						this.getObjectPropertyGraph().getSubsumers(s1.relation).contains(s2.relation) &&
						this.getClassGraph().getSubsumers(s1.concept).contains(s2.concept)) {
					sl.remove(s2);
				}
			}
		}
		return sl;
	}
	
	private Set<Integer> findMinComRel(Integer r1, Integer r2) {
		// Find common subsumers of r1 and r2
		Set<Integer> common = new HashSet<Integer>(this.getObjectPropertyGraph().getSubsumers(r1));
		common.retainAll(this.getObjectPropertyGraph().getSubsumers(r2));
		common.remove(IntegerEntityManager.topObjectPropertyId);
		
		// Extract minimal ones
		Set<Integer> minCommon = new HashSet<Integer>(common);
		for (Integer t1 : common) {
			if (!minCommon.contains(t1)) continue;
			for (Integer t2 : common) {
				if (t1 != t2 && minCommon.contains(t2) && this.getObjectPropertyGraph().getSubsumers(t1).contains(t2)) {
					minCommon.remove(t2);
				}
			}
		}
		
		return minCommon;
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
			if (super.getIdGenerator().isAuxiliary(relation) || relation < IntegerEntityManager.firstUsableIdentifier) continue;
				m.put(relation, super.getRelation(relation));
		}
		return m;
	}
	
	private class Successor {
		public final Integer relation;
		public final Integer concept;

		public Successor(Integer relation, Integer concept) {
			if (relation == null || concept == null) throw new NullPointerException();
			this.relation = relation;
			this.concept = concept;
		}
		
	    public int hashCode() {
	        return 31 * relation.hashCode() + concept.hashCode();
	    }

	    public boolean equals(Object other) {
			if (other instanceof Successor) {
				Successor so = (Successor)other;
				return relation == so.relation && concept == so.concept;
			} else return false;
	    }
	}
}

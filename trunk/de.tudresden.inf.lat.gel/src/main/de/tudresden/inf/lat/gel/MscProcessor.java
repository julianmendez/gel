package de.tudresden.inf.lat.gel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.tudresden.inf.lat.jcel.ontology.axiom.complex.ComplexIntegerAxiom;
import de.tudresden.inf.lat.jcel.ontology.axiom.extension.IntegerOntologyObjectFactory;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerClassExpression;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerEntityManager;
import de.tudresden.inf.lat.jcel.core.graph.IntegerBinaryRelation;

/**
 * The MscProcessor extends the CelProcessor by additionally computing the most specific concept of the provided input individual
 * in the post processing step.
 * 
 * @author Andreas Ecke
 */
public class MscProcessor extends de.tudresden.inf.lat.jcel.core.algorithm.cel.CelProcessor {
	private int k;
	private int individual;
	private IntegerClassExpression msc = null;
	private boolean simplify;
	private IntegerOntologyObjectFactory factory;

	/**
	 * Creates a new MscProcessor that will compute the most specific concept of the provided input individual for the given set of axioms.
	 * @param axioms the translated axioms of the input ontology
	 * @param individual individual ID
	 * @param k role-depth bound
	 */
	public MscProcessor(Set<ComplexIntegerAxiom> axioms, int individual, int k, boolean simplify, IntegerOntologyObjectFactory factory) {
		super(axioms, factory);
		this.individual = individual;
		this.k = k;
		this.simplify = simplify;
		this.factory = factory;
	}

	/**
	 * The classification is done, so we can compute the most specific concept now.
	 */
	protected void postProcess() {
		// create a new minimizer
		Minimizer m = new Minimizer(this.getIdGenerator(), this.getClassGraph(), this.getObjectPropertyGraph(), this.getRelation(), this.getExtendedOntology(), factory);
		
		// get ID of the nominal that represents the individual
		int i = super.getIdGenerator().getAuxiliaryNominal(individual);

		// compute the most specific concept
		msc = kMscRecursive(i, k);

		// remove temporary names and simplify the result
		msc = m.removeTemporaryNames(msc);
		if (simplify) {
			msc = m.minimize(msc);
		}

		// now run the CelProcessor postProcess, which will clean up
		super.postProcess();
	}

	/**
	 * Recursively computes the most specific concept for the given concept names.
	 * @param ind concept or nominal ID
	 * @param k role-depth bound
	 * @return traversal-concept of the given ID
	 */
	private IntegerClassExpression kMscRecursive(int ind, int k) {
		// get all subsumers of ind
		Collection<Integer> commonNames = super.getClassGraph().getSubsumers(ind);
		
		// add all concepts ID them to then intersectionSet
		Set<IntegerClassExpression> intersectionSet = new HashSet<IntegerClassExpression>();
		for (Integer i : commonNames) {
			// test that we don't have a nominal - but that should not happen
			if (!super.getIdGenerator().getAuxiliaryNominals().contains(i)) {
				intersectionSet.add(factory.getDataTypeFactory().createClass(i));
			}
		}
		if (k==0) {
			// if the role-depth bound is 0, return conjunction of the subsumers
			if (intersectionSet.size() == 1) {
				return intersectionSet.iterator().next();
			}
			return factory.getDataTypeFactory().createObjectIntersectionOf(intersectionSet);
		} else {
			// otherwise traverse all r-successors and add existentially restriction for the recursively computed concepts descriptions to the intersection set
			for (Integer relation : super.getRelationIdSet()) {
				if (super.getIdGenerator().isAuxiliary(relation) || relation < IntegerEntityManager.firstUsableIdentifier) continue;
				for (int i : super.getRelation(relation).getByFirst(ind)) {
					//if (k == this.k && i != ind) {
					intersectionSet.add(factory.getDataTypeFactory().createObjectSomeValuesFrom(factory.getDataTypeFactory().createObjectProperty(relation), kMscRecursive(i, k-1)));
					//}
				}
			}
			// return conjunction of all elements in the intersectionSet
			if (intersectionSet.size() == 1) {
				return intersectionSet.iterator().next();
			} else {
				return factory.getDataTypeFactory().createObjectIntersectionOf(intersectionSet);
			}
		}
	}

	/**
	 * Returns the computed most specific concept.
	 * @return most specific concept - or null if it is not done yet
	 */
	public IntegerClassExpression getMsc() {
		return msc;
	}

	private Map<Integer, IntegerBinaryRelation> getRelation() {
		Map<Integer, IntegerBinaryRelation> m = new TreeMap<Integer, IntegerBinaryRelation>();
		for (Integer relation : super.getRelationIdSet()) {
			if (super.getIdGenerator().isAuxiliary(relation) || relation < IntegerEntityManager.firstUsableIdentifier) continue;
				m.put(relation, super.getRelation(relation));
		}
		return m;
	}
}

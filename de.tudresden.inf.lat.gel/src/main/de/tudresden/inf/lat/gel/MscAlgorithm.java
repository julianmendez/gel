package de.tudresden.inf.lat.gel;

import java.util.HashSet;
import java.util.Set;

import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerClassExpression;

public class MscAlgorithm extends Generalization {
	/**
	 * Computes the role-depth bounded least common subsumer of the given
	 * concepts and the role-depth k.
	 * 
	 * @param concepts A list of all concepts from which to compute the lcs, as
	 *        Integers
	 * @param k The role-depth bound
	 * @return the role-depth bounded least common subsumer
	 */
	public IntegerClassExpression mostSpecificConcept(int element, int k) {
		// get all concept names that the individual is part of
		Set<IntegerClassExpression> conjunctionSet = new HashSet<IntegerClassExpression>();
		for (Integer concept : classGraph.getSubsumers(element)) {
			if (!entityManager.isAuxiliary(concept)) {
				conjunctionSet.add(dataTypeFactory.createClass(concept));
			}
		}

		// visit all successors
		for (Successor successor : getSuccessors(element)) {
			IntegerClassExpression recursiveMsc = mostSpecificConcept(successor.getConcept(), k - 1);
			// add existential restrions of the recursive lcs for each minimal
			// relation
			conjunctionSet.add(dataTypeFactory.createObjectSomeValuesFrom(dataTypeFactory.createObjectProperty(successor.getRole()), recursiveMsc));
		}

		// return the conjunction of all concept expressions in the
		// conjunctionSet
		if (conjunctionSet.size() == 1) {
			return conjunctionSet.iterator().next();
		} else {
			return dataTypeFactory.createObjectIntersectionOf(conjunctionSet);
		}
	}
}

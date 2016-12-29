package de.tudresden.inf.lat.gel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import de.tudresden.inf.lat.jcel.coreontology.datatype.IntegerEntityManager;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerClassExpression;

/**
 * Class to compute least common subsumers from the completion graph computed by
 * a processor.
 * 
 * @author Andreas Ecke
 */
public class LcsAlgorithm extends Generalization {
	/**
	 * Computes the role-depth bounded least common subsumer of the given
	 * concepts and the role-depth k.
	 * 
	 * @param concepts A list of all concepts from which to compute the lcs, as
	 *        Integers
	 * @param k The role-depth bound
	 * @return the role-depth bounded least common subsumer
	 */
	public IntegerClassExpression leastCommonSubsumer(List<Integer> concepts, int k) {
		// check if one of the concepts subsumes all other concepts (and is not
		// auxiliary!)
		for (Integer concept : concepts) {
			if (!entityManager.isAuxiliary(concept) && subsumesAll(concept, concepts)) {
				return dataTypeFactory.createClass(concept);
			}
		}
		
		// compute concept names that subsume all input concepts
		Set<IntegerClassExpression> conjunctionSet = getCommonNames(concepts);

		// traverse all roles, if k is larger than 0
		if (k > 0) {
			// construct a set of all successors for each concept
			List<List<Successor>> successors = new ArrayList<>(concepts.size());
			for (int concept : concepts) {
				successors.add(getSuccessors(concept));
			}

			// iterate through the product of all successors
			for (List<Successor> successorTuple : new SuccessorProduct(successors)) {
				Set<Integer> minimalRelations = findMinimalCommonRelations(successorTuple);
				if (minimalRelations.size() > 0) {

					// recursively compute the lcs and add the resulting
					// existential restriction to the conjunction set
					List<Integer> newConcepts = new ArrayList<>();
					for (Successor s : successorTuple) {
						newConcepts.add(s.getConcept());
					}
					IntegerClassExpression recursiveLcs = leastCommonSubsumer(newConcepts, k - 1);

					// add existential restrions of the recursive lcs for each
					// minimal relation
					for (Integer relation : minimalRelations) {
						conjunctionSet.add(dataTypeFactory.createObjectSomeValuesFrom(dataTypeFactory.createObjectProperty(relation), recursiveLcs));
					}
				}
			}
		}

		// return the conjunction of all concept expressions in the
		// conjunctionSet
		if (conjunctionSet.size() == 1) {
			return conjunctionSet.iterator().next();
		} else {
			return dataTypeFactory.createObjectIntersectionOf(conjunctionSet);
		}
	}

	/**
	 * Computes all minimal relations that subsume all of the input relations
	 * (given as successors).
	 * 
	 * @param successorTuple Tuple of the successors for which to extract all
	 *        minimal relations subsuming all roles from the successors
	 * @return The set of all minimal roles
	 */
	private Set<Integer> findMinimalCommonRelations(List<Successor> successorTuple) {
		// Find common subsumers of all relations
		Set<Integer> commonRelations = new HashSet<>(objectPropertyGraph.getSubsumers(successorTuple.get(0).getRole()));
		for (int i = 1; i < successorTuple.size(); i++) {
			commonRelations.retainAll(objectPropertyGraph.getSubsumers(successorTuple.get(i).getRole()));
		}
		commonRelations.remove(IntegerEntityManager.topObjectPropertyId);

		// Extract minimal ones
		Set<Integer> mininmalCommonRelations = new HashSet<>(commonRelations);
		for (Integer r1 : commonRelations) {
			if (!mininmalCommonRelations.contains(r1))
				continue;
			for (Integer r2 : commonRelations) {
				if (r1 != r2 && mininmalCommonRelations.contains(r2) && objectPropertyGraph.getSubsumers(r1).contains(r2)) {
					mininmalCommonRelations.remove(r2);
				}
			}
		}

		return mininmalCommonRelations;
	}

	/**
	 * Tests whether the given concept subsumes all other concepts.
	 * 
	 * @param concept The concept that may subsume the the other concepts, as
	 *        Integer
	 * @param otherConcepts List of other concepts, which should be tested for
	 *        subsumption
	 * @return true, if the concept subsumes all other concepts; false otherwise
	 */
	private boolean subsumesAll(Integer concept, List<Integer> otherConcepts) {
		for (Integer other : otherConcepts) {
			if (!classGraph.getSubsumers(other).contains(concept)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the set of all common names, i.e., concept names that subsume all
	 * of the input concepts.
	 * 
	 * @param concepts List of input concept names, as Integers
	 * @return List of all common names, as IntegerClasses
	 */
	private Set<IntegerClassExpression> getCommonNames(List<Integer> concepts) {
		// compute concept names that subsume all input concepts
		Set<Integer> commonSubsumers = new HashSet<>(classGraph.getSubsumers(concepts.get(0)));
		for (int i = 1; i < concepts.size(); i++) {
			commonSubsumers.retainAll(classGraph.getSubsumers(concepts.get(i)));
		}

		// wrap all common names as owl classes
		Set<IntegerClassExpression> commonNames = new HashSet<>();
		for (Integer i : commonSubsumers) {
			if (!entityManager.isAuxiliary(i)) {
				commonNames.add(dataTypeFactory.createClass(i));
			}
		}

		return commonNames;
	}

	/**
	 * Iterates through all tuples in the cross product of the given successor
	 * sets.
	 */
	private class SuccessorProduct implements Iterator<List<Successor>>,Iterable<List<Successor>> {
		private List<List<Successor>> successors;
		private int[] indices;
		private int[] lengths;
		private boolean empty = false;
		private List<Successor> result;
		private List<Successor> unmodifiableView;

		public SuccessorProduct(List<List<Successor>> successors) {
			this.successors = successors;
			indices = new int[successors.size()];
			lengths = new int[successors.size()];
			result = new ArrayList<>(successors.size());
			unmodifiableView = Collections.unmodifiableList(result);
			for (int i = 0; i < successors.size(); i++) {
				indices[i] = 0;
				lengths[i] = successors.get(i).size() - 1;
				if (lengths[i] < 0) {
					empty = true;
					result.add(null);
				} else {
					result.add(successors.get(i).get(0));
				}
			}
			indices[0] = -1;
		}

		@Override
		public boolean hasNext() {
			if (empty) {
				return false;
			}
			for (int i = 0; i < indices.length; i++) {
				if (indices[i] < lengths[i]) {
					return true;
				}
			}
			return false;
		}

		@Override
		public List<Successor> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			for (int i = 0; i < indices.length; i++) {
				if (indices[i] < lengths[i]) {
					indices[i]++;
					result.set(i, successors.get(i).get(indices[i]));
					break;
				} else {
					indices[i] = 0;
					result.set(i, successors.get(i).get(indices[i]));
				}
			}
			return unmodifiableView;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<List<Successor>> iterator() {
			return this;
		}
	}
}

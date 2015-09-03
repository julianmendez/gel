package de.tudresden.inf.lat.gel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.tudresden.inf.lat.jcel.core.graph.IntegerBinaryRelation;
import de.tudresden.inf.lat.jcel.core.graph.IntegerSubsumerGraph;
import de.tudresden.inf.lat.jcel.coreontology.datatype.IntegerEntityManager;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerDataTypeFactory;

/**
 * This class provided common methods and functions used by generalization inferences. Those generalizations
 * should extend this class.
 * 
 * @author Andreas Ecke
 */
public abstract class Generalization {
	protected IntegerEntityManager entityManager;
	protected IntegerSubsumerGraph classGraph;
	protected IntegerSubsumerGraph objectPropertyGraph;
	protected IntegerDataTypeFactory dataTypeFactory;
	protected Map<Integer, IntegerBinaryRelation> relationGraph;

	/**
	 * Loads the data structures from the GelProcessor. These data structures
	 * include: The class and object property graph, the set of relations, the
	 * entity manager and the data type factory. This method should only be
	 * called after the processing of the GelProcessor terminated, as otherwise
	 * the subsumer graphs are not complete.
	 * 
	 * @param processor The GelProcessor from which to load all the data structures
	 */
	public void loadDataStructures(GelProcessor processor) {
		entityManager = processor.getEntityManagerX();
		classGraph = processor.getClassGraphX();
		objectPropertyGraph = processor.getObjectPropertyGraphX();
		dataTypeFactory = processor.getIntegerOntologyObjectFactory().getDataTypeFactory();
		relationGraph = processor.getRelationGraphX();
	}

	/**
	 * Computes the set of all (minimal) successors of a given concept.
	 * A successor is a concept name in the completion graph that is reachable from the given concept via a single role.
	 * 
	 * @param concept The concept for which the successors will be computed
	 * @return The set of all minimal successors
	 */
	protected List<Successor> getSuccessors(Integer concept) {
		// compute all successors of the given concept
		List<Successor> successors = new ArrayList<Successor>();
		for (Integer relation : relationGraph.keySet()) {
			// check that the relation is not auxiliary or the top or bottom relation
			if (entityManager.isAuxiliary(relation) || relation < IntegerEntityManager.firstUsableIdentifier) {
				continue;
			}
			// add all successors for the relation
			for (Integer successor : relationGraph.get(relation).getByFirst(concept)) {
				successors.add(new Successor(relation, successor));
			}
		}

		// find the minimal successors in this set
		List<Successor> minimalSuccessors = new ArrayList<Successor>(successors);
		for (Successor s1 : successors) {
			if (!minimalSuccessors.contains(s1)) continue;
			for (Successor s2 : successors) {
				if (s1 != s2 && minimalSuccessors.contains(s2)
						&& objectPropertyGraph.getSubsumers(s1.getRole()).contains(s2.getRole())
						&& classGraph.getSubsumers(s1.getConcept()).contains(s2.getConcept())) {
					minimalSuccessors.remove(s2);
				}
			}
		}
		return minimalSuccessors;
	}

	/**
	 * Class to store a successor of a concept, consisting a role and the concept reachable from the original concept via the role.
	 */
	protected class Successor {
		private Integer role;
		private Integer concept;

		public Successor(Integer role, Integer concept) {
			this.role = role;
			this.concept = concept;
		}

		public Integer getRole() {
			return role;
		}

		public Integer getConcept() {
			return concept;
		}
			
		public int hashCode() {
			return 31 * role.hashCode() + concept.hashCode();
		}

		public boolean equals(Object other) {
			if (other instanceof Successor) {
				Successor so = (Successor)other;
				return role == so.role && concept == so.concept;
			} else return false;
		}
	}

}

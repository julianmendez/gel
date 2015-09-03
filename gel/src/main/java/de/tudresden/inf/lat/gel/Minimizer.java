package de.tudresden.inf.lat.gel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.tudresden.inf.lat.jcel.core.algorithm.cel.CelExtendedOntology;
import de.tudresden.inf.lat.jcel.core.graph.IntegerBinaryRelation;
import de.tudresden.inf.lat.jcel.core.graph.IntegerSubsumerGraph;
import de.tudresden.inf.lat.jcel.ontology.axiom.normalized.RI3Axiom;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerClass;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerClassExpression;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerDataTypeFactory;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerObjectIntersectionOf;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerObjectProperty;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerObjectSomeValuesFrom;

/**
 * Class that can simplify concept descriptions.
 * 
 * @author Andreas Ecke
 */
public class Minimizer {
	private IntegerSubsumerGraph classGraph;
	private IntegerSubsumerGraph objectPropertyGraph;
	private IntegerDataTypeFactory dataTypeFactory;
	private Map<Integer, IntegerBinaryRelation> relationGraph;
	private CelExtendedOntology ontology;

	/**
	 * Loads the data structures from the GelProcessor. These data structures
	 * include: The class and object property graph, the set of relations, the
	 * entity manager and the data type factory. This method should only be
	 * called after the processing of the GelProcessor terminated, as otherwise
	 * the subsumer graphs are not complete.
	 * 
	 * @param processor The GelProcessor from which to load all the data
	 *        structures
	 */
	public void loadDataStructures(GelProcessor processor) {
		classGraph = processor.getClassGraphX();
		objectPropertyGraph = processor.getObjectPropertyGraphX();
		dataTypeFactory = processor.getOntologyObjectFactory().getDataTypeFactory();
		relationGraph = processor.getRelationGraphX();
		ontology = processor.getOntologyX();
	}

	/**
	 * Simplifies the given class expression
	 * 
	 * @param expression class expression to simplify
	 * @return equivalent simplified class expression
	 */
	public IntegerClassExpression minimize(IntegerClassExpression expression) {
		if (expression instanceof IntegerObjectIntersectionOf) {
			// class expression is a conjunction - get all conjuncts (that
			// should be either concept names or existential restrictions)
			Set<IntegerClassExpression> exprs = ((IntegerObjectIntersectionOf) expression).getOperands();
			Set<IntegerClassExpression> newExprs = new HashSet<IntegerClassExpression>(exprs);

			// remove all expressions that subsume other expressions
			for (IntegerClassExpression e1 : exprs) {
				if (!newExprs.contains(e1))
					continue;
				for (IntegerClassExpression e2 : exprs) {
					if (e1 != e2 && subConcept(e1, e2) && !(e1 instanceof IntegerObjectSomeValuesFrom && e2 instanceof IntegerClass)) {
						newExprs.remove(e2);
					}
				}
			}

			// minimize all remaining expressions individually
			Set<IntegerClassExpression> newExprs2 = new HashSet<IntegerClassExpression>();
			for (IntegerClassExpression e : newExprs) {
				newExprs2.add(minimize(e));
			}

			// return the conjunction of the minimized expressions
			return dataTypeFactory.createObjectIntersectionOf(newExprs2);
		} else if (expression instanceof IntegerObjectSomeValuesFrom) {
			// if the expression is an existential restriction, minimize the
			// inner class expression
			IntegerObjectSomeValuesFrom l = (IntegerObjectSomeValuesFrom) expression;
			return dataTypeFactory.createObjectSomeValuesFrom(l.getProperty(), minimize(l.getFiller()));
		} else {
			// if the expression is a concept name, there is nothing to minimize
			return expression;
		}
	}

	/**
	 * Tests, if a class expression is a subconcept of another class expression
	 * 
	 * @param e1 first class expression
	 * @param e2 second class expression
	 * @return true, if e1 is subsumed by e2; else false
	 */
	private boolean subConcept(IntegerClassExpression e1, IntegerClassExpression e2) {
		if (e2 instanceof IntegerClass) {
			IntegerClass ic2 = (IntegerClass) e2;
			if (ic2.getId() == 1)
				return true;
			if (e1 instanceof IntegerClass) {
				// if both concepts are classes, we can use the class graph
				return classGraph.getSubsumers(((IntegerClass) e1).getId()).contains(ic2.getId());
			} else if (e1 instanceof IntegerObjectIntersectionOf) {
				// if the e1 is a conjunction, at least one of its conjuncts
				// must be a subconcept of e2
				for (IntegerClassExpression e : ((IntegerObjectIntersectionOf) e1).getOperands()) {
					if (subConcept(e, e2))
						return true;
				}
			}
			return false;
		} else if (e2 instanceof IntegerObjectIntersectionOf) {
			// if e2 is a conjunction, each of its conjuncts must subsume e1
			for (IntegerClassExpression e : ((IntegerObjectIntersectionOf) e2).getOperands()) {
				if (!subConcept(e1, e))
					return false;
			}
			return true;
		} else {
			IntegerObjectSomeValuesFrom isvf2 = (IntegerObjectSomeValuesFrom) e2;
			if (e1 instanceof IntegerObjectSomeValuesFrom) {
				// if both concepts are existential restrictions, test the inner
				// class expressions for subsumption
				IntegerObjectSomeValuesFrom isvf1 = (IntegerObjectSomeValuesFrom) e1;
				// return
				// isvf1.getProperty().getId().equals(isvf2.getProperty().getId())
				// && subConcept(isvf1.getFiller(), isvf2.getFiller());
				if (objectPropertyGraph.getSubsumers(((IntegerObjectProperty) isvf1.getProperty()).getId()).contains(((IntegerObjectProperty) isvf2.getProperty()).getId()) && subConcept(isvf1.getFiller(), isvf2.getFiller())) {
					return true;
				}

				for (RI3Axiom ri3 : ontology.getSubPropertyAxiomSetByLeft(((IntegerObjectProperty) isvf1.getProperty()).getId())) {
					if (objectPropertyGraph.getSubsumers(ri3.getSuperProperty()).contains(((IntegerObjectProperty) isvf2.getProperty()).getId())) {
						for (IntegerObjectSomeValuesFrom eth : findExistentialRestrictions(ri3.getRightSubProperty(), isvf1.getFiller())) {
							if (subConcept(eth.getFiller(), isvf2.getFiller()))
								return true;
						}
					}
				}
				return false;
			} else if (e1 instanceof IntegerObjectIntersectionOf) {
				// if the e1 is a conjunction, at least one of its conjuncts
				// must be a subconcept of e2
				for (IntegerClassExpression e : ((IntegerObjectIntersectionOf) e1).getOperands()) {
					if (subConcept(e, e2))
						return true;
				}
			} else {
				// if e1 is a class and e2 an existential restriction for role
				// r, test if any of the
				// classes in the completion set S(e1, r) is a subconcept of the
				// inner class expression of e2
				for (Integer n1 : relationGraph.get(((IntegerObjectProperty) isvf2.getProperty()).getId()).getByFirst(((IntegerClass) e1).getId())) {
					if (subConcept(dataTypeFactory.createClass(n1), isvf2.getFiller()))
						return true;
				}
			}
			return false;
		}
	}

	private Collection<IntegerObjectSomeValuesFrom> findExistentialRestrictions(Integer property, IntegerClassExpression expression) {
		Collection<IntegerObjectSomeValuesFrom> set = new ArrayList<IntegerObjectSomeValuesFrom>();
		if (expression instanceof IntegerObjectIntersectionOf) {
			for (IntegerClassExpression sub : ((IntegerObjectIntersectionOf) expression).getOperands()) {
				set.addAll(findExistentialRestrictions(property, sub));
			}
		} else if (expression instanceof IntegerObjectSomeValuesFrom) {
			IntegerObjectSomeValuesFrom isvf = (IntegerObjectSomeValuesFrom) expression;
			if (((IntegerObjectProperty) isvf.getProperty()).getId().intValue() == property.intValue()) {
				set.add(isvf);
			}
		}
		return set;
	}
}

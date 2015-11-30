package de.tudresden.inf.lat.gel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.tudresden.inf.lat.jcel.core.algorithm.cel.CelExtendedOntology;
import de.tudresden.inf.lat.jcel.core.algorithm.cel.CelProcessor;
import de.tudresden.inf.lat.jcel.core.graph.IntegerBinaryRelation;
import de.tudresden.inf.lat.jcel.core.graph.IntegerSubsumerGraph;
import de.tudresden.inf.lat.jcel.core.graph.IntegerSubsumerGraphImpl;
import de.tudresden.inf.lat.jcel.coreontology.axiom.NormalizedIntegerAxiom;
import de.tudresden.inf.lat.jcel.coreontology.axiom.NormalizedIntegerAxiomFactory;
import de.tudresden.inf.lat.jcel.coreontology.datatype.IntegerEntityManager;
import de.tudresden.inf.lat.jcel.ontology.axiom.complex.ComplexIntegerAxiom;
import de.tudresden.inf.lat.jcel.ontology.axiom.extension.IntegerOntologyObjectFactory;
import de.tudresden.inf.lat.jcel.ontology.normalization.OntologyNormalizer;

public class GelProcessor extends CelProcessor {
	private IntegerSubsumerGraph classGraph;
	private IntegerSubsumerGraph objectPropertyGraph;
	private Map<Integer, IntegerBinaryRelation> relationGraph;
	private IntegerEntityManager entityManager;
	private CelExtendedOntology ontology;
	private IntegerOntologyObjectFactory integerOntologyObjectFactory;

	public GelProcessor(Set<Integer> originalObjectProperties,
			Set<Integer> originalClasses,
			Set<NormalizedIntegerAxiom> normalizedAxiomSet,
			NormalizedIntegerAxiomFactory factory,
			IntegerEntityManager entityManager) {
		super(originalObjectProperties, originalClasses, normalizedAxiomSet,
				factory, entityManager);
	}

	/**
	 * Copies all important data structures, then calls the post processing step
	 * for the underlying processor. This is important as some of the data
	 * structures are deleted or modified during the post processing, but we
	 * need access to the full data structures to compute generalizations.
	 */
	@Override
	protected void postProcess() {
		// copy needed data structures from the CelProcessor before they get
		// destroyed
		classGraph = copy(super.getClassGraph());
		objectPropertyGraph = copy(super.getObjectPropertyGraph());
		relationGraph = getRelationX();
		entityManager = super.getEntityManager();
		ontology = super.getExtendedOntology();

		// now run the CelProcessor postProcess, which will clean up
		super.postProcess();
	}

	public IntegerSubsumerGraph getClassGraphX() {
		return classGraph;
	}

	public IntegerSubsumerGraph getObjectPropertyGraphX() {
		return objectPropertyGraph;
	}

	public Map<Integer, IntegerBinaryRelation> getRelationGraphX() {
		return relationGraph;
	}

	public IntegerEntityManager getEntityManagerX() {
		return entityManager;
	}

	public CelExtendedOntology getOntologyX() {
		return ontology;
	}

	/**
	 * Creates a copy of the subsumer graph (i.e., class graph or object
	 * property graph).
	 * 
	 * @param graph Subsumer graph that should be copied
	 * @return a copy of the subsumer graph
	 */
	private IntegerSubsumerGraph copy(IntegerSubsumerGraph graph) {
		IntegerSubsumerGraphImpl newGraph = new IntegerSubsumerGraphImpl(graph.getBottomElement(), graph.getTopElement());
		for (Integer c : graph.getElements()) {
			newGraph.add(c);
			for (Integer subsumer : graph.getSubsumers(c)) {
				newGraph.addAncestor(c, subsumer);
			}
		}
		return newGraph;
	}

	/**
	 * Computes a map that assigned to all roles the concepts that they connect
	 * in the completion graph.
	 * 
	 * @return The edges in the completion graph as a map assigning binary
	 *         relations to each role
	 */
	private Map<Integer, IntegerBinaryRelation> getRelationX() {
		Map<Integer, IntegerBinaryRelation> m = new TreeMap<Integer, IntegerBinaryRelation>();
		for (Integer relation : super.getRelationIdSet()) {
			if (super.getEntityManager().isAuxiliary(relation) || relation < IntegerEntityManager.firstUsableIdentifier)
				continue;
			m.put(relation, super.getRelation(relation));
		}
		return m;
	}

	public IntegerOntologyObjectFactory getIntegerOntologyObjectFactory () {
		return this.integerOntologyObjectFactory;
	}
	
	void setIntegerOntologyObjectFactory(IntegerOntologyObjectFactory factory) {
		this.integerOntologyObjectFactory = factory;
	}

	public static GelProcessor newGelProcessor(
			Set<ComplexIntegerAxiom> ontology,
			IntegerOntologyObjectFactory factory) {
		
		Set<Integer> originalClassSet = new HashSet<Integer>();
		Set<Integer> originalObjectPropertySet = new HashSet<Integer>();

		for (ComplexIntegerAxiom axiom : ontology) {
			originalClassSet.addAll(axiom.getClassesInSignature());
			originalObjectPropertySet.addAll(axiom
					.getObjectPropertiesInSignature());
		}

		OntologyNormalizer axiomNormalizer = new OntologyNormalizer();
		Set<NormalizedIntegerAxiom> normalizedAxiomSet = axiomNormalizer
				.normalize(ontology, factory);

		GelProcessor ret= new GelProcessor(originalObjectPropertySet, originalClassSet,
				normalizedAxiomSet, factory.getNormalizedAxiomFactory(),
				factory.getEntityManager());
		ret.setIntegerOntologyObjectFactory(factory);
		return ret;
	}

}

package de.tudresden.inf.lat.gel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

import de.tudresden.inf.lat.jcel.ontology.axiom.complex.ComplexIntegerAxiom;
import de.tudresden.inf.lat.jcel.ontology.axiom.extension.IntegerOntologyObjectFactory;
import de.tudresden.inf.lat.jcel.ontology.axiom.extension.IntegerOntologyObjectFactoryImpl;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerClass;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerClassExpression;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerObjectIntersectionOf;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerObjectProperty;
import de.tudresden.inf.lat.jcel.ontology.datatype.IntegerObjectSomeValuesFrom;
import de.tudresden.inf.lat.jcel.owlapi.translator.TranslationRepository;
import de.tudresden.inf.lat.jcel.owlapi.translator.Translator;

/**
 * The gel reasoner is a wrapper for the processor, that provides a nice
 * interface.
 * 
 * @author Andreas Ecke
 */
public class GelReasoner {
	private OWLOntology ontology = null;

	/**
	 * Creates a new Reasoner object that can be used to compute least common
	 * subsumers and most specific concepts for the given ontology.
	 * 
	 * @param ontology ontology
	 */
	public GelReasoner(OWLOntology ontology) {
		this.ontology = ontology;
	}

	/**
	 * Computes the least common subsumer of the concepts iteratively from binary lcs, i.e. it first computes the lcs 
	 * of the first two concepts, then the lcs of the result with the third concept and so on. Might sometimes be faster 
	 * the computing the n-ary lcs directly.
	 * 
	 * @param concepts The concept descriptions to generalize
	 * @param k role-depth bound
	 * @param simplify whether to simplify the result (note: intermediate results are ALWAYS simplified)
	 * @return The least common subsumer of the concepts
	 */
	public OWLClassExpression binaryLeastCommonSubsumer(OWLClassExpression[] concepts, int k, boolean simplify) {
		OWLClassExpression lcs = concepts[0];
		for (int i = 1; i < concepts.length; i++) {
			lcs = leastCommonSubsumer(new OWLClassExpression[] {lcs, concepts[i]}, k, (i == concepts.length - 1) ? simplify : true);
		}
		return lcs;
	}

	/**
	 * Computes the least common subsumer of the given concept descriptions directly.
	 * 
	 * @param concepts The concept descriptions to generalize
	 * @param k role-depth bound
	 * @param simplify whether to simplify the result
	 * @return The least common subsumer of the concepts
	 */
	public OWLClassExpression leastCommonSubsumer(OWLClassExpression[] concepts, int k, boolean simplify) {
		OWLDataFactory owlFactory = ontology.getOWLOntologyManager().getOWLDataFactory();

		// create new axioms N ≡ C with a new concept name N for each concept
		// description
		OWLClass[] newNames = new OWLClass[concepts.length];
		OWLEquivalentClassesAxiom[] newAxioms = new OWLEquivalentClassesAxiom[concepts.length];
		for (int i = 0; i < concepts.length; i++) {
			if (concepts[i].isClassExpressionLiteral()) {
				if (concepts[i].isOWLThing()) {
					newNames[i] = owlFactory.getOWLThing();
				} else {
					newNames[i] = concepts[i].asOWLClass();
				}
			} else {
				newNames[i] = owlFactory.getOWLClass(IRI.create("http://de.tudresden.inf.lat.gel#Concept" + i));
				newAxioms[i] = owlFactory.getOWLEquivalentClassesAxiom(newNames[i], concepts[i]);
				ontology.getOWLOntologyManager().addAxiom(ontology, newAxioms[i]);
			}
		}

		// translate ontology
		IntegerOntologyObjectFactory jcelFactory = new IntegerOntologyObjectFactoryImpl();
		Translator translator = new Translator(ontology.getOWLOntologyManager().getOWLDataFactory(), jcelFactory);
		Set<ComplexIntegerAxiom> axiomSet = translator.translateSA(ontology.getAxioms());

		// get IDs of the newly introduced concept names
		List<Integer> ids = new ArrayList<>(concepts.length);
		for (OWLClass c : newNames) {
			ids.add(translator.getTranslationRepository().getId(c));
		}

		// classify the ontology
		GelProcessor processor = GelProcessor.newGelProcessor(axiomSet, jcelFactory);
		while (!processor.isReady())
			processor.process();

		// compute the lcs
		LcsAlgorithm lcsAlgorithm = new LcsAlgorithm();
		lcsAlgorithm.loadDataStructures(processor);
		IntegerClassExpression lcs = lcsAlgorithm.leastCommonSubsumer(ids, k);

		// remove axioms again
		for (int i = 0; i < concepts.length; i++) {
			if (newNames[i] != concepts[i])
				ontology.getOWLOntologyManager().removeAxiom(ontology, newAxioms[i]);
		}

		// check if the lcs is one of the newly introduced names
		if (lcs instanceof IntegerClass) {
			IntegerClass concept = (IntegerClass) lcs;
			for (int i = 0; i < concepts.length; i++) {
				if (concept.getId() == ids.get(i).intValue()) {
					return concepts[i];
				}
			}
		}

		// otherwise simplify the result
		if (simplify) {
			Minimizer m = new Minimizer();
			m.loadDataStructures(processor);
			lcs = m.minimize(lcs);
		}

		// translate the lcs back to OWLApi format and return it
		return translateBack(ontology.getOWLOntologyManager().getOWLDataFactory(), translator.getTranslationRepository(), lcs);
	}

	/**
	 * Computes the most specific concept for the given individual.
	 * 
	 * @param individual The individual that should be generalized
	 * @param k role-depth bound
	 * @param simplify whether to simplify the result
	 * @return The most specific concept
	 */
	public OWLClassExpression mostSpecificConcept(OWLNamedIndividual individual, int k, boolean simplify) {
		// translate the ontology to jCel format
		IntegerOntologyObjectFactory factory = new IntegerOntologyObjectFactoryImpl();
		Translator translator = new Translator(ontology.getOWLOntologyManager().getOWLDataFactory(), factory);
		Set<ComplexIntegerAxiom> axiomSet = translator.translateSA(ontology.getAxioms());

		// classify the ontology
		int individualId = translator.getTranslationRepository().getId(individual);
		GelProcessor processor = GelProcessor.newGelProcessor(axiomSet, factory);
		while (!processor.isReady())
			processor.process();

		//compute the msc
		MscAlgorithm mscAlgorithm = new MscAlgorithm();
		mscAlgorithm.loadDataStructures(processor);
		IntegerClassExpression msc = mscAlgorithm.mostSpecificConcept(individualId, k);

		// simplify the result
		if (simplify) {
			Minimizer m = new Minimizer();
			m.loadDataStructures(processor);
			msc = m.minimize(msc);
		}
		
		// translate back to OWLApi format and return it
		return translateBack(ontology.getOWLOntologyManager().getOWLDataFactory(), translator.getTranslationRepository(), msc);
	}

	/**
	 * Translates the given concept description from jCel format to OWLApi format.
	 * 
	 * @param owlFactory A factory to create the complex owl class expression
	 * @param rep The translation repository used for the translation of the ontology
	 * @param exp The concept description in jCel format to translate
	 * @return The concept descrtion in OWLApi format
	 */
	private OWLClassExpression translateBack(OWLDataFactory owlFactory, TranslationRepository rep, IntegerClassExpression exp) {
		if (exp instanceof IntegerClass) {
			return rep.getOWLClass(((IntegerClass) exp).getId());
		} else if (exp instanceof IntegerObjectIntersectionOf) {
			Set<OWLClassExpression> classExpressions = new HashSet<>();
			for (IntegerClassExpression e : ((IntegerObjectIntersectionOf) exp).getOperands()) {
				classExpressions.add(translateBack(owlFactory, rep, e));
			}
			return owlFactory.getOWLObjectIntersectionOf(classExpressions);
		} else if (exp instanceof IntegerObjectSomeValuesFrom) {
			OWLObjectProperty property = rep.getOWLObjectProperty(((IntegerObjectProperty) ((IntegerObjectSomeValuesFrom) exp).getProperty()).getId());
			OWLClassExpression e = translateBack(owlFactory, rep, ((IntegerObjectSomeValuesFrom) exp).getFiller());
			return owlFactory.getOWLObjectSomeValuesFrom(property, e);
		} else {
			return null;
		}
	}
}

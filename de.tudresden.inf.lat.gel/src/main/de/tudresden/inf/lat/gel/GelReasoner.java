package de.tudresden.inf.lat.gel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import de.tudresden.inf.lat.jcel.core.axiom.complex.ComplexIntegerAxiom;
import de.tudresden.inf.lat.jcel.core.axiom.complex.IntegerEquivalentClassesAxiom;
import de.tudresden.inf.lat.jcel.core.datatype.IntegerClass;
import de.tudresden.inf.lat.jcel.core.datatype.IntegerClassExpression;
import de.tudresden.inf.lat.jcel.core.datatype.IntegerObjectIntersectionOf;
import de.tudresden.inf.lat.jcel.core.datatype.IntegerObjectSomeValuesFrom;
import de.tudresden.inf.lat.jcel.owlapi.translator.AxiomSetTranslator;
import de.tudresden.inf.lat.jcel.owlapi.translator.TranslationRepository;

/**
 * The gel reasoner is a wrapper for the processor, that provides a nice interface.
 * 
 * @author Andreas Ecke
 */
public class GelReasoner {
	private OWLOntology ontology = null;
	private OWLClass bottomClass = null;
	private OWLClass topClass = null;
	private OWLObjectProperty bottomObjectProperty = null;
	private OWLObjectProperty topObjectProperty = null;
	private OWLDataProperty bottomDataProperty = null;
	private OWLDataProperty topDataProperty = null;
	//private int firstNewConceptId = 0;

	/**
	 * Creates a new Reasoner object that can be used to compute least common subsumers and most specific concepts for the given ontology.
	 * @param ontology 
	 */
	public GelReasoner(OWLOntology ontology) {
		this.ontology = ontology;
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		bottomClass = factory.getOWLNothing();
		topClass = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLThing();
		bottomObjectProperty = factory.getOWLBottomObjectProperty();
		topObjectProperty = factory.getOWLTopObjectProperty();
		bottomDataProperty = factory.getOWLBottomDataProperty();
		topDataProperty = factory.getOWLTopDataProperty();
	}
	

	public OWLClassExpression ComputeLcs(int k, OWLClassExpression[] concepts, boolean simplify) {
		Set<OWLAxiom> axioms = ontology.getAxioms();
		OWLClass[] newConcepts = new OWLClass[concepts.length];
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		for (int i=0; i<concepts.length; i++) {
			newConcepts[i] = factory.getOWLClass(IRI.create("http://de.tudresden.inf.lat.gel#Concept" + i));
			axioms.add(factory.getOWLEquivalentClassesAxiom(newConcepts[i], concepts[i]));
		}

		TranslationRepository translationRep = createTranslationRepository(axioms);
		AxiomSetTranslator translator = new AxiomSetTranslator(translationRep);
		
		Set<ComplexIntegerAxiom> axiomSet = translator.translate(axioms);
		int[] transConcepts = new int[newConcepts.length];
		for (int i=0; i<newConcepts.length; i++) {
			transConcepts[i] = translator.getClassMap().get(newConcepts[i]);
		}
		LcsProcessor lcsProcessor = new LcsProcessor(axiomSet, transConcepts, k, simplify);
		
		Date start = new Date();
		while (!lcsProcessor.isReady()) lcsProcessor.process();
		System.out.println("Total time: " + ((new Date()).getTime() - start.getTime()) + "ms");
		IntegerClassExpression lcs = lcsProcessor.getLcs();
		if (lcs instanceof IntegerClass) {
			IntegerClass lcsClass = (IntegerClass)lcs;
			for (int i=0; i<concepts.length; i++) {
				if (translationRep.getOWLClass(lcsClass.getId()).toStringID().equals(newConcepts[i].toStringID())) return concepts[i];
			}
		}
		return translateBack(translationRep, lcs);
	}
	
	public OWLClassExpression ComputeMsc(int k, OWLNamedIndividual individual, boolean simplify) {
		TranslationRepository translationRep = createTranslationRepository(ontology.getAxioms());
		AxiomSetTranslator translator = new AxiomSetTranslator(translationRep);
		Set<ComplexIntegerAxiom> axiomSet = translator.translate(ontology.getAxioms());
	
		int indiv = translator.getIndividualMap().get(individual);
		MscProcessor mscProcessor = new MscProcessor(axiomSet, indiv, k, simplify);
		while (!mscProcessor.isReady()) mscProcessor.process();
		
		return translateBack(translationRep, mscProcessor.getMsc());
	}
	
	private OWLClassExpression translateBack(TranslationRepository rep, IntegerClassExpression exp) {
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		if (exp instanceof IntegerClass) {
			return rep.getOWLClass(((IntegerClass) exp).getId());
		} else if (exp instanceof IntegerObjectIntersectionOf) {
			Set<OWLClassExpression> classExpressions = new HashSet<OWLClassExpression>();
			for (IntegerClassExpression e : ((IntegerObjectIntersectionOf) exp).getOperands()) {
				classExpressions.add(translateBack(rep, e));
			}
			return factory.getOWLObjectIntersectionOf(classExpressions);
		} else if (exp instanceof IntegerObjectSomeValuesFrom) {
			OWLObjectProperty property = rep.getOWLObjectProperty(((IntegerObjectSomeValuesFrom) exp).getProperty().getId());
			OWLClassExpression e = translateBack(rep, ((IntegerObjectSomeValuesFrom) exp).getFiller());
			return factory.getOWLObjectSomeValuesFrom(property, e);
		} else {
			return null;
		}
	}
	
	
	protected TranslationRepository createTranslationRepository(Set<OWLAxiom> axiomSet) {
		TranslationRepository ret = null;
		if (axiomSet != null) {
			Set<OWLClass> conceptNameSet = collectClasses(axiomSet);
			Set<OWLObjectProperty> propertySet = collectProperties(axiomSet);
			Set<OWLNamedIndividual> individualSet = collectIndividuals(axiomSet);
			Set<OWLDataProperty> dataPropertySet = collectDataProperties(axiomSet);
			Set<OWLLiteral> literalSet = collectLiterals(axiomSet);
			ret = new TranslationRepository();
			ret.load(bottomClass, topClass,	bottomObjectProperty, topObjectProperty, bottomDataProperty, topDataProperty,
					conceptNameSet, propertySet, individualSet, dataPropertySet, literalSet);
			//firstNewConceptId = conceptNameSet.size()+2;
		}
		return ret;
	}

	protected Set<OWLClass> collectClasses(Set<OWLAxiom> axiomSet) {
		Set<OWLClass> ret = new HashSet<OWLClass>();
		for (OWLAxiom axiom : axiomSet) {
			ret.addAll(axiom.getClassesInSignature());
		}
		ret.add(bottomClass);
		ret.add(topClass);
		return ret;
	}

	protected Set<OWLNamedIndividual> collectIndividuals(Set<OWLAxiom> axiomSet) {
		Set<OWLNamedIndividual> ret = new HashSet<OWLNamedIndividual>();
		for (OWLAxiom axiom : axiomSet) {
			Set<OWLNamedIndividual> entities = axiom
					.getIndividualsInSignature();
			ret.addAll(entities);
		}
		return ret;
	}

	protected Set<OWLObjectProperty> collectProperties(Set<OWLAxiom> axiomSet) {
		Set<OWLObjectProperty> ret = new HashSet<OWLObjectProperty>();
		for (OWLAxiom axiom : axiomSet) {
			Set<OWLObjectProperty> entities = axiom
					.getObjectPropertiesInSignature();
			ret.addAll(entities);
		}
		return ret;
	}

	private Set<OWLDataProperty> collectDataProperties(Set<OWLAxiom> axiomSet) {
		Set<OWLDataProperty> ret = new HashSet<OWLDataProperty>();
		for (OWLAxiom axiom : axiomSet) {
			Set<OWLDataProperty> entities = axiom
					.getDataPropertiesInSignature();
			ret.addAll(entities);
		}
		return ret;
	}

	private Set<OWLLiteral> collectLiterals(Set<OWLAxiom> axiomSet) {
		Set<OWLLiteral> ret = new HashSet<OWLLiteral>();
		for (OWLAxiom axiom : axiomSet) {
			if ((axiom instanceof OWLDataPropertyAssertionAxiom)) {
				ret.add(((OWLDataPropertyAssertionAxiom) axiom).getObject());
			}
			if (axiom instanceof OWLNegativeDataPropertyAssertionAxiom) {
				ret.add(((OWLNegativeDataPropertyAssertionAxiom) axiom)
						.getObject());
			}
			Set<OWLClassExpression> classExpressions = axiom
					.getNestedClassExpressions();
			for (OWLClassExpression classExpr : classExpressions) {
				if (classExpr instanceof OWLDataHasValue) {
					ret.add(((OWLDataHasValue) classExpr).getValue());
				}
				if (classExpr instanceof OWLDataOneOf) {
					ret.addAll(((OWLDataOneOf) classExpr).getValues());
				}
			}

		}
		return ret;
	}
}

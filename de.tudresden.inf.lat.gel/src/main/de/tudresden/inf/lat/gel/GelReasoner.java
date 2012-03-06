package de.tudresden.inf.lat.gel;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
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
 * The gel reasoner is a wrapper for the processor, that provides a nice interface.
 * 
 * @author Andreas Ecke
 */
public class GelReasoner {
	private OWLOntology ontology = null;

	/**
	 * Creates a new Reasoner object that can be used to compute least common subsumers and most specific concepts for the given ontology.
	 * @param ontology 
	 */
	public GelReasoner(OWLOntology ontology) {
		this.ontology = ontology;
	}
	

	public OWLClassExpression ComputeLcs(int k, OWLClassExpression a, OWLClassExpression b, boolean simplify, boolean opti1, boolean opti2) {
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		//create new axioms A\equiv C, B\equiv D and add to ontology
		OWLClass newA = factory.getOWLClass(IRI.create("http://de.tudresden.inf.lat.gel#ConceptA"));
		OWLClass newB = factory.getOWLClass(IRI.create("http://de.tudresden.inf.lat.gel#ConceptB"));
		OWLEquivalentClassesAxiom newAxiomA = factory.getOWLEquivalentClassesAxiom(newA, a);
		OWLEquivalentClassesAxiom newAxiomB = factory.getOWLEquivalentClassesAxiom(newB, b);
		ontology.getOWLOntologyManager().addAxiom(ontology, newAxiomA);
		ontology.getOWLOntologyManager().addAxiom(ontology, newAxiomB);

		//translate ontology
		IntegerOntologyObjectFactory intfac = new IntegerOntologyObjectFactoryImpl();
		Translator translator = new Translator(ontology, intfac);
		Set<ComplexIntegerAxiom> axiomSet = translator.getOntology();
		
		//find ids of concepts A, B, ...
		int transA =  translator.getTranslationRepository().getId(newA);
		int transB =  translator.getTranslationRepository().getId(newB);
		
		//compute lcs of these ids
		final DecimalFormat df = new DecimalFormat( "0.0" );
		LcsProcessor lcsProcessor = new LcsProcessor(axiomSet, transA, transB, k, simplify, opti1, opti2, intfac);
		long start = System.nanoTime();
		while (!lcsProcessor.isReady()) lcsProcessor.process();
		System.out.println("Total time: " + df.format((System.nanoTime() - start) / 1000000.0) + "ms");
		IntegerClassExpression lcs = lcsProcessor.getLcs();
		
		//remove axioms again
		ontology.getOWLOntologyManager().removeAxiom(ontology, newAxiomA);
		ontology.getOWLOntologyManager().removeAxiom(ontology, newAxiomB);
		
		//translate back to OWL expressions
		if (lcs instanceof IntegerClass) {
			IntegerClass lcsClass = (IntegerClass)lcs;
			if (translator.translateC(lcsClass).toStringID().equals(newA.toStringID())) return a;
			if (translator.translateC(lcsClass).toStringID().equals(newB.toStringID())) return b;
		}
		return translateBack(translator.getTranslationRepository(), lcs);
	}
	
	public OWLClassExpression ComputeLcs(int k, OWLClassExpression[] concepts, boolean simplify, boolean opti1, boolean opti2) {
		OWLClassExpression lcs = concepts[0];
		for (int i=1; i<concepts.length; i++) {
			lcs = ComputeLcs(k, lcs, concepts[i], simplify, opti1, opti2);
		}
		return lcs;
	}
	
	public OWLClassExpression ComputeMsc(int k, OWLNamedIndividual individual, boolean simplify) {
		IntegerOntologyObjectFactory intfac = new IntegerOntologyObjectFactoryImpl();
		Translator translator = new Translator(ontology, intfac);
		Set<ComplexIntegerAxiom> axiomSet = translator.getOntology();
	
		int indiv = translator.getTranslationRepository().getId(individual);
		MscProcessor mscProcessor = new MscProcessor(axiomSet, indiv, k, simplify, intfac);
		while (!mscProcessor.isReady()) mscProcessor.process();
		
		return translateBack(translator.getTranslationRepository(), mscProcessor.getMsc());
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
			OWLObjectProperty property = rep.getOWLObjectProperty(((IntegerObjectProperty)((IntegerObjectSomeValuesFrom) exp).getProperty()).getId());
			OWLClassExpression e = translateBack(rep, ((IntegerObjectSomeValuesFrom) exp).getFiller());
			return factory.getOWLObjectSomeValuesFrom(property, e);
		} else {
			return null;
		}
	}
}

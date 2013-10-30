package de.tudresden.inf.lat.gel;

import java.awt.Dimension;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.ui.clsdescriptioneditor.ExpressionEditor;
import org.protege.editor.owl.ui.clsdescriptioneditor.OWLExpressionChecker;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;

/**
 * Class for input or output of complex concept descriptions. It is a dialog consisting of an ExpressionEditor (provided by Protege)
 * and OK and Cancel buttons. If OK is clicked, than the new concept is returned, otherwise the initially provided concept is returned.
 * 
 * @author Andreas Ecke
 */
public class ClassExpressionEditor extends JPanel {
	private static final long serialVersionUID = 580293901264973203L;
	
	private OWLWorkspace owlWorkspace;
	private ExpressionEditor<OWLClassExpression> expressionEditor;
	
	/**
	 * Creates a new class expression editor.
	 * 
	 * @param owlWorkspace The owl workspace needed for completion and translation into OWLApi format
	 */
	public ClassExpressionEditor(OWLWorkspace owlWorkspace) {
		this.owlWorkspace = owlWorkspace;
		
		// create an expression checker and the expression editor itself
		OWLExpressionChecker<OWLClassExpression> expressionChecker = owlWorkspace.getOWLModelManager().getOWLExpressionCheckerFactory().getOWLClassExpressionChecker();
		expressionEditor = new ExpressionEditor<OWLClassExpression>(owlWorkspace.getOWLEditorKit(), expressionChecker);

		// wrap the expression editor in a scroll pane with fixed size and a vertical scrollbar
		JScrollPane expressionEditorScrollPane = new JScrollPane(expressionEditor);
		expressionEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		expressionEditorScrollPane.setPreferredSize(new Dimension(400, 300));

		// add the expression editor to this panel
		add(expressionEditorScrollPane);
	}
	
	/**
	 * Show the class expression editor as a dialog with the given concept. The user can change the concept and either accept
	 * the changes or revert them.
	 * 
	 * @param exp The concept to show initially
	 * @return the new concept if OK was clicked; the initial concept otherwise
	 */
	public OWLClassExpression showDialog(OWLClassExpression exp) {
		// if no concept is given, use top
		if (exp == null) {
			exp = owlWorkspace.getOWLModelManager().getOWLDataFactory().getOWLThing();
		}
		
		// show the concept as a modal dialog (with ok and cancel buttons)
		expressionEditor.setExpressionObject(exp);
		int response = JOptionPane.showConfirmDialog(owlWorkspace, this, "Class expression editor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (response == JOptionPane.OK_OPTION) {
			// if ok is clicked and the class expression in in the expression editor is valid, return it
			try {
				return expressionEditor.createObject();
			} catch (OWLException e) { }
		}
		// else return the old class expression (or top if non was given)
		return exp;
	}
}

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

public class ClassExpressionEditor extends JPanel {
	private static final long serialVersionUID = 580293901264973203L;
	
	OWLWorkspace owlWorkspace;
	ExpressionEditor<OWLClassExpression> expressionEditor;
	
	public ClassExpressionEditor(OWLWorkspace owlWorkspace) {
		this.owlWorkspace = owlWorkspace;
		OWLExpressionChecker<OWLClassExpression> expressionChecker = owlWorkspace.getOWLModelManager().getOWLExpressionCheckerFactory().getOWLClassExpressionChecker();

		expressionEditor = new ExpressionEditor<OWLClassExpression>(owlWorkspace.getOWLEditorKit(), expressionChecker);

		JScrollPane expressionEditorScrollPane = new JScrollPane(expressionEditor);
		expressionEditor.setPreferredSize(new Dimension(400, 300));

		//setLayout(new BorderLayout());
		setSize(new Dimension(200, 200));
		add(expressionEditorScrollPane); //, BorderLayout.NORTH);
	}
	
	public OWLClassExpression showDialog(OWLClassExpression exp) {
		if (exp == null) {
			exp = owlWorkspace.getOWLModelManager().getOWLDataFactory().getOWLThing();
		}
		expressionEditor.setExpressionObject(exp);
		int response = JOptionPane.showConfirmDialog(owlWorkspace, this, "Class expression editor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (response == JOptionPane.OK_OPTION) {
			try {
				return expressionEditor.createObject();
			} catch (OWLException e) { }
		}
		return exp;//owlWorkspace.getOWLModelManager().getOWLDataFactory().getOWLThing();
	}
}

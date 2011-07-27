package de.tudresden.inf.lat.gel;


import org.protege.editor.owl.ui.view.AbstractActiveOntologyViewComponent;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.SimpleRenderer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.*;

public class LcsView extends AbstractActiveOntologyViewComponent implements ActionListener {
	private static final long serialVersionUID = 4052074502148410361L;
	
	private JCheckBox[] lcsInputCheckBoxes;
	private JTextField[] lcsInputTextFields;
	private OWLClassExpression[] expressions;
	private JSpinner lcsDepth;
	//private JTextArea lcsResult;
	private JCheckBox lcsSimplifyCheckBox;

	// create the window layout
	@Override
	protected void initialiseOntologyView() throws Exception {
		setLayout(new BorderLayout());
		
		//JPanel lcs = new JPanel(new BorderLayout());
		//lcs.setBorder(BorderFactory.createTitledBorder("Input concept descriptions"));
		
		Box lcsBox = Box.createVerticalBox();
		lcsBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		lcsBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		//lcsBox.setAlignmentY(0.0f);
		
		Box label = Box.createHorizontalBox();
		label.add(new JLabel("Input concept descriptions"));
		label.add(Box.createHorizontalGlue());
		
		lcsBox.add(label);
		lcsBox.add(Box.createVerticalStrut(2));
		
		//lcsBox.add(new JLabel("Input concept descriptions"));
		int num = 8;
		lcsInputCheckBoxes = new JCheckBox[num];
		lcsInputTextFields = new JTextField[num];
		expressions = new OWLClassExpression[num];
		for (int i=0; i<num; i++) {
			expressions[i] = getOWLModelManager().getOWLDataFactory().getOWLThing();
			Box field = Box.createHorizontalBox();
			field.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
			lcsInputCheckBoxes[i] = new JCheckBox("", i<2);
			field.add(lcsInputCheckBoxes[i]);
			field.add(Box.createHorizontalStrut(5));
			lcsInputTextFields[i] = new JTextField(render(expressions[i]));//(i<2) ? "Thing" : "");
			lcsInputTextFields[i].setEditable(false);
			field.add(lcsInputTextFields[i]);
			JButton edit = new JButton("edit");
			edit.setActionCommand("edit" + i);
			edit.addActionListener(this);
			field.add(Box.createHorizontalStrut(3));
			field.add(edit);
			lcsBox.add(field);
			lcsBox.add(Box.createVerticalStrut(2));
		}

		lcsBox.add(Box.createVerticalStrut(3));
		Box lcsDepthBox = Box.createHorizontalBox();
		lcsDepthBox.add(new JLabel("Maximum Role-Depth"));
		lcsDepthBox.add(Box.createHorizontalStrut(10));
		lcsDepth = new JSpinner(new SpinnerNumberModel(5,0,100,1));
		lcsDepthBox.add(lcsDepth);
		lcsBox.add(lcsDepthBox);
		lcsBox.add(Box.createVerticalStrut(3));
		
		lcsSimplifyCheckBox = new JCheckBox("Simplify result", true);
		
		JButton lcsButton = new JButton("Compute Lcs");
		lcsButton.setActionCommand("lcs");
		lcsButton.addActionListener(this);
		
		Box bottom = Box.createHorizontalBox();
		bottom.add(lcsSimplifyCheckBox);
		bottom.add(Box.createHorizontalGlue());
		bottom.add(lcsButton);
		
		lcsBox.add(bottom);

		
		JScrollPane scrollPane = new JScrollPane(lcsBox);
	    //scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    //scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		//lcs.add(scrollPane, BorderLayout.CENTER);

		/*
		JPanel lcs1 = new JPanel(new BorderLayout());
		lcs1.setBorder(BorderFactory.createTitledBorder("Least Common Subsumer Result"));

		lcsResult = new JTextArea(100, 10);
		//lcsResult.setEditable(false);
		lcsResult.setLineWrap(true);
		lcsResult.setFont(new Font("Default", Font.PLAIN, 12));
	    JScrollPane scrollPane = new JScrollPane(lcsResult);
	    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		lcs1.add(scrollPane, BorderLayout.CENTER);
		*/
		add(scrollPane, BorderLayout.CENTER);
		//add(lcs1, BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// button click - compute lsc
		if ("lcs".equals(e.getActionCommand())) {
			GelReasoner r = new GelReasoner(super.getOWLModelManager().getActiveOntology());
			/*ArrayList<String> s = new ArrayList<String>();
			// select all concepts where the checkbox is checked
			for (int i=0; i<8; i++) {
				if (lcsInputCheckBoxes[i].isSelected())
					s.add(lcsInputTextFields[i].getText());
			}*/
			ArrayList<OWLClassExpression> concepts = new ArrayList<OWLClassExpression>();
			for (int i=0; i<8; i++) {
				if (lcsInputCheckBoxes[i].isSelected()) {
					concepts.add(expressions[i]);
				}
			}
			if (concepts.size()>=2) {
				// if there are at least two, compute the lcs
				OWLClassExpression[] n = new OWLClassExpression[concepts.size()];
				n = concepts.toArray(n);
				OWLClassExpression result = r.ComputeLcs((Integer)lcsDepth.getValue(), n, lcsSimplifyCheckBox.isSelected());
				ClassExpressionEditor cee = new ClassExpressionEditor(getOWLWorkspace());
				cee.showDialog(result);
			} else {
				//lcsResult.setText("Can't compute the lcs for less than 2 concepts");
			}
		} else if (e.getActionCommand().startsWith("edit")) {
			int num = Integer.parseInt(e.getActionCommand().substring(4));
			ClassExpressionEditor cee = new ClassExpressionEditor(getOWLWorkspace());
			expressions[num] = cee.showDialog(expressions[num]);
			lcsInputTextFields[num].setText(render(expressions[num])); //expressions[num].toString());
		}
	}
	
	private String render(OWLClassExpression e) {
		if (e instanceof OWLClass) {
			return ((OWLClass)e).getIRI().getFragment();
		} else if (e instanceof OWLObjectIntersectionOf) {
			String r = "(";
			int i=0;
			for (OWLClassExpression se : ((OWLObjectIntersectionOf)e).getOperands()) {
				r += (i>0) ? " and " + render(se) : render(se);
				i++;
			}
			return r + ")";
		} else if (e instanceof OWLObjectSomeValuesFrom) {
			OWLObjectPropertyExpression p = ((OWLObjectSomeValuesFrom) e).getProperty();
			return p.getNamedProperty().getIRI().getFragment() + " some " + render(((OWLObjectSomeValuesFrom) e).getFiller());
		}
		return null;
	}
	
	@Override
	protected void disposeOntologyView() {}


	@Override
	protected void updateView(OWLOntology activeOntology) {}
}

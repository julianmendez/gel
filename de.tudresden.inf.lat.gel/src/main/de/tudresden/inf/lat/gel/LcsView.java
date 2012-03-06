package de.tudresden.inf.lat.gel;


import org.protege.editor.owl.ui.view.AbstractActiveOntologyViewComponent;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;

public class LcsView extends AbstractActiveOntologyViewComponent implements ActionListener {
	private static final long serialVersionUID = 4052074502148410361L;
	
	private JSpinner lcsDepth;
	private JCheckBox lcsSimplifyCheckBox;
	private JCheckBox lcsOpti1CheckBox;
	private JCheckBox lcsOpti2CheckBox;
	private JButton addField;
	private Box inputBox;
	
	List<JCheckBox> inputCheckBoxes = new ArrayList<JCheckBox>();
	List<JTextField> inputTextFields = new ArrayList<JTextField>();
	List<OWLClassExpression> inputConcepts = new ArrayList<OWLClassExpression>();
	
	private Box createField() {
		int i = inputConcepts.size();
		inputConcepts.add(getOWLModelManager().getOWLDataFactory().getOWLThing());
		Box field = Box.createHorizontalBox();
		field.setBorder(BorderFactory.createEmptyBorder(0, 10, 3, 10));
		inputCheckBoxes.add(new JCheckBox("", true));
		field.add(inputCheckBoxes.get(i));
		field.add(Box.createRigidArea(new Dimension(5, 0)));
		inputTextFields.add(new JTextField(render(inputConcepts.get(i)), 10));
		inputTextFields.get(i).setEditable(false);
		field.add(inputTextFields.get(i));
		field.add(Box.createRigidArea(new Dimension(5, 0)));
		JButton edit = new JButton("edit");
		edit.setActionCommand("edit" + i);
		edit.addActionListener(this);
		field.add(edit);
		return field;
	}
	
	// create the window layout
	@Override
	protected void initialiseOntologyView() throws Exception {
		setLayout(new BorderLayout());
		
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputBox = Box.createVerticalBox();
		//create input fields
		inputBox.add(createField());
		inputBox.add(createField());
		addField = new JButton("more");
		addField.setActionCommand("add");
		addField.addActionListener(this);
		inputBox.add(addField);
		inputPanel.add(inputBox, BorderLayout.NORTH);
		JScrollPane inputList = new JScrollPane(inputPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		inputList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Input concept descriptions"));

		Box optionsBox = Box.createVerticalBox();
		optionsBox.setAlignmentX(0.0f);
		optionsBox.setAlignmentY(0.0f);
		Box depthBox = Box.createHorizontalBox();
		depthBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		depthBox.add(new JLabel("Depth"));
		depthBox.add(Box.createHorizontalStrut(5));
		lcsDepth = new JSpinner(new SpinnerNumberModel(5,0,100,1));
		((JSpinner.DefaultEditor)lcsDepth.getEditor()).getTextField().setColumns(3);
		depthBox.add(lcsDepth);
		optionsBox.add(depthBox);
		lcsSimplifyCheckBox = new JCheckBox("Simplify result", true);
		lcsOpti1CheckBox = new JCheckBox("Optimization 1", true);
		lcsOpti2CheckBox = new JCheckBox("Optimization 2", false);
		optionsBox.add(lcsSimplifyCheckBox);
		optionsBox.add(lcsOpti1CheckBox);
		optionsBox.add(lcsOpti2CheckBox);
		JButton lcsButton = new JButton("Compute Lcs");
		lcsButton.setActionCommand("lcs");
		lcsButton.addActionListener(this);
		optionsBox.add(lcsButton);

		add(inputList, BorderLayout.CENTER);
		add(optionsBox, BorderLayout.SOUTH);
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
			for (int i=0; i<inputConcepts.size(); i++) {
				if (inputCheckBoxes.get(i).isSelected()) {
					concepts.add(inputConcepts.get(i));
				}
			}
			if (concepts.size()>=2) {
				// if there are at least two, compute the lcs
				OWLClassExpression[] n = new OWLClassExpression[concepts.size()];
				n = concepts.toArray(n);
				OWLClassExpression result = r.ComputeLcs((Integer)lcsDepth.getValue(), n, lcsSimplifyCheckBox.isSelected(), lcsOpti1CheckBox.isSelected(), lcsOpti2CheckBox.isSelected());
				ClassExpressionEditor cee = new ClassExpressionEditor(getOWLWorkspace());
				cee.showDialog(result);
			} else {
				System.out.println("Nope");
				//lcsResult.setText("Can't compute the lcs for less than 2 concepts");
			}
		} else if (e.getActionCommand().startsWith("edit")) {
			int num = Integer.parseInt(e.getActionCommand().substring(4));
			ClassExpressionEditor cee = new ClassExpressionEditor(getOWLWorkspace());
			inputConcepts.set(num, cee.showDialog(inputConcepts.get(num)));
			inputTextFields.get(num).setText(render(inputConcepts.get(num)));
		} else if ("add".equals(e.getActionCommand())) {
			inputBox.remove(addField);
			inputBox.add(createField());
			inputBox.add(addField);
			this.validate();
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

package de.tudresden.inf.lat.gel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.protege.editor.owl.ui.view.AbstractActiveOntologyViewComponent;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

public class MscView extends AbstractActiveOntologyViewComponent implements ActionListener {
	private static final long serialVersionUID = 4809453567446243314L;

	private JComboBox mscIndividualComboBox;
	private JSpinner mscDepth;
	//private JTextArea mscResult;
	private JCheckBox mscSimplifyCheckBox;
	private OWLNamedIndividual[] individuals;
	
	// create the window layout
	@Override
	protected void initialiseOntologyView() throws Exception {
		setLayout(new BorderLayout());
		
		JPanel msc = new JPanel(new BorderLayout());
		msc.setBorder(BorderFactory.createTitledBorder("Most Specific Concept"));
		
		Box mscBox = Box.createVerticalBox();
		mscBox.setAlignmentX(0);
		
		Box indivBox = Box.createHorizontalBox();
		indivBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		indivBox.add(new JLabel("Individual"));
		indivBox.add(Box.createHorizontalStrut(10));
		mscIndividualComboBox = new JComboBox();
		initIndividuals();
		indivBox.add(mscIndividualComboBox);
		mscBox.add(indivBox);

		Box mscDepthBox = Box.createHorizontalBox();
		mscDepthBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		mscDepthBox.add(new JLabel("Depth"));
		mscDepthBox.add(Box.createHorizontalStrut(10));
		mscDepth = new JSpinner(new SpinnerNumberModel(5,0,100,1));
		mscDepthBox.add(mscDepth);
		mscBox.add(mscDepthBox);

		mscSimplifyCheckBox = new JCheckBox("Simplify result", true);
		mscBox.add(mscSimplifyCheckBox);
		
		// register button click
		JButton mscButton = new JButton("Compute Msc");
		mscButton.setActionCommand("msc");
		mscButton.addActionListener(this);
		mscBox.add(mscButton);
		
		/*
		mscResult = new JTextArea(100, 10);
		mscResult.setLineWrap(true);
		mscResult.setFont(new Font("Default", Font.PLAIN, 12));
		mscResult.setEditable(false);
	    JScrollPane scrollPane = new JScrollPane(mscResult);
	    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    
		mscBox.add(scrollPane);
		*/
		
		msc.add(mscBox, BorderLayout.CENTER);
		add(msc, BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// button click - compute msc
		if ("msc".equals(e.getActionCommand())) {
			GelReasoner r = new GelReasoner(super.getOWLModelManager().getActiveOntology());
			OWLClassExpression msc = r.ComputeMsc((Integer)mscDepth.getValue(), individuals[mscIndividualComboBox.getSelectedIndex()], mscSimplifyCheckBox.isSelected());
			ClassExpressionEditor cee = new ClassExpressionEditor(getOWLWorkspace());
			cee.showDialog(msc);
		}
	}
	
	
	private void initIndividuals() {
		mscIndividualComboBox.removeAllItems();
		Set<OWLNamedIndividual> indiSet = super.getOWLModelManager().getActiveOntology().getIndividualsInSignature();
		individuals = new OWLNamedIndividual[indiSet.size()];
		int i = 0;
		for (OWLNamedIndividual o : indiSet) {
			individuals[i] = o;
			mscIndividualComboBox.addItem(o.getIRI().getFragment());
			i++;
		}
	}
	
	@Override
	protected void disposeOntologyView() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateView(OWLOntology arg0) throws Exception {
		// TODO Auto-generated method stub
		initIndividuals();
		mscIndividualComboBox.invalidate();
	}
}

package de.tudresden.inf.lat.gel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
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
		
		//JPanel msc = new JPanel(new BorderLayout());
		//msc.setBorder(BorderFactory.createTitledBorder("Most Specific Concept"));
		
		Box mscBox = Box.createVerticalBox();
		mscBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		mscBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		
		Box indivBox = Box.createHorizontalBox();
		indivBox.add(new JLabel("Individual"));
		indivBox.add(Box.createHorizontalStrut(10));
		mscIndividualComboBox = new JComboBox();
		initIndividuals();
		indivBox.add(mscIndividualComboBox);
		indivBox.setMaximumSize(new Dimension((int)indivBox.getMaximumSize().getWidth(), (int)indivBox.getPreferredSize().getHeight()));
		mscBox.add(indivBox);
		mscBox.add(Box.createVerticalStrut(5));

		Box mscDepthBox = Box.createHorizontalBox();
		mscDepthBox.add(new JLabel("Maximum Role-Depth"));
		mscDepthBox.add(Box.createHorizontalStrut(10));
		mscDepth = new JSpinner(new SpinnerNumberModel(5,0,100,1));
		mscDepthBox.add(mscDepth);
		mscDepthBox.setMaximumSize(new Dimension((int)mscDepthBox.getMaximumSize().getWidth(), (int)mscDepthBox.getPreferredSize().getHeight()));
		mscBox.add(mscDepthBox);
		mscBox.add(Box.createVerticalStrut(2));
		
		mscSimplifyCheckBox = new JCheckBox("Simplify result", true);
		
		JButton mscButton = new JButton("Compute Msc");
		mscButton.setActionCommand("msc");
		mscButton.addActionListener(this);
		
		Box bottom = Box.createHorizontalBox();
		bottom.add(mscSimplifyCheckBox);
		bottom.add(Box.createHorizontalGlue());
		bottom.add(mscButton);
		// register button click
		mscBox.add(bottom);
		
		JScrollPane scrollPane = new JScrollPane(mscBox);
	    //scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    //scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		
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
		
		//msc.add(scrollPane, BorderLayout.CENTER);
		add(scrollPane, BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// button click - compute msc
		if ("msc".equals(e.getActionCommand()) && mscIndividualComboBox.getSelectedIndex() >= 0) {
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

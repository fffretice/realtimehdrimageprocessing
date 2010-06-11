/*
	Drago toolbar widget
    Copyright (C) 2009 Edward Duong

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Email: ed.duong@gmail.com
*/

package fasthdr.view;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fasthdr.controller.FastHDR;
import fasthdr.tmo.Drago;

public class ToolBarDrago implements ToolBarInterface {
	
	private static final double BIAS_DEFAULT = 0.85;
	private static final double BIAS_MIN = 0;
	private static final double BIAS_MAX = 1;
	
	private double bias = BIAS_DEFAULT;
	private JLabel biasLabel;
	private JFormattedTextField biasField;
	private JSlider biasSlider;
	
	private Drago tmo = null;
	
	public ToolBarDrago(){
		if(FastHDR.getFrame() != null){
			tmo = new Drago(FastHDR.getFrame(), FastHDR.getImagePanel());
		}
		performTMO();
	}
	
	@Override
	public void addWidgets(JToolBar toolBar) {
		addBiasWidget(toolBar);
	}

	@Override
	public void removeWidgets(JToolBar toolBar) {
		toolBar.remove(biasLabel);
		toolBar.remove(biasField);
		toolBar.remove(biasSlider);
	}
	
	@Override
	public void performTMO() {
		if(tmo != null){ tmo.performTMOwithPreview(bias); }
	}

	@Override
	public void cancelTMO() {
		if(tmo != null){ tmo.cancel(); }
	}
	
	public void addBiasWidget(JToolBar toolBar){
		// Create a label, slider and textField
		// Each slider notch is 0.01. BIAS_MAX / notches = 1 / 100 = 0.01 
		final int notches = 100;
		biasSlider = new JSlider(JSlider.HORIZONTAL, 0, notches, (int)(BIAS_DEFAULT * notches));
		NumberFormat format = NumberFormat.getNumberInstance();
		biasField = new JFormattedTextField(format);
		
		biasSlider.setPreferredSize(new Dimension(175, 20));
		biasSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				// Update bias model
				bias = biasSlider.getValue() / (double)notches;
				
				// Update bias text field
				biasField.setValue(bias);
				
				// Run tmo!
				performTMO();
			}
		});

		// Create label
		biasLabel = new JLabel("Bias");
		biasLabel.setLabelFor(biasSlider);
		
		biasField.setValue(BIAS_DEFAULT);
		biasField.setColumns(3);
		biasField.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				// Update bias model
				bias = ((Number)biasField.getValue()).doubleValue();
		        
				// Clamp range
				if(bias < BIAS_MIN){ bias = BIAS_MIN; }
		        if(bias > BIAS_MAX){ bias = BIAS_MAX; }
		        
		        // Update bias text field after clamp
		        biasField.setValue(bias);
		        
		        // Update slider (this will invoke the ChangeListener
		        // causing the tmo to run)
		        biasSlider.setValue((int)(bias * notches));
			}
		});
		
		// Add components to toolBar
		toolBar.add(biasLabel);
		toolBar.add(biasSlider);
		toolBar.add(biasField);
	}
}

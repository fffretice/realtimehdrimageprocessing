/*
	Fattal toolbar widget
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
import fasthdr.tmo.Fattal;

public class ToolBarFattal implements ToolBarInterface {
	
	public static final double ALPHA_DEFAULT = 0.1;
	public static final double ALPHA_MIN = 0;
	public static final double ALPHA_MAX = 2;
	public static final double ALPHA_INCREMENT_SIZE = 0.1;
	
	public static final double BETA_DEFAULT = 0.8;
	public static final double BETA_MIN = 0;
	public static final double BETA_MAX = 1;
	public static final double BETA_INCREMENT_SIZE = 0.1;
	
	public static final double COLOR_SAT_DEFAULT = 1;
	public static final double COLOR_SAT_MIN = 0;
	public static final double COLOR_SAT_MAX = 4;
	public static final double COLOR_SAT_INCREMENT_SIZE = 0.1;
	
	public static final double NOISE_REDUC_DEFAULT = 0.001;
	public static final double NOISE_REDUC_MIN = 0;
	public static final double NOISE_REDUC_MAX = 1;
	public static final double NOISE_REDUC_INCREMENT_SIZE = 0.1;
	
	private double alpha = ALPHA_DEFAULT;
	private double beta = BETA_DEFAULT;
	private double colorSat = COLOR_SAT_DEFAULT;
	private double noiseReduc = NOISE_REDUC_DEFAULT;
	
	private JFormattedTextField alphaField;
	private JFormattedTextField betaField;
	private JFormattedTextField colorSatField;
	private JFormattedTextField noiseReducField;
	
	private JSlider alphaSlider;
	private JSlider betaSlider = null;
	private JSlider colorSatSlider = null;
	private JSlider noiseReducSlider = null;
	
	private JLabel alphaLabel = null;
	private JLabel betaLabel = null;
	private JLabel colorSatLabel = null;
	private JLabel noiseReducLabel = null;
	
	private Fattal tmo;
	
	public ToolBarFattal(){
		if(FastHDR.getFrame() != null){
			tmo = new Fattal(FastHDR.getFrame(), FastHDR.getImagePanel());
		}
		performTMO();
	}
	
	@Override
	public void addWidgets(JToolBar toolBar){
		addAlphaWidget(toolBar);
		addBetaWidget(toolBar);
		addColorSatWidget(toolBar);
		addNoiseReducWidget(toolBar);
	}
	
	@Override
	public void removeWidgets(JToolBar toolBar) {
		toolBar.remove(alphaLabel);
		toolBar.remove(alphaField);
		toolBar.remove(alphaSlider);
		
		toolBar.remove(betaLabel);
		toolBar.remove(betaField);
		toolBar.remove(betaSlider);
		
		toolBar.remove(colorSatLabel);
		toolBar.remove(colorSatField);
		toolBar.remove(colorSatSlider);
		
		toolBar.remove(noiseReducLabel);
		toolBar.remove(noiseReducField);
		toolBar.remove(noiseReducSlider);
	}

	@Override
	public void performTMO() {
		if(tmo != null){ tmo.performTMOwithPreview(alpha, beta, colorSat, noiseReduc); }
	}

	@Override
	public void cancelTMO() {
		if(tmo != null){ tmo.cancel(); }
	}
	
	public void addAlphaWidget(JToolBar toolBar){
		// Create a label, slider and textField
		// Each slider notch is 0.02.
		final int notches = 100;
		alphaSlider = new JSlider(JSlider.HORIZONTAL, 0, notches, (int)(ALPHA_DEFAULT * notches / ALPHA_MAX));
		NumberFormat format = NumberFormat.getNumberInstance();
		alphaField = new JFormattedTextField(format);
		
		alphaSlider.setPreferredSize(new Dimension(150, 20));
		alphaSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				// Update bias model
				alpha = alphaSlider.getValue() / (double)notches * ALPHA_MAX;
				
				// Update bias text field
				alphaField.setValue(alpha);
				
				// Run tmo!
				performTMO();
			}
		});

		// Create label
		alphaLabel = new JLabel("A");
		alphaLabel.setLabelFor(alphaSlider);
		
		alphaField.setValue(ALPHA_DEFAULT);
		alphaField.setColumns(3);
		alphaField.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				// Update bias model
				alpha = ((Number)alphaField.getValue()).doubleValue();
		        
				// Clamp range
				if(alpha < ALPHA_MIN){ alpha = ALPHA_MIN; }
		        if(alpha > ALPHA_MAX){ alpha = ALPHA_MAX; }
		        
		        // Update bias text field after clamp
		        alphaField.setValue(alpha);
		        
		        // Update slider (this will invoke the ChangeListener
		        // causing the tmo to run)
		        alphaSlider.setValue((int)(alpha * notches / ALPHA_MAX));
			}
		});
		
		// Add components to toolBar
		toolBar.add(alphaLabel);
		toolBar.add(alphaSlider);
		toolBar.add(alphaField);
	}
	
	public void addBetaWidget(JToolBar toolBar){
		// Create a label, slider and textField
		// Each slider notch is 0.01. BETA_MAX / notches = 1 / 100 = 0.01 
		final int notches = 100;
		betaSlider = new JSlider(JSlider.HORIZONTAL, 0, notches, (int)(BETA_DEFAULT * notches / BETA_MAX));
		NumberFormat format = NumberFormat.getNumberInstance();
		betaField = new JFormattedTextField(format);
		
		betaSlider.setPreferredSize(new Dimension(150, 20));
		betaSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				// Update bias model
				beta = betaSlider.getValue() / (double)notches * BETA_MAX;
				
				// Update bias text field
				betaField.setValue(beta);
				
				// Run tmo!
				performTMO();
			}
		});

		// Create label
		betaLabel = new JLabel("B");
		betaLabel.setLabelFor(betaSlider);
		
		betaField.setValue(BETA_DEFAULT);
		betaField.setColumns(3);
		betaField.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				// Update bias model
				beta = ((Number)betaField.getValue()).doubleValue();
		        
				// Clamp range
				if(beta < BETA_MIN){ beta = BETA_MIN; }
		        if(beta > BETA_MAX){ beta = BETA_MAX; }
		        
		        // Update bias text field after clamp
		        betaField.setValue(beta);
		        
		        // Update slider (this will invoke the ChangeListener
		        // causing the tmo to run)
		        betaSlider.setValue((int)(beta * notches / BETA_MAX));
			}
		});
		
		// Add components to toolBar
		toolBar.add(betaLabel);
		toolBar.add(betaSlider);
		toolBar.add(betaField);
	}
	
	public void addColorSatWidget(JToolBar toolBar){
		// Create a label, slider and textField
		// Each slider notch is 0.04. COLOR_SAT_MAX / notches = 4 / 100 = 0.04 
		final int notches = 100;
		colorSatSlider = new JSlider(JSlider.HORIZONTAL, 0, notches, (int)(COLOR_SAT_DEFAULT * notches / COLOR_SAT_MAX));
		NumberFormat format = NumberFormat.getNumberInstance();
		colorSatField = new JFormattedTextField(format);
		
		colorSatSlider.setPreferredSize(new Dimension(150, 20));
		colorSatSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				// Update bias model
				colorSat = colorSatSlider.getValue() / (double)notches * COLOR_SAT_MAX;
				
				// Update bias text field
				colorSatField.setValue(colorSat);
				
				// Run tmo!
				performTMO();
			}
		});

		// Create label
		colorSatLabel = new JLabel("Color Sat.");
		colorSatLabel.setLabelFor(colorSatSlider);
		
		colorSatField.setValue(COLOR_SAT_DEFAULT);
		colorSatField.setColumns(3);
		colorSatField.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				// Update bias model
				colorSat = ((Number)colorSatField.getValue()).doubleValue();
		        
				// Clamp range
				if(colorSat < COLOR_SAT_MIN){ colorSat = COLOR_SAT_MIN; }
		        if(colorSat > COLOR_SAT_MAX){ colorSat = COLOR_SAT_MAX; }
		        
		        // Update bias text field after clamp
		        colorSatField.setValue(colorSat);
		        
		        // Update slider (this will invoke the ChangeListener
		        // causing the tmo to run)
		        colorSatSlider.setValue((int)(colorSat * notches/ COLOR_SAT_MAX));
			}
		});
		
		// Add components to toolBar
		toolBar.add(colorSatLabel);
		toolBar.add(colorSatSlider);
		toolBar.add(colorSatField);
	}
	
	public void addNoiseReducWidget(JToolBar toolBar){
		// Create a label, slider and textField
		// Each slider notch is 0.001. NOISE_REDUC_MAX / notches = 1 / 1000 = 0.001 
		final int notches = 1000;
		noiseReducSlider = new JSlider(JSlider.HORIZONTAL, 0, notches, (int)(NOISE_REDUC_DEFAULT * notches / NOISE_REDUC_MAX));
		NumberFormat format = NumberFormat.getNumberInstance();
		noiseReducField = new JFormattedTextField(format);
		
		noiseReducSlider.setPreferredSize(new Dimension(150, 20));
		noiseReducSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				// Update bias model
				noiseReduc = noiseReducSlider.getValue() / (double)notches * NOISE_REDUC_MAX;
				
				// Update bias text field
				noiseReducField.setValue(noiseReduc);
				
				// Run tmo!
				performTMO();
			}
		});

		// Create label
		noiseReducLabel = new JLabel("Noise Reduc.");
		noiseReducLabel.setLabelFor(noiseReducSlider);
		
		noiseReducField.setValue(NOISE_REDUC_DEFAULT);
		noiseReducField.setColumns(3);
		noiseReducField.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				// Update bias model
				noiseReduc = ((Number)noiseReducField.getValue()).doubleValue();
		        
				// Clamp range
				if(noiseReduc < NOISE_REDUC_MIN){ noiseReduc = NOISE_REDUC_MIN; }
		        if(noiseReduc > NOISE_REDUC_MAX){ noiseReduc = NOISE_REDUC_MAX; }
		        
		        // Update bias text field after clamp
		        noiseReducField.setValue(noiseReduc);
		        
		        // Update slider (this will invoke the ChangeListener
		        // causing the tmo to run)
		        noiseReducSlider.setValue((int)(noiseReduc * notches / NOISE_REDUC_MAX));
			}
		});
		
		// Add components to toolBar
		toolBar.add(noiseReducLabel);
		toolBar.add(noiseReducSlider);
		toolBar.add(noiseReducField);
	}
}

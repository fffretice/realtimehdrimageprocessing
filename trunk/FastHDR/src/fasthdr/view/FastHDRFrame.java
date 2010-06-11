/*
	FastHDR frame widget
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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import fasthdr.controller.FastHDR;
import fasthdr.exception.MalformedFileException;
import fasthdr.io.BufferedImageConverter;
import fasthdr.io.FileOpenTypeFilter;
import fasthdr.io.FileSaveTypeFilter;

public class FastHDRFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	
	public static final String[] tmoStrings = {"Linear", "Log", "Drago", "Fattal"};
	
	private final JFrame frame;
	
	// Menu bar components
	private JMenuBar menuBar;
	private JMenuItem menu_file_open;
	private JMenuItem menu_file_save;
	private JMenuItem menu_file_exit;
	
	private JMenuItem menu_view_placeholder;
	private JMenuItem menu_tools_placeholder;
	private JMenuItem menu_help_about;
	
	//Create a file chooser
	private final JFileChooser fileOpener;
	private final JFileChooser fileSaver;
	
	// Image Panel
	private ImagePanel imagePanel;
	
	// Tool bar components
	private JToolBar toolBar;
	private JComboBox tmoDropDown;
	private ToolBarInterface tmoToolBarComponents; 
	
	public FastHDRFrame(){
		// Name.
		super("Fast HDR");
		frame = this;
		
		// Set the "X" button to close the application.
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		// Default size.
		this.setSize(1200, 900);
		
		// Create open file chooser
		fileOpener = new JFileChooser();
		fileOpener.setFileFilter(new FileOpenTypeFilter());
		
		// Create save file chooser
		fileSaver = new JFileChooser();
		fileSaver.setFileFilter(new FileSaveTypeFilter());
		
		// Create menu bar
		buildMenu();
		
		// Create image panel
		buildImagePanel();
		
		// Create toolBar
		buildToolBar();
		
		// Add all components to JFrame
		this.setJMenuBar(menuBar);
        this.getContentPane().add(toolBar, BorderLayout.NORTH);
		this.add(imagePanel);
	}

	private void buildMenu(){
		// Create the menu bar.
		menuBar = new JMenuBar();
		
		// Setup the "File" menu bar.
		JMenu menu_file = new JMenu("File");
		menu_file_open = new JMenuItem("Open...");
		menu_file_open.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent e) {
				EventQueue.invokeLater(new Runnable() { 
					@Override
					public void run() { 
				        int returnVal = fileOpener.showOpenDialog(frame);

				        if(returnVal == JFileChooser.APPROVE_OPTION) {
				        	try {
				        		// Cancel currently running TMOs
				        		if(tmoToolBarComponents != null){ tmoToolBarComponents.cancelTMO(); }
								FastHDR.setFile(fileOpener.getSelectedFile());
								
								// Set to linear TMO
								tmoDropDown.setSelectedIndex(0);
							} 
				        	catch (FileNotFoundException e) {
								e.printStackTrace();
							} 
				        	catch (IOException e) {
								e.printStackTrace();
							} 
				        	catch (MalformedFileException e) {
								e.printStackTrace();
							}
				        }
		            } 
		        });
			}
		});
		
		menu_file_save = new JMenuItem("Save...");
		menu_file_save.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				EventQueue.invokeLater(new Runnable() { 
					@Override
					public void run() { 
						if(imagePanel.getImage() != null){
							int returnVal = fileSaver.showSaveDialog(frame);
							if(returnVal == JFileChooser.APPROVE_OPTION) {
								try {
									File outFile = fileSaver.getSelectedFile();
									
										BufferedImage bi = BufferedImageConverter.createBufferedImage(imagePanel.getImage(), frame);
									    ImageIO.write(bi, "png", outFile);
								} 
								catch (IOException e) {
									e.printStackTrace();
								}
							}	
						}
						else{
							JOptionPane.showMessageDialog(frame,
								"There is no image to save.",
								"Warning",
								JOptionPane.WARNING_MESSAGE);
						}
		            } 
		        });
			}
		});
		
		menu_file_exit = new JMenuItem("Exit");
		
		menu_file_exit.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		menu_file.add(menu_file_open);
		menu_file.add(menu_file_save);
		menu_file.addSeparator();
		menu_file.add(menu_file_exit);
		
		// Setup the "View" menu bar.
		JMenu menu_view = new JMenu("View");
		menu_view_placeholder = new JMenuItem("Placeholder...");
		menu_view.add(menu_view_placeholder);
		
		// Setup the "Tools" menu bar.
		JMenu menu_tools = new JMenu("Tools");
		menu_tools_placeholder = new JMenuItem("Placeholder...");
		menu_tools.add(menu_tools_placeholder);
		
		// Setup the "Help" menu bar.
		JMenu menu_help = new JMenu("Help");
		menu_help_about = new JMenuItem("About");
		menu_help_about.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(frame,
						"FastHDR v1.0 by Edward Duong",
						"About",
						JOptionPane.INFORMATION_MESSAGE);
			}
			
		});
		
		menu_help.add(menu_help_about);
		
		menuBar.add(menu_file);
		menuBar.add(menu_view);
		menuBar.add(menu_tools);
		menuBar.add(menu_help);
	}
	
	private void buildImagePanel(){
		imagePanel = new ImagePanel();
		FastHDR.setImagePanel(imagePanel);
	}
	
	private void buildToolBar(){
		toolBar = new JToolBar("Tone Map Parameters");
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setHgap(10);
		toolBar.setLayout(layout);
		
		tmoDropDown = new JComboBox(tmoStrings);
		tmoDropDown.setSelectedIndex(0);
		ComboBoxActionListener actionListener = new ComboBoxActionListener();
		tmoDropDown.addActionListener(actionListener);
		toolBar.add(tmoDropDown);
		toolBar.addSeparator();
	}
	
	public class ComboBoxActionListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
	        ItemSelectable is = (ItemSelectable)e.getSource();
	        String s = (String)(is.getSelectedObjects()[0]);
	        
	        // Change the toolbar (queued to AWT)
	        if(s.equals("Linear")){
	        	loadLinear();
	        }
	        else if(s.equals("Log")){
	        	loadLog();
	        }
	        else if(s.equals("Drago")){
	        	loadDrago();
	        }
			else if(s.equals("Fattal")){
				loadFattal();
			}
		}		
	}
	
	private void loadLinear(){
		EventQueue.invokeLater(new Runnable() { 
			@Override
			public void run() { 
            	// Remove existing widgets
				if(tmoToolBarComponents != null){ tmoToolBarComponents.removeWidgets(toolBar); }
				
				// Create new components
				tmoToolBarComponents = new ToolBarLinear();
				tmoToolBarComponents.addWidgets(toolBar);
				
				// Re-draw components
				toolBar.revalidate();
				toolBar.repaint();
				
				// Initialize resources, run algorithm and display
				tmoToolBarComponents.performTMO();
            } 
        });
	}
	
	private void loadLog(){
		EventQueue.invokeLater(new Runnable() { 
			@Override
			public void run() { 
            	// Remove existing widgets
				if(tmoToolBarComponents != null){ tmoToolBarComponents.removeWidgets(toolBar); }
				
				// Create new components
				tmoToolBarComponents = new ToolBarLog();
				tmoToolBarComponents.addWidgets(toolBar);
				
				// Re-draw components
				toolBar.revalidate();
				toolBar.repaint();
				
				// Initialize resources, run algorithm and display
				tmoToolBarComponents.performTMO();
            } 
        });
	}
	
	private void loadDrago(){
		EventQueue.invokeLater(new Runnable() { 
			@Override
			public void run() { 
            	// Remove existing widgets
				if(tmoToolBarComponents != null){ tmoToolBarComponents.removeWidgets(toolBar); }
				
				// Create new components
				tmoToolBarComponents = new ToolBarDrago();
				tmoToolBarComponents.addWidgets(toolBar);
				
				// Re-draw components
				toolBar.revalidate();
				toolBar.repaint();
            } 
        });
	}
	
	private void loadFattal(){
		EventQueue.invokeLater(new Runnable() { 
			@Override
			public void run() { 
            	// Remove existing widgets
				if(tmoToolBarComponents != null){ tmoToolBarComponents.removeWidgets(toolBar); }
				
				// Create new components
				tmoToolBarComponents = new ToolBarFattal();
				tmoToolBarComponents.addWidgets(toolBar);
				
				// Re-draw components
				toolBar.revalidate();
				toolBar.repaint();
				
				//tmoToolBarComponents.performTMO();
            } 
        });
	}
}

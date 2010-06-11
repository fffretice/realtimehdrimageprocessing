/*
	FastHDR image panel widget
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.MemoryImageSource;

import javax.swing.JPanel;

import fasthdr.model.HDRChannel;

public class ImagePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;

	private ImagePanel panel;
	
	private int imageX = 0;
	private int imageY = 0;
	private Image javaImage = null;
	
	public ImagePanel(){
		this.panel = this;
		this.addMouseListener(new MouseListener(){
			private int xStart;
			private int yStart;
			
			@Override
			public void mouseClicked(MouseEvent arg0) {}
			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// Left click.
				if((arg0.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK){
					xStart = arg0.getX();
					yStart = arg0.getY();
				}
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// Left click.
				if((arg0.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK){
					imageX += arg0.getX() - xStart;
					imageY += arg0.getY() - yStart;
					panel.repaint();
				}
			}
		});
	}
	
	public synchronized void setImage(HDRChannel r, HDRChannel g, HDRChannel b){
		double[] rData = r.getData();
		double[] gData = g.getData();
		double[] bData = b.getData();
		
		int size = r.getSize();
	    int width = r.getWidth();
	    int height = r.getHeight();
	    
	    int[] pixels = new int[size];
	    generateImage(pixels, rData, gData, bData, 0, size - 1);
        
	    javaImage = createImage(new MemoryImageSource(width, height, pixels, 0, width));
	    repaint();
	}
	
	private void generateImage(final int[] pixels, final double[] rData, final double[] gData, final double[] bData, final int a, final int b){
    	// Base Case
		if(b - a < 2000000){
    		for(int i = a; i <= b; i++){
    			pixels[i] = ((0xff<<24) | ((int)rData[i]<<16) | ((int)gData[i]<<8) | (int)bData[i]);
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ generateImage(pixels, rData, gData, bData, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ generateImage(pixels, rData, gData, bData, halfSplit + 1, b); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	public synchronized Image getImage(){
		return javaImage;
	}
	
	public void centerImage(){
		imageX = 0;
	    imageY = 0;
	    repaint();
	}
	
	public void paint(Graphics g) {
		g.setColor(Color.lightGray);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		
		if(javaImage != null){
			g.drawImage(javaImage, imageX, imageY, this);
		}
    }
}

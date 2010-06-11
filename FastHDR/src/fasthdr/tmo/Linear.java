/*
	Linear tone mapping algorithm.
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
    Refence: pfstmo library
*/

package fasthdr.tmo;

import fasthdr.colorspace.ColorSpaceConverter;
import fasthdr.exception.UnknownColorSpaceException;
import fasthdr.model.HDRChannel;
import fasthdr.model.HDRFrame;
import fasthdr.view.ImagePanel;

public class Linear {

	public static final double RANGE_MIN = 0;
	public static final double RANGE_MAX = 255;
	
	//////Read only. Do not modify contents. //////
	private final HDRFrame frame;
	private final HDRChannel xChannel;
	private final HDRChannel yChannel;
	private final HDRChannel zChannel;
	private final ImagePanel displayPanel;
	private final int size;
	///////////////////////////////////////////////

	private HDRFrame reusableFrame = null;
	private Thread thread = null;
	
	public Linear(HDRFrame fr, ImagePanel panel){
		frame = fr;
		displayPanel = panel;
		xChannel = frame.getChannel("X");
		yChannel = frame.getChannel("Y");
		zChannel = frame.getChannel("Z");
		size = frame.getSize();
		
		// Generate resusable frame with channel copies
		reusableFrame = new HDRFrame(frame.getWidth(), frame.getHeight());
		reusableFrame.copyAddChannelData(xChannel, yChannel, zChannel);
	}
	
	public void performTMO(){
		if(frame == null){
			return;
		}
		
		thread = new Thread(new LinearRunnable());
		thread.start();
	}
	
	public void cancelTMO() {
		
	}
	
	public class LinearRunnable implements Runnable{
		@Override
		public void run() {
			long start = System.currentTimeMillis();
			
			// Prepare and clean-up the resuableFrame
			// Re-use channels R, G, B for X, Y, Z if they exist, otherwise X, Y, Z should already exist
			if(reusableFrame.getChannel("R") != null){ reusableFrame.renameChannel("R", "X"); }
			if(reusableFrame.getChannel("G") != null){ reusableFrame.renameChannel("G", "Y"); }
			if(reusableFrame.getChannel("B") != null){ reusableFrame.renameChannel("B", "Z"); }
			
			// Copy original frame data
			reusableFrame.copyChannelData(xChannel, yChannel, zChannel, "X", "Y", "Z");
			
			try {
				ColorSpaceConverter.convertXYZtoRGB(reusableFrame, ColorSpaceConverter.CIE_XYZ_2DEGREE, ColorSpaceConverter.SRGB);
			} 
			catch (UnknownColorSpaceException e) {
				e.printStackTrace();
			}
			
			double[] rData = reusableFrame.getChannel("R").getData();
			double[] gData = reusableFrame.getChannel("G").getData();
			double[] bData = reusableFrame.getChannel("B").getData();
			
			// Find min/max r, g and b values.
			// There is a faster min/max algo that could be implemented
			double minMaxPairs[] = new double[6];
			minMaxPairs[0] = Double.MAX_VALUE;
			minMaxPairs[2] = Double.MAX_VALUE;
			minMaxPairs[4] = Double.MAX_VALUE;
			findMinMax(rData, gData, bData, minMaxPairs, 0, size - 1);
			
			// Shift all data to 0 and re-scale r, g and b value to 0-255 range 
			shiftRescale(rData, gData, bData, minMaxPairs, 0, size - 1);
			
			displayPanel.setImage(reusableFrame.getChannel("R"), reusableFrame.getChannel("G"), reusableFrame.getChannel("B"));
			System.out.println("Linear Full: " + (System.currentTimeMillis() - start));
		}
	}
	
	private void findMinMax(final double[] aData, final double[] bData, final double[] cData, final double[] minMaxPairs, final int a, final int b){
		// minMaxPairs[0] stores aData min, minMaxPairs[1] stores aData max
    	// minMaxPairs[2] stores bData min, minMaxPairs[3] stores bData max
    	// minMaxPairs[4] stores cData min, minMaxPairs[5] stores cData max
		if(b - a < 2000000){
			for(int i = a; i <= b; i++){
				// Set min
				if(aData[i] < minMaxPairs[0]){ minMaxPairs[0] = aData[i]; }
				if(bData[i] < minMaxPairs[2]){ minMaxPairs[2] = bData[i]; }
				if(cData[i] < minMaxPairs[4]){ minMaxPairs[4] = cData[i]; }
				
				// Set max
				if(aData[i] > minMaxPairs[1]){ minMaxPairs[1] = aData[i]; }
				if(bData[i] > minMaxPairs[3]){ minMaxPairs[3] = bData[i]; }
				if(cData[i] > minMaxPairs[5]){ minMaxPairs[5] = cData[i]; }
			}
    		return;
    	}
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ findMinMax(aData, bData, cData, minMaxPairs, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ findMinMax(aData, bData, cData, minMaxPairs, halfSplit + 1, b); }};
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	private void shiftRescale(final double[] aData, final double[] bData, final double[] cData, final double[] minMaxPairs, final int a, final int b){
		// minMaxPairs[0] stores aData min, minMaxPairs[1] stores aData max
    	// minMaxPairs[2] stores bData min, minMaxPairs[3] stores bData max
    	// minMaxPairs[4] stores cData min, minMaxPairs[5] stores cData max
		if(b - a < 2000000){
			for(int i = a; i <= b; i++){
				if(minMaxPairs[0] < 0){ aData[i] = RANGE_MAX * (aData[i] + (minMaxPairs[0] * -1)) / (minMaxPairs[1] + (minMaxPairs[0] * -1)); }
				else{ aData[i] = RANGE_MAX * (aData[i] - minMaxPairs[0]) / (minMaxPairs[1] - minMaxPairs[0]); }
				
				if(minMaxPairs[2] < 0){ bData[i] = RANGE_MAX * (bData[i] + (minMaxPairs[2] * -1)) / (minMaxPairs[3] + (minMaxPairs[2] * -1)); }
				else{ bData[i] = RANGE_MAX * (bData[i] - minMaxPairs[2]) / (minMaxPairs[3] - minMaxPairs[2]); }
				
				if(minMaxPairs[4] < 0){ cData[i] = RANGE_MAX * (cData[i] + (minMaxPairs[4] * -1)) / (minMaxPairs[5] + (minMaxPairs[4] * -1)); }
				else{ cData[i] = RANGE_MAX * (cData[i] - minMaxPairs[4]) / (minMaxPairs[5] - minMaxPairs[4]); }
			}
    		return;
    	}
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ shiftRescale(aData, bData, cData, minMaxPairs, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ shiftRescale(aData, bData, cData, minMaxPairs, halfSplit + 1, b); }};
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
}

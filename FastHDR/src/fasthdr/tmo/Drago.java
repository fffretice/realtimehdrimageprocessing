/*
	Drago tone mapping algorithm
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
    Reference: pfstmo library, Paper: Adaptive Logarithmic Mapping For Displaying High Contrast Scenes
*/

package fasthdr.tmo;

import fasthdr.colorspace.ColorSpaceConverter;
import fasthdr.model.HDRChannel;
import fasthdr.model.HDRFrame;
import fasthdr.util.GaussianPyramid;
import fasthdr.util.Subsampler;
import fasthdr.view.ImagePanel;


public class Drago {
	
	//////Read only. Do not modify contents. //////
	// Full TMO
	private final HDRFrame frame;
	private final ImagePanel displayPanel;
	private final int lumSize;
	private final double lumPixFloor[];
	private final double maxLuminance;
	private final double avgLuminance;
	private final double divider;
	
	// Preview TMO
	private final HDRChannel[] xChannelSubsamples;
	private final HDRChannel[] yChannelSubsamples;
	private final HDRChannel[] zChannelSubsamples;
	///////////////////////////////////////////////
	
	private double biasP;
	
	private boolean cancel = false;
	private Thread thread = null;
	
	public Drago(HDRFrame inFrame, ImagePanel panel){
		long start = System.currentTimeMillis();
		// PREPROCESS
		frame = inFrame;
		displayPanel = panel;
		
		HDRChannel xChannel = frame.getChannel("X");
		HDRChannel yChannel = frame.getChannel("Y");
		HDRChannel zChannel = frame.getChannel("Z");
		
		double[] yData = yChannel.getData();
		int size = frame.getSize();
		
		// PREPROCESS TMO
		// Calculate average luminance and maximum luminance
		double max = 0;
		double avg = 0;
		for(int i = 0 ; i < size; i++) {
			avg += Math.log(yData[i] + 1e-4);
			max = (yData[i] > max) ? yData[i] : max ;
		}
		avgLuminance = Math.exp(avg / size);
		
		// Normalize
		maxLuminance = max / avgLuminance;
		
		// Set divider
		divider = Math.log10(maxLuminance + 1.0);
		
		// Inverse mapping: linear
		// It is preference to have slightly more solid black 0 and solid white 255 in spectrum
		// by stretching a mapping
		// In the future: This can be preprocessed on application startup instead.
		lumSize = 258;
		lumPixFloor = new double[lumSize];
		
		for(int p = 1; p < lumSize; p++) {				
			lumPixFloor[p] = (double)(p - 1) / 255;
		}
		
		// PREPROCESS PREVIEW
		yChannelSubsamples = Subsampler.quadrantDownSample(yChannel);
		xChannelSubsamples = Subsampler.quadrantDownSample(xChannel);
		zChannelSubsamples = Subsampler.quadrantDownSample(zChannel);
		
		System.out.println("Drago Preprocess: " + (System.currentTimeMillis() - start));
	}
	
	public void performTMOwithPreview(double bias){
		if(frame == null){ return; }
		if(thread != null && thread.isAlive()){ cancel(); }
		
		thread = new Thread(new DragoTMOwithPreview(bias));
		thread.start();
	}
	
	// Cancel TMO and preview
	public void cancel(){
		cancel = true;
		try { thread.join(); } 
		catch (InterruptedException e) { e.printStackTrace(); }
		cancel = false;
		thread = null;
	}
	
	public class DragoTMOwithPreview implements Runnable{
		
		private final double bias;
		
		public DragoTMOwithPreview(double b){ 
			bias = b;
		}
		
		// Perform TMO with NO preview
		/*@Override
		public void run() {
			HDRFrame resultFrame1 = tonemap(bias, frame.getChannel("X"), frame.getChannel("Y"), frame.getChannel("Z"));
			displayPanel.setImage(resultFrame1.getChannel("R"), resultFrame1.getChannel("G"), resultFrame1.getChannel("B"));
		}*/
		
		@Override
		public void run() {
			long start = System.currentTimeMillis();
			
			// TMO top left pixels
			HDRFrame resultFrame1 = tonemap(xChannelSubsamples[0], yChannelSubsamples[0], zChannelSubsamples[0], bias);
			if(cancel == true){ return; }
			
			// Display preview
			displayPanel.setImage(GaussianPyramid.upSample(resultFrame1.getChannel("R")), 
					  GaussianPyramid.upSample(resultFrame1.getChannel("G")), 
					  GaussianPyramid.upSample(resultFrame1.getChannel("B")));
			
			System.out.println("Drago Preview: " + (System.currentTimeMillis() - start));
			
			start = System.currentTimeMillis();
			// TMO top right pixels (We could reconstruct the image incrementally with 
			// Subsampler.quadrantPartialReconstruction(...)), apply a Gaussian blur
			// and re-display (for a slightly more accurate preview). I find the results 
			// of doing so are not worth the extra computation.
			HDRFrame resultFrame2 = tonemap(xChannelSubsamples[1], yChannelSubsamples[1], zChannelSubsamples[1], bias);
			if(cancel == true){ return; }
			
			// TMO bottom left pixels
			HDRFrame resultFrame3 = tonemap(xChannelSubsamples[2], yChannelSubsamples[2], zChannelSubsamples[2], bias);
			if(cancel == true){ return; }
			
			// TMO bottom right pixels
			HDRFrame resultFrame4 = tonemap(xChannelSubsamples[3], yChannelSubsamples[3], zChannelSubsamples[3], bias);
			if(cancel == true){ return; }
			
			// Re-construct full image
			if(cancel == true){ return; }
			HDRChannel fullR = Subsampler.quadrantUpSample(resultFrame1.getChannel("R"), resultFrame2.getChannel("R"), 
					resultFrame3.getChannel("R"), resultFrame4.getChannel("R"));
			
			if(cancel == true){ return; }
			HDRChannel fullG = Subsampler.quadrantUpSample(resultFrame1.getChannel("G"), resultFrame2.getChannel("G"), 
					resultFrame3.getChannel("G"), resultFrame4.getChannel("G"));
			
			if(cancel == true){ return; }
			HDRChannel fullB = Subsampler.quadrantUpSample(resultFrame1.getChannel("B"), resultFrame2.getChannel("B"), 
					resultFrame3.getChannel("B"), resultFrame4.getChannel("B"));
			
			
			// Display fully tmo'ed image
			displayPanel.setImage(fullR, fullG, fullB);
			System.out.println("Drago Full: " + (System.currentTimeMillis() - start));
		}
	}
	
	private HDRFrame tonemap(HDRChannel xChannel, HDRChannel yChannel, HDRChannel zChannel, double b){
		// Copy original data
		HDRFrame newFrame = new HDRFrame(xChannel.getWidth(), xChannel.getHeight());
		newFrame.copyAddChannelData(xChannel, yChannel, zChannel);
		
		double[] newXData = newFrame.getChannel("X").getData();
		double[] newYData = newFrame.getChannel("Y").getData();
		double[] newZData = newFrame.getChannel("Z").getData();
		
		// Apply Drago TMO
		biasP = Math.log(b) / -0.693147; // Log(base e)(0.5)
		
		dragoTMO(biasP, newYData, newXData, newYData, newZData, ColorSpaceConverter.CIE_XYZtoSRGB_RGB, 0, newXData.length - 1);
		
		if(cancel == true){ return null; }
		
		// Rename the X, Y, Z channel to R, G, B, respectively
		newFrame.renameChannel("X", "R");
		newFrame.renameChannel("Y", "G");
		newFrame.renameChannel("Z", "B");
		return newFrame;
	}
	
	private static int DRAGO_BASE_CASE = 250000;
	private void dragoTMO(final double biasP, final double[] y, 
			final double[] newXData, final double[] newYData, final double[] newZData,
			final double[][] conversionMatrix, final int a, final int b){
		if(cancel == true){ return; }
		// Base Case
		if(b - a < DRAGO_BASE_CASE){
			double luminanceAvgRatio = 0;
			double newLum = 0;
			double aVal, bVal, cVal;
    		for(int i = a; i <= b; i++){
    			if(cancel == true){ return; }
    			// Core Drago Equation
    			luminanceAvgRatio = y[i] / avgLuminance;
    			newLum = (Math.log(luminanceAvgRatio + 1.0) / Math.log(2.0 + Math.pow(luminanceAvgRatio / maxLuminance, biasP) * 8.0)) / divider;
				
    			// Re-scale to new luminance
    			double scale = newLum / newYData[i];
    			newXData[i] *= scale;
    			newYData[i] *= scale;
    			newZData[i] *= scale;
    			
    			// XYZ colorspace to RGB conversion
    			aVal = newXData[i];
				bVal = newYData[i];
				cVal = newZData[i];
				// Multiply by conversion matrix
				newXData[i] = (conversionMatrix[0][0] * aVal) + (conversionMatrix[0][1] * bVal) + (conversionMatrix[0][2] * cVal);
				newYData[i] = (conversionMatrix[1][0] * aVal) + (conversionMatrix[1][1] * bVal) + (conversionMatrix[1][2] * cVal);
				newZData[i] = (conversionMatrix[2][0] * aVal) + (conversionMatrix[2][1] * bVal) + (conversionMatrix[2][2] * cVal);
				
				// WARNING: at this point newXData, newYData, newZData now represent the R, G, B channel respectively
				
				// Inverse pixel mapping
				newXData[i] = ColorSpaceConverter.pixelBinarySearch(newXData[i], lumPixFloor, lumSize);
				newYData[i] = ColorSpaceConverter.pixelBinarySearch(newYData[i], lumPixFloor, lumSize);
				newZData[i] = ColorSpaceConverter.pixelBinarySearch(newZData[i], lumPixFloor, lumSize);
				
				// Clamp to solid black and solid white
				if(newXData[i] < 0){ newXData[i] = 0; } else if(newXData[i] > 255){ newXData[i] = 255; }
				if(newYData[i] < 0){ newYData[i] = 0; } else if(newYData[i] > 255){ newYData[i] = 255; }
				if(newZData[i] < 0){ newZData[i] = 0; } else if(newZData[i] > 255){ newZData[i] = 255; }
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ dragoTMO(biasP, y, newXData, newYData, newZData, conversionMatrix, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ dragoTMO(biasP, y, newXData, newYData, newZData, conversionMatrix, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
}

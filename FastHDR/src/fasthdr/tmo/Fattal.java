/*
	Fattal Tone mapping algorithm.
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
    Refence: tmo_fattal02.cpp, pfstmo library, Paper: Gradient Domain High Dynamic Range Compression
*/

package fasthdr.tmo;

import java.util.ArrayList;
import java.util.Collections;

import fasthdr.colorspace.ColorSpaceConverter;
import fasthdr.model.HDRChannel;
import fasthdr.model.HDRFrame;
import fasthdr.pde.Multigrid;
import fasthdr.util.CancelSignal;
import fasthdr.util.GaussianBlur;
import fasthdr.util.GaussianPyramid;
import fasthdr.view.ImagePanel;

public class Fattal {
	
	private static final int MIN_GAUSSIAN_PYRAMID = 32;
	private static final int PREVIEW_PIXEL_LIMIT = 250000;
	
	//////Read only variables. Do not modify contents. //////
	// Full TMO
	private final HDRFrame frame;
	private final ImagePanel displayPanel;
	private final int lumSize;
	private final double lumPixFloor[];
	private int pyramidLevels = 0;
	private final HDRChannel normalizedLuminance;
	private final HDRChannel[] gradients;
	private final double[] avgGradients;
	
	// Preview TMO
	private HDRChannel xPreviewChannel;
	private HDRChannel yPreviewChannel;
	private HDRChannel zPreviewChannel;
	private int previewPyramidLevels;
	private final HDRChannel normalizedLuminancePreview;
	private final HDRChannel[] gradientsPreview;
	private final double[] avgGradientsPreview;
	///////////////////////////////////////////////
	
	private CancelSignal cancelSignal;
	private Thread thread = null;
	
	public Fattal(HDRFrame inFrame, ImagePanel panel){
		long start = System.currentTimeMillis();
		
		cancelSignal = new CancelSignal(false);
		
		// PREPROCESS TMO
		frame = inFrame;
		displayPanel = panel;
		
		HDRChannel xChannel = frame.getChannel("X");
		HDRChannel yChannel = frame.getChannel("Y");
		HDRChannel zChannel = frame.getChannel("Z");
		
		double[] yData = yChannel.getData();
		
		int width = xChannel.getWidth();
		int height = xChannel.getHeight();
		int size = xChannel.getSize();
		
		// Y-Channel in CIEL XYZ is the luminance channel
		double minLuminance = yData.length > 0 ? yData[0] : 0;
		double maxLuminance = yData.length > 0 ? yData[0] : 0;
		
		// Find the min and max luminance values
		for(int i = 0; i < size; i++){
			if(yData[i] < minLuminance){ minLuminance = yData[i]; }
			if(yData[i] > maxLuminance){ maxLuminance = yData[i]; }
		}
		
		// Luminance normalized to range 0-100 then apply log (base e). Range should be roughly within -4 to 2.
		normalizedLuminance = new HDRChannel(width, height, "normLum");
		double[] normalizedLuminanceData = normalizedLuminance.getData();
		
		for(int i = 0; i < size; i++){
			normalizedLuminanceData[i] = Math.log(100.0 * (yData[i] / maxLuminance) + 0.0001);
		}
		
		// Create gaussian pyramids.
		// Select minWidthHeight to be the min(width, height)
		int minWidthHeight = Math.min(width, height);
		
		while(minWidthHeight >= MIN_GAUSSIAN_PYRAMID){
			pyramidLevels++;
			minWidthHeight = minWidthHeight / 2;
		}
		
		// Create Gaussian blur pyramid.
		HDRChannel[] pyramid = GaussianPyramid.createDownSamplePyramid(normalizedLuminance, pyramidLevels);
		
		// Calculate average gradients at each pyramid level.
		gradients = new HDRChannel[pyramidLevels];
		avgGradients = new double[pyramidLevels];
		
		for(int i = 0; i < pyramidLevels; i++){
			gradients[i] = new HDRChannel(pyramid[i].getWidth(), pyramid[i].getHeight(), "grads");
			avgGradients[i] = calculateGradients(pyramid[i], gradients[i], i);
		}
		
		// Inverse mapping: linear
		// It is preference to have slightly more solid black 0 and solid white 255 in spectrum
		// by stretching a mapping
		lumSize = 258;
		lumPixFloor = new double[lumSize];
		
		for(int p = 1; p < lumSize; p++) {				
			lumPixFloor[p] = (double)(p - 1) / 255;
		}
		
		// PREPROCESS PREVIEW
		previewPyramidLevels = 0;
		int newWidth = width;
		int newHeight = height;
		
		// Estimate an appropriate preview image size
		while(newWidth * newHeight >= PREVIEW_PIXEL_LIMIT){
			previewPyramidLevels++;
			newWidth = newWidth / 2;
			newHeight = newHeight / 2;
		}
		
		System.out.println("Preview LEVELS " + previewPyramidLevels);
		
		// Copy original channels to preview channels
		xPreviewChannel = new HDRChannel(xChannel.getWidth(), xChannel.getHeight(), xChannel.getName());
		yPreviewChannel = new HDRChannel(yChannel.getWidth(), yChannel.getHeight(), yChannel.getName());
		zPreviewChannel = new HDRChannel(zChannel.getWidth(), zChannel.getHeight(), zChannel.getName());
		
		xPreviewChannel.copyChannelData(xChannel);
		yPreviewChannel.copyChannelData(yChannel);
		zPreviewChannel.copyChannelData(zChannel);
		
		// Down sample the preview channels. This halves their width and height per iteration
		for(int i = 0; i < previewPyramidLevels; i++){
			xPreviewChannel = GaussianPyramid.downSample(xPreviewChannel);
			yPreviewChannel = GaussianPyramid.downSample(yPreviewChannel);
			zPreviewChannel = GaussianPyramid.downSample(zPreviewChannel);
		}
		
		// Preprocess preview images
		// Reusing variables declarations
		yData = yPreviewChannel.getData();
		
		width = xPreviewChannel.getWidth();
		height = xPreviewChannel.getHeight();
		size = xPreviewChannel.getSize();
		
		// Y-Channel in CIEL XYZ is the luminance channel
		minLuminance = yData.length > 0 ? yData[0] : 0;
		maxLuminance = yData.length > 0 ? yData[0] : 0;
		
		// Find the min and max luminance values
		for(int i = 0; i < size; i++){
			if(yData[i] < minLuminance){ minLuminance = yData[i]; }
			if(yData[i] > maxLuminance){ maxLuminance = yData[i]; }
		}
		
		// Luminance normalized to range 0-100 then apply log (base e). Range should be roughly within -4 to 2.
		normalizedLuminancePreview = new HDRChannel(width, height, "normLum");
		double[] normalizedLuminanceDataPreview = normalizedLuminancePreview.getData();
		
		for(int i = 0; i < size; i++){
			normalizedLuminanceDataPreview[i] = Math.log(100.0 * (yData[i] / maxLuminance) + 0.0001);
		}
		
		// Preview should use the same pyramid level for the preview as the original image.
		// Create Gaussian blur pyramid.
		pyramid = GaussianPyramid.createDownSamplePyramid(normalizedLuminancePreview, pyramidLevels);
		
		// Calculate average gradients at each pyramid level.
		gradientsPreview = new HDRChannel[pyramidLevels];
		avgGradientsPreview = new double[pyramidLevels];
		
		for(int i = 0; i < pyramidLevels; i++){
			gradientsPreview[i] = new HDRChannel(pyramid[i].getWidth(), pyramid[i].getHeight(), "grads");
			avgGradientsPreview[i] = calculateGradients(pyramid[i], gradientsPreview[i], i);
		}
		
		System.out.println("Fattal Preprocess: " + (System.currentTimeMillis() - start) + "ms");
	}
	
	private double calculateGradients(HDRChannel pyramidSlice, HDRChannel gradient, int i) {
		int width = pyramidSlice.getWidth();
		int height = pyramidSlice.getHeight();
		int size = pyramidSlice.getSize();
		
		double pyramidData[] = pyramidSlice.getData();
		double gradientData[] = gradient.getData();
		
		double divider = Math.pow(2.0, i+1);
		double avgGradient = 0.0;

		double gx, gy;
		int w, n, e, s;
		
		int y, x;
		for(y = 0; y < height; y++){
			for(x = 0; x < width; x++){
				w = (x == 0 ? 0 : x - 1);
				n = (y == 0 ? 0 : y - 1);
				s = (y + 1 == height ? y : y + 1);
				e = (x + 1 == width ? x : x + 1);
				
				// X-Axis and Y-Axis gradient
				gx = (pyramidData[(y * width) + w] - pyramidData[(y * width) + e]) / divider;
				gy = (pyramidData[(s * width) + x] - pyramidData[(n * width) + x]) / divider;
				
				// Store the gradient data
				gradientData[(y * width) + x] = Math.sqrt(gx * gx + gy * gy);
				
				// Accumulate gradient value
				avgGradient += gradientData[(y * width) + x];
			}
		}
		
		// Return the average gradient value
		return avgGradient / size;
	}
	
	public void performTMOwithPreview(double param_alpha, double param_beta, double param_saturate, double param_noise){
		if(frame == null){ return; }
		if(thread != null && thread.isAlive()){ cancel(); }
		
		thread = new Thread(new FattalTMOwithPreview(param_alpha, param_beta, param_saturate, param_noise, cancelSignal));
		thread.start();
	}
	
	// Cancel TMO and preview
	public void cancel(){
		cancelSignal.cancel();
		try { thread.join(); } 
		catch (InterruptedException e) { e.printStackTrace(); }
		cancelSignal.resume();
		thread = null;
	}
	
	public class FattalTMOwithPreview implements Runnable{
		
		// User params
		private double param_alpha;
		private double param_beta;
		private double param_saturate;
		private double param_noise;
		private CancelSignal cancelSignal;
		
		public FattalTMOwithPreview(double alpha, double beta, double colorSat, double noiseReduc, CancelSignal signal){ 
			param_alpha = alpha;
			param_beta = beta;
			param_saturate = colorSat;
			param_noise = noiseReduc;
			cancelSignal = signal;
		}
		
		@Override
		public void run() {
			long start = System.currentTimeMillis();
			// TMO preview
			HDRFrame previewFrame = tonemap(xPreviewChannel, yPreviewChannel, zPreviewChannel, 
					gradientsPreview, avgGradientsPreview, pyramidLevels, normalizedLuminancePreview,
					param_alpha, param_beta, param_saturate, param_noise);
			if(cancelSignal.isCancelled() == true){ return; }
			
			// Display preview
			HDRChannel rPreviewChannel = previewFrame.getChannel("R");
			HDRChannel gPreviewChannel = previewFrame.getChannel("G");
			HDRChannel bPreviewChannel = previewFrame.getChannel("B");
			
			double[] rData = rPreviewChannel.getData();
			double[] gData = gPreviewChannel.getData();
			double[] bData = bPreviewChannel.getData();
			
			// Increase brightness by 10%
			for(int k = 0; k < rPreviewChannel.getSize(); k++){
				if(rData[k] * 1.16 < 255){ rData[k] = rData[k] * (1 + 0.075 * previewPyramidLevels); }
				if(gData[k] * 1.16 < 255){ gData[k] = gData[k] * (1 + 0.075 * previewPyramidLevels); }
				if(bData[k] * 1.16 < 255){ bData[k] = bData[k] * (1 + 0.075 * previewPyramidLevels); }
			}
			
			for(int k = 0; k < previewPyramidLevels; k++){
				if(cancelSignal.isCancelled() == true){ return; }
				rPreviewChannel = GaussianBlur.gaussianBlur(GaussianPyramid.upSample(rPreviewChannel));
				gPreviewChannel = GaussianBlur.gaussianBlur(GaussianPyramid.upSample(gPreviewChannel));
				bPreviewChannel = GaussianBlur.gaussianBlur(GaussianPyramid.upSample(bPreviewChannel));
			}

			// Display preview image
			displayPanel.setImage(rPreviewChannel, gPreviewChannel, bPreviewChannel);
			if(cancelSignal.isCancelled() == true){ return; }
			
			System.out.println("Fattal Preview: " + (System.currentTimeMillis() - start) + "ms");
			
			start = System.currentTimeMillis();
			// TMO full image
			HDRFrame fullFrame = tonemap(frame.getChannel("X"), frame.getChannel("Y"), frame.getChannel("Z"), 
					gradients, avgGradients, pyramidLevels, normalizedLuminance,
					param_alpha, param_beta, param_saturate, param_noise);
			if(cancelSignal.isCancelled() == true){ return; }
			
			// Display full image
			displayPanel.setImage(fullFrame.getChannel("R"), fullFrame.getChannel("G"), fullFrame.getChannel("B"));
			
			System.out.println("Fattal Full: " + (System.currentTimeMillis() - start) + "ms");
		}
	}
	
	private HDRFrame tonemap(HDRChannel xChannel, HDRChannel yChannel, HDRChannel zChannel, 
			HDRChannel[] grads, double[] avgGrads, int pyramidLvls, HDRChannel normLum,
			double alpha, double beta, double colorSat, double noiseReduc){
		int width = xChannel.getWidth();
		int height = xChannel.getHeight();
		int size = xChannel.getSize();
		
		// Copy original data
		HDRFrame newFrame = new HDRFrame(xChannel.getWidth(), xChannel.getHeight());
		newFrame.copyAddChannelData(xChannel, yChannel, zChannel);
		
		double[] newXData = newFrame.getChannel("X").getData();
		double[] newYData = newFrame.getChannel("Y").getData();
		double[] newZData = newFrame.getChannel("Z").getData();
		
		// Calculate scaling factor matrix.
		HDRChannel scalingFactor = calculateScalingFactors(grads, avgGrads, pyramidLvls, alpha, beta, noiseReduc);
		if(cancelSignal.isCancelled() == true){ return null; }
		
		double[] gradientXAxisData = new double[width * height];
		double[] gradientYAxisData = new double[width * height];
		
		// Attenuate gradients
		attenuateGradients(normLum.getData(), gradientXAxisData, gradientYAxisData, scalingFactor.getData(), width, height, 0, height - 1);
		if(cancelSignal.isCancelled() == true){ return null; }
		
		// Calculate Divergence
		// Section 5: Implementation
		HDRChannel gradientDivergence = new HDRChannel(width, height, "gradDiv");
		
		calculateDivergence(gradientDivergence.getData(), gradientXAxisData, gradientYAxisData, width, 0, height - 1);
		if(cancelSignal.isCancelled() == true){ return null; }
		
		// Solve partial differential equation using Multigrid framework with a PDE Biconjugate Gradient solver
		long pdeStart = System.currentTimeMillis();
		HDRChannel pdeSolution = new Multigrid(cancelSignal).solve(gradientDivergence);
		if(cancelSignal.isCancelled() == true){ return null; }
		System.out.println("Fattal PDE: " + (System.currentTimeMillis() - pdeStart));
		
		copyExpLuminanceChannel(newYData, pdeSolution.getData(), 0, size -1);
		if(cancelSignal.isCancelled() == true){ return null; }
		    
		// Remove min/max values and re-normalize.
		long sortStart = System.currentTimeMillis();
		double[] minMaxPair = findMaxMinPercentile(newYData, 0.001, 0.995);
		if(cancelSignal.isCancelled() == true){ return null; }
		minMaxPair[1] = minMaxPair[1] - minMaxPair[0];
		System.out.println("Fattal Pixel Sort: " + (System.currentTimeMillis() - sortStart));
		
		long reconStart = System.currentTimeMillis();
		reconstruct(yChannel.getData(), newXData, newYData, newZData, 
				minMaxPair, colorSat, ColorSpaceConverter.CIE_XYZtoSRGB_RGB, 0, size - 1);
		if(cancelSignal.isCancelled() == true){ return null; }
		System.out.println("Fattal Image Reconstruction: " + (System.currentTimeMillis() - reconStart));
		
		// Rename the X, Y, Z channel to R, G, B, respectively
		newFrame.renameChannel("X", "R");
		newFrame.renameChannel("Y", "G");
		newFrame.renameChannel("Z", "B");
		return newFrame;
	}
	
	private HDRChannel calculateScalingFactors(HDRChannel[] gradients, double[] avgGradients, int levels, double alpha, double beta, double noise_reduc) {
		// Section 4: Gradient attenuation function
		// Propagate (linear interpolation) the scaling factor for each pixel starting at the top (smallest resolution) of the pyramid 
		// down to the base (full sized resolution).
		
		int width = gradients[levels - 1].getWidth();
		int height = gradients[levels - 1].getHeight();
		
		HDRChannel[] scalingFactors = new HDRChannel[levels];
		
		// Beginning with top of the pyramid (smallest sized resolution)
		scalingFactors[levels - 1] = new HDRChannel(width, height, "scalingFactors");
		
		// Initialize bottomFI to 1
		scalingFactors[levels - 1].setAllData(1);
		
		// Compute the scaling factor at each level of the pyramid starting at the top
		int k;
		for(k = levels - 1; k >= 0; k--){
			
			width = gradients[k].getWidth();
			height = gradients[k].getHeight();
			double[] gradientData = gradients[k].getData();
			double[] scalingFactorData = scalingFactors[k].getData();
			
			/*for(int y = 0; y < height; y++){
				for(int x = 0; x < width; x++){
					if(cancelSignal.isCancelled()){ return null; }
					double a = alpha * avgGradients[k];
					double value = 1.0;
					
					// Attenuation threshold
					if(gradientData[y * width + x] > 0.0001){
						// Attenuation equation phi (Section 4)
						double gradientNoise = gradientData[y * width + x] + noise_reduc;
						value = a / gradientNoise * Math.pow(gradientNoise / a, beta);
						scalingFactorData[y * width + x] *= value; 
					}
				}
			}*/
			
			calculateScalingFactors(gradientData, scalingFactorData, avgGradients[k], alpha, beta, noise_reduc, 0, width * height - 1);
			if(cancelSignal.isCancelled() == true){ return null; }
			
			// If this is not the base level of the pyramid, initialize the next level
			if(k > 1){
				width = gradients[k - 1].getWidth();
				height = gradients[k - 1].getHeight();
				scalingFactors[k-1] = new HDRChannel(width, height, "scalingFactors");
			}
			
			if(k > 0){
				// Up-sample the current level, apply a Gaussian blur and store the results in the next level.
				// Up-sampling will double the width and height using linear interpolation and pointwise
				// multiplication (Section 4)
				boolean padWidth = false;
				boolean padHeight = false;
				// Down samples (dividing width and height by 2) causes channels to lose 1 pixel row and/or column, add it back.
				if(k > 1 && scalingFactors[k-1].getWidth() != scalingFactors[k].getWidth() * 2){ padWidth = true; }
				if(k > 1 && scalingFactors[k-1].getHeight() != scalingFactors[k].getHeight() * 2){ padHeight = true; }
				scalingFactors[k-1] = GaussianBlur.gaussianBlur(GaussianPyramid.upSample(scalingFactors[k], padWidth, padHeight));
			}
		}
		
		// Return the base channel of the pyramid
		return scalingFactors[0];
	}
	
	private static int SCALING_FACTOR_BASE_CASE = 250000;
	private void calculateScalingFactors(final double[] gradientData, final double[] scalingFactorData, 
			final double avgGradients, final double alpha, final double beta, final double noise_reduc,
			final int a, final int b){
		if(cancelSignal.isCancelled() == true){ return; }
		// Base Case
		if(b - a < SCALING_FACTOR_BASE_CASE){
    		double avgGradientAlpha, value, gradientNoise;
			for(int i = a; i <= b; i++){
				if(cancelSignal.isCancelled() == true){ return; }
				avgGradientAlpha = alpha * avgGradients;
				value = 1.0;
				
				// Attenuation threshold
				if(gradientData[i] > 0.0001){
					// Attenuation equation phi (Section 4)
					gradientNoise = gradientData[i] + noise_reduc;
					value = avgGradientAlpha / gradientNoise * Math.pow(gradientNoise / avgGradientAlpha, beta);
					scalingFactorData[i] *= value; 
    			}
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ calculateScalingFactors(gradientData, scalingFactorData, avgGradients, alpha, beta, noise_reduc, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ calculateScalingFactors(gradientData, scalingFactorData, avgGradients, alpha, beta, noise_reduc, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	private static int ATTENUATE_GRADIENT_BASE_CASE_ROWS = 1000;
	private void attenuateGradients(final double[] normLumData, final double[] gradientXAxisData, final double[] gradientYAxisData,
			final double[] scalingFactorData, final int width, final int height,
			final int rowStart, final int rowEnd){
		if(cancelSignal.isCancelled() == true){ return; }
		// Base Case
		if(rowEnd - rowStart < ATTENUATE_GRADIENT_BASE_CASE_ROWS){
			int x, y, s, e;
			for(y = rowStart; y <= rowEnd; y++){
				for(x = 0; x < width; x++ ){
					if(cancelSignal.isCancelled() == true){ return; }
					// Edge case guard
					s = (y + 1 == height ? y : y + 1);
					e = (x + 1 == width ? x : x + 1);    // Right neighbor pixel
					gradientXAxisData[(y * width) + x] = (normLumData[(y * width) + e] - normLumData[(y * width) + x]) * scalingFactorData[(y * width) + x];
														 // Bottom neighbor pixel
					gradientYAxisData[(y * width) + x] = (normLumData[(s * width) + x] - normLumData[(y * width) + x]) * scalingFactorData[(y * width) + x];
				}
			}
    		return;
    	}
		// Recurse
    	final int halfSplit = (rowEnd - rowStart) / 2 + rowStart;
    	Thread t1 = new Thread(){ public void run(){ attenuateGradients(normLumData, gradientXAxisData, gradientYAxisData, scalingFactorData, width, height, rowStart, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ attenuateGradients(normLumData, gradientXAxisData, gradientYAxisData, scalingFactorData, width, height, halfSplit + 1, rowEnd); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	private static int DIVERGENCE_BASE_CASE_ROWS = 1000;
	private void calculateDivergence(final double[] gradientDivergenceData, 
			final double[] gradientXAxisData, final double[] gradientYAxisData,
			final int width, final int rowStart, final int rowEnd){
		if(cancelSignal.isCancelled() == true){ return; }
		// Base Case
		if(rowEnd - rowStart < DIVERGENCE_BASE_CASE_ROWS){
			int index, x, y;
			for(y = rowStart; y <= rowEnd; y++){
				for(x = 0; x < width; x++ ){
					if(cancelSignal.isCancelled() == true){ return; }
					// Equation DivG
					index = (y * width) + x;
					gradientDivergenceData[index] = gradientXAxisData[index] + gradientYAxisData[index];
					// Non-edge cases
					if( x > 0 ){ gradientDivergenceData[index] = gradientDivergenceData[index] - gradientXAxisData[index - 1]; }
					if( y > 0 ){ gradientDivergenceData[index] = gradientDivergenceData[index] - gradientYAxisData[index - width]; }
				}
			}
    		return;
    	}
		// Recurse
		final int halfSplit = (rowEnd - rowStart) / 2 + rowStart;
    	Thread t1 = new Thread(){ public void run(){ calculateDivergence(gradientDivergenceData, gradientXAxisData, gradientYAxisData, width, rowStart, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ calculateDivergence(gradientDivergenceData, gradientXAxisData, gradientYAxisData, width, halfSplit + 1, rowEnd); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	private double[] findMaxMinPercentile(double[] luminanceData, double minPercent, double maxPercent) {

		int size = luminanceData.length;

		// Include all non-zero luminance values 
		ArrayList<Double> luminanceValues = new ArrayList<Double>(size);
		for(int i = 0; i < size; i++){
			if(cancelSignal.isCancelled()){ return null; }
			if(luminanceData[i] != 0.0){
				luminanceValues.add(luminanceData[i]);
			}
		}
		
		// Sort luminance values
		Collections.sort(luminanceValues);
		
		// Return the min and max value at the percent
		double[] minMaxPair = new double[2];
		minMaxPair[0] = luminanceValues.get((int)(minPercent * size));
		minMaxPair[1] = luminanceValues.get((int)(maxPercent * size));
		return minMaxPair;
	}
	
	private static int COPY_EXPONENTIATE_LUMINANCE_CHANNEL_BASE_CASE = 250000;
	private void copyExpLuminanceChannel(final double[] newYData, final double[] data, 
			final int a, final int b){
		if(cancelSignal.isCancelled() == true){ return; }
		// Base Case
		if(b - a < COPY_EXPONENTIATE_LUMINANCE_CHANNEL_BASE_CASE){
			for(int i = a; i <= b; i++){
				if(cancelSignal.isCancelled() == true){ return; }
				// Exponentiate the solution
				newYData[i] = Math.exp(data[i]) - 0.0001;
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ copyExpLuminanceChannel(newYData, data, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ copyExpLuminanceChannel(newYData, data, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	private static int RECONSTRUCT_BASE_CASE = 250000;
	private void reconstruct(final double[] yData, final double[] newXData, final double[] newYData, final double[] newZData,
			final double[] minMaxPair, final double colorSat, final double[][] conversionMatrix, 
			final int a, final int b){
		if(cancelSignal.isCancelled() == true){ return; }
		// Base Case
		if(b - a < RECONSTRUCT_BASE_CASE){
			double aVal, bVal, cVal;
			for(int i = a; i <= b; i++){
				if(cancelSignal.isCancelled() == true){ return; }
				// (L - Lmin) / Lmax
				newYData[i] = (newYData[i] - minMaxPair[0]) / minMaxPair[1];
				// Clamp minimum luminance
				if( newYData[i] <= 0.0 ){
					newYData[i] = 0.0001;
				}
				
				// Section 5: C out equation
				// Re-scale the non-luminance components: X and Z
				// Adjust luminance for X and Z channels
				newXData[i] = Math.pow(newXData[i] / yData[i], colorSat) * newYData[i];
				newZData[i] = Math.pow(newZData[i] / yData[i], colorSat) * newYData[i];
				
				// XYZ colorspace to RGB conversion
    			aVal = newXData[i];
				bVal = newYData[i];
				cVal = newZData[i];
				// Multiply by conversion matrix
				newXData[i] = (conversionMatrix[0][0] * aVal) + (conversionMatrix[0][1] * bVal) + (conversionMatrix[0][2] * cVal);
				newYData[i] = (conversionMatrix[1][0] * aVal) + (conversionMatrix[1][1] * bVal) + (conversionMatrix[1][2] * cVal);
				newZData[i] = (conversionMatrix[2][0] * aVal) + (conversionMatrix[2][1] * bVal) + (conversionMatrix[2][2] * cVal);
				
				// WARNING: at this point newX, newY, newZ now represent the R, G, B channel respectively
				
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
    	Thread t1 = new Thread(){ public void run(){ reconstruct(yData, newXData, newYData, newZData, minMaxPair, colorSat, conversionMatrix, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ reconstruct(yData, newXData, newYData, newZData, minMaxPair, colorSat, conversionMatrix, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
}

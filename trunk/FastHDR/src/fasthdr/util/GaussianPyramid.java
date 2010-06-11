/*
	Gaussian Pyramid algorithm
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

package fasthdr.util;

import fasthdr.model.HDRChannel;

public class GaussianPyramid {
	public static HDRChannel[] createDownSamplePyramid(HDRChannel channel, int levels) {
		// Create a pyramid from a given channel. Base at index 0 contains the largest resolution.
		// Each other index (up to levels-1) will contain width / 2 and height / 2, where width and height
		// are width and heights from the channel at index-1.
		
		int width = channel.getWidth();
		int height = channel.getHeight();
		
		// Construct array of HDRChannels. 
		// Index 0 is pyramid slice (k - levels). 
		// The highest index is the pyramid level k.
		HDRChannel[] pyramid = new HDRChannel[levels];
		
		// Pyramid base
		pyramid[0] = new HDRChannel(width, height, channel.getName());
		pyramid[0].copyChannelData(channel);
		
		// Blur the base
		HDRChannel blurredChannel = GaussianBlur.gaussianBlur(pyramid[0]);
		
		// For each pyramid level store and down sample the parent.
		// pyramid at index 0 has full resolution image, each level
		// halves the width and height.
		for(int i = 1; i < levels; i++){
			pyramid[i] = downSample(blurredChannel);
			blurredChannel = GaussianBlur.gaussianBlur(pyramid[i]);
		}
		
		return pyramid;
	}
	
	public static HDRChannel[] createUpSamplePyramid(HDRChannel channel, int levels) {
		// WARNING: This method is memory intensive!
		// Create a pyramid from a given channel. Base at index 0 contains the smallest resolution.
		// Each other index (up to levels-1) will contain width * 2 and height * 2, where width and height
		// are width and heights from the channel at index-1.
	
		int width = channel.getWidth();
		int height = channel.getHeight();
		
		// Construct array of HDRChannels. 
		// Index 0 is pyramid slice (k - levels). 
		// The highest index is the pyramid level k.
		HDRChannel[] pyramid = new HDRChannel[levels];
		
		// Pyramid base
		pyramid[0] = new HDRChannel(width, height, channel.getName());
		pyramid[0].copyChannelData(channel);
		
		// Blur the base
		HDRChannel blurredChannel = GaussianBlur.gaussianBlur(pyramid[0]);
		
		// For each pyramid level store and down sample the parent.
		// pyramid at index 0 has full resolution image, each level
		// halves the width and height.
		for(int i = 1; i < levels; i++){
			pyramid[i] = upSample(blurredChannel);
			blurredChannel = GaussianBlur.gaussianBlur(pyramid[i]);
		}
		
		return pyramid;
	}

	public static HDRChannel downSample(HDRChannel channel) {
		// Halve the width and height
		int halfWidth = channel.getWidth() / 2;
		int halfHeight = channel.getHeight() / 2;
		
		int width = channel.getWidth();
		double data[] = channel.getData();
		
		// Down sampled channel
		HDRChannel downSampleChannel = new HDRChannel(halfWidth, halfHeight, channel.getName());
		double downSampleChannelData[] = downSampleChannel.getData();
		
		new GaussianPyramid().downSample(data, downSampleChannelData, 
				halfWidth, halfHeight, width, 
				0, halfHeight - 1);
		
		return downSampleChannel;
	}
	
	private void downSample(final double[] src, final double[] des, 
			final int halfWidth, final int halfHeight, final int width,
			final int rowStart, final int rowEnd){
		// Base Case (200 or less rows of an image. ~200 * doubleWidth = no. pixels)
		if(rowEnd - rowStart < 1000){
			for(int y = rowStart; y <= rowEnd; y++){
				for(int x = 0; x < halfWidth; x++){
					// Index mapping for the downSampleChannel <-> Original channel
					int halfIndex = (y * halfWidth) + x;
					int index = (2 * y * width) + (2 * x);
					
					// Sample the four corresponding pixels from data and map it to a single pixel
					des[halfIndex] = (src[index] + src[index + 1] + src[index + width] + src[index + width + 1]) / 4.0;
				}
			}

    		return;
    	}
		// Recurse
    	final int halfSplit = (rowEnd - rowStart) / 2 + rowStart;
    	Thread t1 = new Thread(){ public void run(){ downSample(src, des, halfWidth, halfHeight, width, rowStart, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ downSample(src, des, halfWidth, halfHeight, width, halfSplit + 1, rowEnd); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	public static HDRChannel upSample(HDRChannel channel) {
		return upSample(channel, false, false);
	}
	
	public static HDRChannel upSample(HDRChannel channel, boolean padWidth, boolean padHeight) {
		// Double the width and height
		int doubleWidth = channel.getWidth() * 2;
		int doubleHeight = channel.getHeight() * 2;
		
		if(padWidth){ doubleWidth++; }
		if(padHeight){ doubleHeight++; }
		
		int width = channel.getWidth();
		int height = channel.getHeight();
		double data[] = channel.getData();
		
		// Up sampled channel
		HDRChannel upSampleChannel = new HDRChannel(doubleWidth, doubleHeight, channel.getName());
		double upSampleChannelData[] = upSampleChannel.getData();
		
		new GaussianPyramid().upSample(data, upSampleChannelData, 
				doubleWidth, doubleHeight, width, height, 
				0, doubleHeight - 1);
		
		return upSampleChannel;
	}
	
	private void upSample(final double[] src, final double[] des, 
			final int doubleWidth, final int doubleHeight, final int width, final int height, 
			final int rowStart, final int rowEnd){
		// Base Case (200 or less rows of an image. ~200 * doubleWidth = no. pixels)
		if(rowEnd - rowStart < 2000){
			for(int y = rowStart; y <= rowEnd; y++){
    			for(int x = 0; x < doubleWidth; x++){
	    			// Index mapping for the downSampleChannel <-> Original channel
					int doubleIndex = (y * doubleWidth) + x;
					int newX = x / 2;
					int newY = y / 2;
					
					newX = newX < width ? newX : width - 1;
					newY = newY < height ? newY : height - 1;
					
					int index = (newY * width) + (newX);
					
					// Extrapolate pixel information for four pixels from a single pixel in data
					des[doubleIndex] = src[index];
    			}
    		}

    		return;
    	}
		// Recurse
    	final int halfSplit = (rowEnd - rowStart) / 2 + rowStart;
    	Thread t1 = new Thread(){ public void run(){ upSample(src, des, doubleWidth, doubleHeight, width, height, rowStart, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ upSample(src, des, doubleWidth, doubleHeight, width, height, halfSplit + 1, rowEnd); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
}

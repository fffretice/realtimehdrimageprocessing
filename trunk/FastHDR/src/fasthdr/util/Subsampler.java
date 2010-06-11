/*
	Linear sub-sampling algorithm
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

package fasthdr.util;

import fasthdr.model.HDRChannel;

public class Subsampler {

	public static enum Quadrant {TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT};
	
	// Wavelet image down sampling. Each 2x2 pixel blocks in the original is mapped
	// to a different channel (with half of original width and half of original height). 
	public static HDRChannel[] quadrantDownSample(HDRChannel channel) {
		// Halve the width and height
		int halfWidth = channel.getWidth() / 2;
		int halfHeight = channel.getHeight() / 2;
		
		int width = channel.getWidth();
		double data[] = channel.getData();
		
		// Down sampled channel
		HDRChannel downSampleChannels[] = new HDRChannel[4];
		downSampleChannels[0] = new HDRChannel(halfWidth, halfHeight, channel.getName());
		downSampleChannels[1] = new HDRChannel(halfWidth, halfHeight, channel.getName());
		downSampleChannels[2] = new HDRChannel(halfWidth, halfHeight, channel.getName());
		downSampleChannels[3] = new HDRChannel(halfWidth, halfHeight, channel.getName());
		
		double topLeftData[] = downSampleChannels[0].getData();
		double topRightData[] = downSampleChannels[1].getData();
		double bottomLeftData[] = downSampleChannels[2].getData();
		double bottomRightData[] = downSampleChannels[3].getData();
		
		// Sample the four corresponding pixels from data and map it to a single pixel
		quadrantPartition(data, topLeftData, topRightData, bottomLeftData, bottomRightData, 
						  halfWidth, halfHeight, width, 0, (halfWidth * halfHeight) - 1);
		
		return downSampleChannels;
	}
	
	private static void quadrantPartition(final double[] data, 
			final double[] topLeftData, final double[] topRightData, 
			final double[] bottomLeftData, final double[] bottomRightData,
			final int halfWidth, final int halfHeight, final int width,
			final int a, final int b){
    	// Base Case
		if(b - a < 250000){
			// For each pixel
			for(int y = 0; y < halfHeight; y++){
				for(int x = 0; x < halfWidth; x++){
					// Index mapping for the downSampleChannel <-> Original channel
					int halfIndex = (y * halfWidth) + x;
					int index = (2 * y * width) + (2 * x);
					
					// Sample the four corresponding pixels from data and map it to a single pixel
					topLeftData[halfIndex] = data[index];
					topRightData[halfIndex] = data[index + 1];
					bottomLeftData[halfIndex] = data[index + width];
					bottomRightData[halfIndex] = data[index + width + 1];
				}
			}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ quadrantPartition(data, topLeftData, topRightData, bottomLeftData, bottomRightData, halfWidth, halfHeight, width, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ quadrantPartition(data, topLeftData, topRightData, bottomLeftData, bottomRightData, halfWidth, halfHeight, width, halfSplit + 1, b); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	public static HDRChannel quadrantUpSample(HDRChannel topLeft, HDRChannel topRight, HDRChannel bottomLeft, HDRChannel bottomRight) {
		int halfWidth = topLeft.getWidth();
		int halfHeight = topLeft.getHeight();
		
		int width = topLeft.getWidth() * 2;
		
		// Up sampled channel
		HDRChannel upSampleChannel = new HDRChannel(topLeft.getWidth() * 2, topLeft.getHeight() * 2, topLeft.getName());
		double upSampleChannelData[] = upSampleChannel.getData();
		
		quadrantReconstruction(upSampleChannelData, topLeft.getData(), topRight.getData(), bottomLeft.getData(), bottomRight.getData(), 
							   halfWidth, halfHeight, width, 0, (halfWidth * halfHeight) - 1);
		
		return upSampleChannel;
	}
	
	private static void quadrantReconstruction(final double[] data, 
			final double[] topLeftData, final double[] topRightData, 
			final double[] bottomLeftData, final double[] bottomRightData,
			final int halfWidth, final int halfHeight, final int width,
			final int a, final int b){
    	// Base Case
		if(b - a < 250000){
			// For each pixel
			for(int y = 0; y < halfHeight; y++){
				for(int x = 0; x < halfWidth; x++){
					// Index mapping for the downSampleChannel <-> Original channel
					int halfIndex = (y * halfWidth) + x;
					int index = (2 * y * width) + (2 * x);
					
					// Sample the four corresponding pixels from data and map it to a single pixel
					data[index] = topLeftData[halfIndex];
					data[index + 1] = topRightData[halfIndex];
					data[index + width] = bottomLeftData[halfIndex];
					data[index + width + 1] = bottomRightData[halfIndex];
				}
			}
			
			return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ quadrantReconstruction(data, topLeftData, topRightData, bottomLeftData, bottomRightData, halfWidth, halfHeight, width, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ quadrantReconstruction(data, topLeftData, topRightData, bottomLeftData, bottomRightData, halfWidth, halfHeight, width, halfSplit + 1, b); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	public static void quadrantPartialReconstruction(HDRChannel dataQuad, HDRChannel dest, Quadrant q) {
		int halfWidth = dataQuad.getWidth();
		int halfHeight = dataQuad.getHeight();
		
		int width = dest.getWidth();

		quadrantPartialReconstruction(dataQuad.getData(), dest.getData(), q,
							   halfWidth, halfHeight, width, 0, (halfWidth * halfHeight) - 1);
	}
	
	private static void quadrantPartialReconstruction(final double[] dataQuad, final double[] dest, final Quadrant q,
			final int halfWidth, final int halfHeight, final int width, final int a, final int b){
    	// Base Case
		if(b - a < 250000){
			// For each pixel
			for(int y = 0; y < halfHeight; y++){
				for(int x = 0; x < halfWidth; x++){
					// Index mapping for the downSampleChannel <-> Original channel
					int halfIndex = (y * halfWidth) + x;
					int index = (2 * y * width) + (2 * x);
					
					// Sample the four corresponding pixels from data and map it to a single pixel
					if(q == Quadrant.TOPLEFT){ dest[index] = dataQuad[halfIndex]; }
					else if(q == Quadrant.TOPRIGHT){ dest[index + 1] = dataQuad[halfIndex]; }
					else if(q == Quadrant.BOTTOMLEFT){ dest[index + width] = dataQuad[halfIndex]; }
					else if(q == Quadrant.BOTTOMRIGHT){ dest[index + width + 1] = dataQuad[halfIndex]; }
				}
			}
			
			return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ quadrantPartialReconstruction(dataQuad, dest, q, halfWidth, halfHeight, width, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ quadrantPartialReconstruction(dataQuad, dest, q, halfWidth, halfHeight, width, halfSplit + 1, b); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
}

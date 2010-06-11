/*
	Gaussian Blur algorithm
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

public class GaussianBlur {

	public static HDRChannel gaussianBlur(HDRChannel channel){
		int width = channel.getWidth();
		int height = channel.getHeight();

		int size = channel.getSize();
		double data[] = channel.getData();
		
		int x, y;
		
		double blurXData[] = new double[size];
		
		// X-axis blur.
		for(y = 0; y < height; y++){
			for(x = 1; x < width - 1; x++){
				// Avg the adjacent pixels. Center pixel weighted 2x, neighbors weighted 1x
				int centerIndex = (y * width) + x;
				blurXData[centerIndex] = ((2 * data[centerIndex]) + data[centerIndex - 1] + data[centerIndex + 1]) / 4.0;
			}
			// Avg the left and right edge cases. Center pixel weighted 3x, neighbors weighted 1x
			int leftEdgeIndex = y * width;
			int rightEdgeIndex = (y * width) + width - 1;
			blurXData[leftEdgeIndex] = ((3 * data[leftEdgeIndex]) + data[leftEdgeIndex + 1]) / 4.0;
			blurXData[rightEdgeIndex] = (3 * data[rightEdgeIndex] + data[rightEdgeIndex - 1]) / 4.0;
		}

		// We return a Channel with the same name as the inputted channel but blurred
		HDRChannel blurredChannel = new HDRChannel(width, height, channel.getName());
		double blurredChannelData[] = blurredChannel.getData();
		
		// Y-axis blur.
		for(x = 0; x < width; x++){
			for(y = 1; y < height - 1; y++){
				
				// Avg the adjacent pixels. Center pixel weighted 2x, neighbors weighted 1x
				int centerIndex = (y * width) + x;
				blurredChannelData[centerIndex] = ((2 * blurXData[centerIndex]) + blurXData[centerIndex - 1] + blurXData[centerIndex + 1]) / 4.0;
			}
			// Avg the top and bottom edge cases. Center pixel weighted 3x, neighbors weighted 1x
			int topEdgeIndex = x;
			int bottomEdgeIndex = ((height - 1) * width) + x;
			blurredChannelData[topEdgeIndex] = (3 * blurXData[topEdgeIndex] + blurXData[topEdgeIndex + width]) / 4.0;
			blurredChannelData[bottomEdgeIndex] = (3 * blurXData[bottomEdgeIndex] + blurXData[bottomEdgeIndex - width]) / 4.0;
		}
		
		return blurredChannel;
	}
	
	/*private void gaussianBlur(final double[] rData, final double[] gData, final double[] bData, final int a, final int b){
		// Base Case
		if(b - a < 250000 || (b - a) / 2 < width){
    		for(int i = a; i <= b; i++){}
    		
    		// X-axis blur.
    		for(y = 0; y < height; y++){
    			for(x = 1; x < width - 1; x++){
    				
    				// Avg the adjacent pixels. Center pixel weighted 2x, neighbors weighted 1x
    				int centerIndex = (y * width) + x;
    				blurXData[centerIndex] = ((2 * data[centerIndex]) + data[centerIndex - 1] + data[centerIndex + 1]) / 4.0;
    			}
    			// Avg the left and right edge cases. Center pixel weighted 3x, neighbors weighted 1x
    			int leftEdgeIndex = y * width;
    			int rightEdgeIndex = (y * width) + width - 1;
    			blurXData[leftEdgeIndex] = ((3 * data[leftEdgeIndex]) + data[leftEdgeIndex + 1]) / 4.0;
    			blurXData[rightEdgeIndex] = (3 * data[rightEdgeIndex] + data[rightEdgeIndex - 1]) / 4.0;
    		}
    		
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ gaussianBlur(rData, gData, bData, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ gaussianBlur(rData, gData, bData, halfSplit + 1, b); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}*/
}

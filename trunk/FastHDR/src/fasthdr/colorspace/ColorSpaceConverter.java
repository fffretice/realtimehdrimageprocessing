/*
	ColourSpaceConverter
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
    Reference: pfstmo library
*/

package fasthdr.colorspace;

import fasthdr.exception.UnknownColorSpaceException;
import fasthdr.model.HDRChannel;
import fasthdr.model.HDRFrame;

//Converter for XYZ <-> RGB color spaces

// More information on Color space conversion: 
// http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
public class ColorSpaceConverter {
	
	// XYZ Color Spaces
	public static final String CIE_XYZ_2DEGREE = "CIE_XYZ_2Deg";
	
	// RGB Color Spaces
	public static final String SRGB = "sRGB";
	
	// Color Space Matrices
	public static final double[][] CIE_XYZtoSRGB_RGB = 	{{3.2404542, -1.5371385, -0.4985314},
											 			{-0.9692660, 1.8760108, 0.0415560},
											 			{0.0556434, -0.2040259, 1.0572252}};
	
	public static final double[][] SRGB_RGBtoCIE_XYZ = 	{{0.4124564, 0.3575761, 0.1804375},
											 			{0.2126729, 0.7151522, 0.0721750},
											 			{0.0193339, 0.1191920, 0.9503041}};
	
	final public static void convertXYZtoRGB(HDRFrame srcFrame, String srcColorSpace, String destColorSpace) throws UnknownColorSpaceException{
		// Get color space conversion matrix
		double[][] colorSpaceMatrix = getColorSpaceMatrix(srcColorSpace, destColorSpace);
		
		// Check that conversion matrix exists
		if(colorSpaceMatrix == null){
			throw new UnknownColorSpaceException("Cannot find source or destination color space type or conversion matrix does not exist.");
		}
		
		// Existing XYZ channels
		HDRChannel x = srcFrame.getChannels().get("X");
		HDRChannel y = srcFrame.getChannels().get("Y");
		HDRChannel z = srcFrame.getChannels().get("Z");
		double[] xData = x.getData();
		double[] yData = y.getData();
		double[] zData = z.getData();
		
		// For each pixel convert XYZ -> RGB
		new ColorSpaceConverter().colorSpaceMultiply(xData, yData, zData, colorSpaceMatrix, 0, xData.length - 1);
		
		// Re-use X, Y, Z channels as R, G, B channel, respectively
		srcFrame.renameChannel("X", "R");
		srcFrame.renameChannel("Y", "G");
		srcFrame.renameChannel("Z", "B");
	}
	
	final public static void convertRGBtoXYZ(HDRFrame srcFrame, String srcColorSpace, String destColorSpace) throws UnknownColorSpaceException{
		// Get color space conversion matrix
		double[][] colorSpaceMatrix = getColorSpaceMatrix(srcColorSpace, destColorSpace);
		
		// Check that conversion matrix exists
		if(colorSpaceMatrix == null){
			throw new UnknownColorSpaceException("Cannot find source or destination color space type or conversion matrix does not exist.");
		}
		
		// Existing XYZ channels
		HDRChannel r = srcFrame.getChannels().get("R");
		HDRChannel g = srcFrame.getChannels().get("G");
		HDRChannel b = srcFrame.getChannels().get("B");
		double[] rData = r.getData();
		double[] gData = g.getData();
		double[] bData = b.getData();
		
		// For each pixel convert XYZ -> RGB
		new ColorSpaceConverter().colorSpaceMultiply(rData, gData, bData, colorSpaceMatrix, 0, rData.length - 1);
		
		// Re-use X, Y, Z channels as R, G, B channel, respectively
		srcFrame.renameChannel("R", "X");
		srcFrame.renameChannel("G", "Y");
		srcFrame.renameChannel("B", "Z");
	}
	
	public void colorSpaceMultiply(final double[] aData, final double[] bData, final double[] cData, final double[][] matrix, final int a, final int b){
    	// Base Case
		if(b - a < 250000){
    		for(int i = a; i <= b; i++){
    			double aVal = aData[i];
				double bVal = bData[i];
				double cVal = cData[i];
				// Multiply by conversion matrix
				aData[i] = (matrix[0][0] * aVal) + (matrix[0][1] * bVal) + (matrix[0][2] * cVal);
				bData[i] = (matrix[1][0] * aVal) + (matrix[1][1] * bVal) + (matrix[1][2] * cVal);
				cData[i] = (matrix[2][0] * aVal) + (matrix[2][1] * bVal) + (matrix[2][2] * cVal);
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ colorSpaceMultiply(aData, bData, cData, matrix, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ colorSpaceMultiply(aData, bData, cData, matrix, halfSplit + 1, b); }};    	
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	final private static double[][] getColorSpaceMatrix(String srcColorSpace, String destColorSpace){
		// CIE XYZ -> SRGB
		if(srcColorSpace.equals(CIE_XYZ_2DEGREE) && destColorSpace.equals(SRGB)){
			return CIE_XYZtoSRGB_RGB;
		}
		
		// SRGB -> CIE XYZ
		else if(srcColorSpace.equals(SRGB) && destColorSpace.equals(CIE_XYZ_2DEGREE)){
			return SRGB_RGBtoCIE_XYZ;
		}
		
		return null;
	}
	
	final public static int pixelBinarySearch(double lum, double[] lumMap, int lumSize) {
		int rangeLow = 0; 
		int rangeMid = 0;
		int rangeUp = lumSize;
		while(true) {
			rangeMid = (rangeLow + rangeUp) / 2;
			if(rangeMid == rangeLow){ break; }
			if(lum < lumMap[rangeMid]){ rangeUp = rangeMid; }
			else{ rangeLow = rangeMid; }
		}
		return rangeLow;
	}
}

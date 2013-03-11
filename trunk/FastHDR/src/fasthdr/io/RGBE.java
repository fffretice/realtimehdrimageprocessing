/*
	HDR file format IO
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
    Reference: pfstmo library, rgbeio.cpp
*/

package fasthdr.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import fasthdr.exception.MalformedFileException;
import fasthdr.model.HDRFrame;

public class RGBE implements FileFormatInterface {

	public static final double WHITE_EFFICACY = 179.0;
	public static final String FORMAT_TYPE = "hdr";

	public static String getFormatType() {
		return FORMAT_TYPE;
	}
	
	private double[] readRadianceHeader(File infile) throws MalformedFileException{
		int format = 0;
		double exposure = 1.0f;
		int width = 0;
		int height = 0;
		
		try{
			FileInputStream fis = new FileInputStream(infile);
			
			while(true){
				String line = readLine(fis).trim();
				
				if(line.equals("")){
					break;
				}
			    if(line.equals("#?RADIANCE")){
			    	// format specifier found
			    	format = 1;
			    }
			    if(line.equals("#?RGBE")){
			    	// format specifier found
			    	format = 1;
			    }
			    if(line.equals("#?AUTOPANO")){
			    	// format specifier found
			    	format = 1;
			    }
			    if( line.startsWith("#") ){ // comment found - skip
			    	continue;
			    }
			    if(line.equals("FORMAT=32-bit_rle_rgbe")){
			    	// header found
			    	continue;
			    }
			    if(line.startsWith("EXPOSURE=")){
			    	String tokens[] = line.split("EXPOSURE=");
			    	if(tokens.length > 1){
			    		// exposure value
			    		exposure *= Double.parseDouble(tokens[1].trim());
			    	}
			    }
			}
			
			// ignore wierd exposure adjustments
			if(exposure > 1e12 || exposure < 1e-12){
				exposure = 1.0f;
			}
			    
			if(format != 1){
				throw new MalformedFileException("RGBE format specifier not found. This file may be corrupted or unsupported.");
			}
	
			// image size
			String line = readLine(fis).trim();
			String tokens[] = line.split("\\s");
			
			if(tokens.length != 4){
				throw new MalformedFileException("RGBE: unknown image size");
			}
			
			try{
				height = Integer.parseInt(tokens[1]);
				width = Integer.parseInt(tokens[3]);
			}
			catch(NumberFormatException e){
				e.printStackTrace();
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		return new double[]{width, height, exposure};
	}
	
	public void readRadiance(File infile, int width, int height, double exposure, HDRFrame inframe) throws MalformedFileException, IOException{
		double[] rData = inframe.getChannel("R").getData();
		double[] gData = inframe.getChannel("G").getData();
		double[] bData = inframe.getChannel("B").getData();
		
		// read image
		// depending on format read either rle or normal (note: only rle supported)
		byte[] scanline = new byte[width * 4];
		FileInputStream fis = new FileInputStream(infile);
		
		// Skip the header
		while(readLine(fis).trim().isEmpty() == false){}
		readLine(fis); // Eat an additional line
		
		for(int y = 0 ; y < height ; y++){
		    // read rle header
		    byte header[] = new byte[4];
		    fis.read(header, 0, 4);
		    if(javaSignedByteToInt(header[0]) != 2 || javaSignedByteToInt(header[1]) != 2 || (javaSignedByteToInt(header[2]) <<8 ) + javaSignedByteToInt(header[3]) != width) {
		    	//--- simple scanline (not rle)
		    	int byteCount = fis.read(scanline, 4, (4 * width) - 4);
		    	if(byteCount != (4 * width) - 4){
		    		throw new MalformedFileException("RGBE: not enough data to read in the simple format.");
		    	}
		    	
		    	//--- yes, we've read one pixel as a header
		    	scanline[0] = header[0];
		    	scanline[1] = header[1];
		    	scanline[2] = header[2];
		    	scanline[3] = header[3];
	
		    	//--- write scanline to the image
		    	for( int x=0 ; x<width ; x++ ) {
		    		double[] rgb = rgbe2rgb(scanline[4 * x + 0], scanline[4 * x + 1],
											scanline[4 * x + 2], scanline[4 * x + 3], 
											exposure);
		    		
		    		rData[y * width + x] = rgb[0];
		    		gData[y * width + x] = rgb[1];
		    		bData[y * width + x] = rgb[2];
		    	}
		    }
		    else {
		    	//--- rle scanline
	
		    	//--- each channel is encoded separately
		    	for(int ch = 0; ch < 4; ch++){
		    		int peek = 0;
		    		while(peek < width){
		    		    byte[] p = new byte[2];
		    		    fis.read(p, 0, 2);
		    		    if(javaSignedByteToInt(p[0]) > 128){
		    		    	// a run
		    		    	int run_len = javaSignedByteToInt(p[0]) - 128;

		    		    	while(run_len > 0){
		    		    		scanline[(width * ch) + peek++] = p[1];
		    		    		run_len--;
		    		    	}
		    		    }
		    		    else{
		    		    	// a non-run
		    		    	scanline[(width * ch) + peek++] = p[1];

		    		    	int nonrun_len = javaSignedByteToInt(p[0]) - 1;
		    		    	if(nonrun_len > 0){
		    		    		//if(nonrun_len + peek > width){
		    		    		//	nonrun_len = width - peek;
		    		    		//}
		    		    		fis.read(scanline, (width * ch) + peek, nonrun_len);
		    		    		peek += nonrun_len;
		    		    	}
		    		    }
		    		}
		    		if(peek != width){
		    			throw new MalformedFileException("RGBE: difference in size while reading RLE scanline");
		    		}
		    	}
		    	//--- write scanline to the image
		    	for(int x = 0 ; x < width ;x++){
		    		double[] rgb = rgbe2rgb(scanline[x + width * 0], scanline[x + width * 1],
			 								scanline[x + width * 2], scanline[x + width * 3], 
			 								exposure);
	
					rData[y * width + x] = rgb[0];
					gData[y * width + x] = rgb[1];
					bData[y * width + x] = rgb[2];
		    	}
		    }
		}
	}

	@Override
	public HDRFrame read(File infile) throws IOException,
			MalformedFileException, FileNotFoundException {
		
		double[] header = readRadianceHeader(infile);
		int width = (int)header[0];
		int height = (int)header[1];
		double exposure = header[2];
		
		HDRFrame returnFrame = new HDRFrame(width, height);
		returnFrame.addChannel("R");
		returnFrame.addChannel("G");
		returnFrame.addChannel("B");
		
		readRadiance(infile, width, height, exposure, returnFrame);
		
		
		
		return returnFrame;
	}

	@Override
	public void write(File outfile) {
		// TODO Future Work...
	}

	public static String readLine(FileInputStream fis) throws IOException {
		StringBuffer sb = new StringBuffer();
		byte b[] = new byte[1];
		while (fis.read(b) != -1 && b[0] != 0x0A) {
			sb.append((char) b[0]);
		}
		return sb.toString();
	}
	
	public double[] rgbe2rgb(byte r, byte g, byte b, byte e, double exposure){
		double[] rgb = new double[3];
		if(javaSignedByteToInt(e) != 0){		// a non-zero pixel
			int ee = javaSignedByteToInt(e) - 128 + 8;
	    	double f = Math.pow(2, ee) * WHITE_EFFICACY / exposure;

	    	rgb[0] = (double)(javaSignedByteToInt(r) * f);
	    	rgb[1] = (double)(javaSignedByteToInt(g) * f);
	    	rgb[2] = (double)(javaSignedByteToInt(b) * f);
		}
		else{
			rgb[0] = 0;
			rgb[1] = 0;
			rgb[2] = 0;
		}
		return rgb;
	}
	
	public static int javaSignedByteToInt(byte b){
		if((int)b < 0){
			return (int)b + 256; 
		}
		return b;
	}
}

/*
	PFS file format IO
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
    Reference: pfstmo library, 
*/

package fasthdr.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import fasthdr.exception.MalformedFileException;
import fasthdr.model.HDRChannel;
import fasthdr.model.HDRFrame;
import fasthdr.model.HDRTag;
import fasthdr.util.DataTypeConverter;

public class PFS implements FileFormatInterface {
	
	public static final String HEADER = "PFS1";
	public static final String END_HEADER = "ENDH";
	public static final int MIN_RESOLUTION = 3;
	public static final int MAX_RESOLUTION = 65535;
	public static final int MIN_CHANNEL_COUNT = 0;
	public static final int MAX_CHANNEL_COUNT = 1024;
	public static final int MIN_TAG_COUNT = 0;
	public static final int MAX_TAG_COUNT = 1024;
	public static final int MIN_CHANNEL_NAME_LENGTH = 1;
	public static final int MAX_CHANNEL_NAME_LENGTH = 32;
	public static final int NEW_LINE = 0x0a;
	
	public static final String FORMAT_TYPE = "pfs";

	public static String getFormatType() {
		return FORMAT_TYPE;
	}
	
	public HDRFrame read(File infile) throws IOException, MalformedFileException, FileNotFoundException {
		
		// Init.
		FileInputStream fileInputStream = null;
		//InputStreamReader inputStreamReader = null;
		String line = null;
		StringTokenizer strTok = null;
		
		try{
			// Check that infile is not null.
			if(infile == null){
				throw new FileNotFoundException("File object is null.");
			}
			
			// Setup file reader.
			fileInputStream = new FileInputStream(infile);
			//inputStreamReader = new InputStreamReader(fileInputStream, Charset.forName("UTF-8"));
			
			// Get header.
			line = readLine(fileInputStream);
			if( !line.equals(HEADER) ){
				throw new MalformedFileException("File is missing header.");
			}
			
			// Get pixel width and height.
			int width, height;
			strTok = new StringTokenizer(readLine(fileInputStream), " ");
			if(strTok.countTokens() == 2){
				width = Integer.parseInt(strTok.nextToken());
				height = Integer.parseInt(strTok.nextToken());
				if(width < MIN_RESOLUTION || width > MAX_RESOLUTION || height < MIN_RESOLUTION || height > MAX_RESOLUTION){
					throw new MalformedFileException("Invalid width and/or height value(s).");
				}
			}
			else{
				throw new MalformedFileException("Missing width and/or height value(s).");
			}
			
			// Get channel count.
			strTok = new StringTokenizer(readLine(fileInputStream), " ");
			int channelCount = Integer.parseInt(strTok.nextToken());
			if(channelCount < MIN_CHANNEL_COUNT || channelCount > MAX_CHANNEL_COUNT){
				throw new MalformedFileException("Invalid channel count.");
			}
			
			// Create frame
			HDRFrame frame = new HDRFrame(width, height);
			
			// Get frame tags
			frame.setTags(readTags(fileInputStream));
			
			// Get channels names and tags.
			ArrayList<String> channelNames = new ArrayList<String>();
			for(int i=0; i<channelCount; i++){
				line = readLine(fileInputStream);
				if(line.length() < MIN_CHANNEL_NAME_LENGTH || line.length() > MAX_CHANNEL_NAME_LENGTH){
					throw new MalformedFileException("Invalid channel name length.");
				}
				else{
					// Create channel
					HDRChannel channel = frame.addChannel(line);
					channelNames.add(line);
					
					// Get channel tags
					channel.setTags(readTags(fileInputStream));
				}
			}
			
			// End of header.
			byte headerEnd[] = new byte[4];
			fileInputStream.read(headerEnd, 0, 4);
			if(!new String(headerEnd, Charset.forName("UTF-8")).equals(END_HEADER)){
				throw new MalformedFileException("Invalid end of header.");
			}
			
			// Get channel data
			for(String channelName: channelNames){
				
				// Channel data
				HDRChannel channel = frame.getChannels().get(channelName);
				double data[] = channel.getData();
				
				// Read channel data row by row
				for(int j = 0; j < channel.getHeight(); j++){
					byte buf[] = new byte[channel.getWidth() * 4];
					fileInputStream.read(buf, 0, buf.length);
					
					// For each 4 elements in buf, convert it to a single float element
					for(int k = 0; k < channel.getWidth(); k++){
						byte bufPart[] = new byte[4];
						bufPart[0] = (byte)buf[(k * 4)];
						bufPart[1] = (byte)buf[(k * 4) + 1];
						bufPart[2] = (byte)buf[(k * 4) + 2];
						bufPart[3] = (byte)buf[(k * 4) + 3];
						// Cast to double for increased arithmetic precision
						data[j * channel.getWidth() + k] = (double)DataTypeConverter.cFloatToJavaFloat(bufPart);
					}
				}
			}
			
			fileInputStream.close();
			fileInputStream = null;
			
			return frame;
		}
		finally{
			if(fileInputStream != null){
				fileInputStream.close();
			}
		}
	}
	
	public static String readLine(InputStreamReader inputStreamReader) throws IOException{
		StringBuffer sb = new StringBuffer();
		char c;
		while(inputStreamReader.ready()){
			c = (char)inputStreamReader.read();
			if(c == NEW_LINE){
				break;
			}
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	public static String readLine(FileInputStream fis) throws IOException{
		StringBuffer sb = new StringBuffer();
		byte b[] = new byte[1];
		while(fis.read(b) != -1 && b[0] != 0x0A){
			sb.append((char)b[0]);
		}
		return sb.toString();
	}
	
	public static Hashtable<String, HDRTag> readTags(FileInputStream fis) throws IOException, NumberFormatException{
		Hashtable<String, HDRTag> tags = new Hashtable<String, HDRTag>();
		
		// Get tag count.
		int tagCount;
		String line = readLine(fis);
		tagCount = Integer.parseInt(line);
		if(tagCount < MIN_TAG_COUNT || tagCount > MAX_TAG_COUNT){
			System.out.println("Non-valid tag count value.");
			return null;
		}
		
		// Get tags.
		for(int i=0; i<tagCount; i++){
			line = readLine(fis);
			StringTokenizer strTok = new StringTokenizer(line, "=");
			if(strTok.countTokens() == 2){
				String name = strTok.nextToken();
				String value = strTok.nextToken();
				tags.put(name, new HDRTag(name, value));
			}
			else{
				System.out.println("Non-valid tag key and/or value.");
			}
		}
		
		return tags;
	}

	public void write(File outfile) {
		// TODO Future work...
	}
}

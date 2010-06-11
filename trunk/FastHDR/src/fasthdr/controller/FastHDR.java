/*
	FastHDR
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

package fasthdr.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.SwingUtilities;

import fasthdr.colorspace.ColorSpaceConverter;
import fasthdr.exception.MalformedFileException;
import fasthdr.exception.UnknownColorSpaceException;
import fasthdr.io.FileOpenTypeFilter;
import fasthdr.io.PFS;
import fasthdr.io.RGBE;
import fasthdr.model.HDRFrame;
import fasthdr.view.FastHDRFrame;
import fasthdr.view.ImagePanel;

public class FastHDR {
	
	private static File imageFile = null;
	private static HDRFrame imageFrame = null; // Reference can be set but the data within this frame should never be changed
	
	private static ImagePanel imagePanel;
	
	public static void setFile(File file) throws FileNotFoundException, IOException, MalformedFileException{
		imageFile = file;
		
		String fileExtension = FileOpenTypeFilter.getExtension(file);
		if(fileExtension != null){
			if(fileExtension.equals(PFS.getFormatType())){
				imageFrame = new PFS().read(imageFile);
			}
			else if(fileExtension.equals(RGBE.getFormatType())){
				imageFrame = new RGBE().read(imageFile);
				try {
					ColorSpaceConverter.convertRGBtoXYZ(imageFrame, ColorSpaceConverter.SRGB, ColorSpaceConverter.CIE_XYZ_2DEGREE);
				} catch (UnknownColorSpaceException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static File getFile(){
		return imageFile;
	}
	
	public static HDRFrame getFrame(){
		return imageFrame;
	}
	
	public static void setImagePanel(ImagePanel panel) {
		imagePanel = panel;
	}
	
	public static ImagePanel getImagePanel(){
		return imagePanel;
	}
	
	public static void main(String[] args) {
		final FastHDRFrame frame = new FastHDRFrame();
		
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	frame.setVisible(true);
            }
        });
	}
}

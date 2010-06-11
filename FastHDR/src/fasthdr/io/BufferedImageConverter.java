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
    Reference: pfstmo library, 
    http://www.java2s.com/Code/Java/2D-Graphics-GUI/ConvertjavaawtimageBufferedImagetojavaawtImage.htm
*/

package fasthdr.io;/*

Java Media APIs: Cross-Platform Imaging, Media and Visualization
Alejandro Terrazas
Sams, Published November 2002, 
ISBN 0672320940
*/

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;

public class BufferedImageConverter {
	static public BufferedImage createBufferedImage(Image imageIn, Component comp) {
		return createBufferedImage(imageIn, BufferedImage.TYPE_INT_ARGB, comp);
	}

	static public BufferedImage createBufferedImage(Image imageIn, int imageType, Component comp) {
		MediaTracker mt = new MediaTracker(comp);
		mt.addImage(imageIn, 0);
		try {
			mt.waitForID(0);
		} 
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		BufferedImage bufferedImageOut = new BufferedImage(imageIn.getWidth(null), imageIn.getHeight(null), imageType);
		Graphics g = bufferedImageOut.getGraphics();
		g.drawImage(imageIn, 0, 0, null);

		return bufferedImageOut;
	}
}

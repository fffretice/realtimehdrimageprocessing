/*
	HDRChannel model
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

package fasthdr.model;

import java.util.Hashtable;

public class HDRChannel {
	private int width;
	private int height;
	private int size;
	private String name;
	private double[] data;
	
	private Hashtable<String, HDRTag> tags;
	
	// Named Channel
	public HDRChannel(int width, int height, String name){
		this.width = width;
		this.height = height;
		this.size = width * height;
		this.name = name;
		data = new double[width * height];
		tags = new Hashtable<String, HDRTag>();
	}
	
	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	public int getSize(){
		return size;
	}

	public void setSize(int size){
		this.size = size;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Hashtable<String, HDRTag> getTags() {
		return tags;
	}
	
	public void setTags(Hashtable<String, HDRTag> tagSet){
		tags = tagSet;
	}
	
	public void setAllData(double value){
		for(int i = 0; i < size; i++){
			data[i] = value;
		}
	}
	
	public void copyChannelData(HDRChannel channelToCopy){
		width = channelToCopy.getWidth();
		height = channelToCopy.getHeight();
		size = channelToCopy.getSize();
		
		double[] srcData = channelToCopy.getData();
		
		// If data is already of the same size then do not re-allocate memory
		if(data.length != srcData.length){
			data  = new double[size];
		}
		
		copyData(srcData, data, 0, size - 1);
	}
	
	public void copyChannel(HDRChannel channelToCopy){
		
		width = channelToCopy.getWidth();
		height = channelToCopy.getHeight();
		size = channelToCopy.getSize();
		name = channelToCopy.getName();		
		tags = new Hashtable<String, HDRTag>();
		data  = new double[size];
		
		for(String tagName: channelToCopy.getTags().keySet()){
			HDRTag tag = channelToCopy.getTags().get(tagName);
			tags.put(new String(tag.getName()), new HDRTag(new String(tag.getName()), new String(tag.getValue())));
		}

		double[] srcData = channelToCopy.getData();
		
		copyData(srcData, data, 0, size - 1);
	}
	
	public double[] getData() {
		return data;
	}
	
	private static int COPY_DATA_BASE_CASE = 250000;
	private void copyData(final double[] src, final double[] des, final int a, final int b){
		// Base Case
		if(b - a < COPY_DATA_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			des[i] = src[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ copyData(src, des, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ copyData(src, des, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
}

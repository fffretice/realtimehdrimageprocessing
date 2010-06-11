/*
	HDR frame model
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

public class HDRFrame {
	private int width;
	private int height;
	private int size;
	
	private Hashtable<String, HDRTag> tags;
	private Hashtable<String, HDRChannel> channels;
	
	public HDRFrame(int width, int height){
		this.width = width;
		this.height = height;
		this.size = width * height;
		tags = new Hashtable<String, HDRTag>();
		channels = new Hashtable<String, HDRChannel>();
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

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	public Hashtable<String, HDRTag> getTags(){
		return tags;
	}
	
	public HDRTag getTag(String name){
		return tags.get(name);
	}
	
	public void setTags(Hashtable<String, HDRTag> tagSet){
		tags = tagSet;
	}
	
	public Hashtable<String, HDRChannel> getChannels(){
		return channels;
	}
	
	public HDRChannel getChannel(String name){
		return channels.get(name);
	}
	
	public HDRChannel addChannel(String name){
		HDRChannel channel = new HDRChannel(width, height, name);
		channels.put(name, channel);
		return channel;
	}
	
	public HDRChannel addChannel(HDRChannel channel){
		channels.put(channel.getName(), channel);
		return channel;
	}
	
	public void removeChannel(String name){
		channels.remove(name);
	}

	public void renameChannel(String src, String des){
		HDRChannel channel = channels.remove(src);
		channel.setName(des);
		channels.put(des, channel);
	}
	
	// Copy and Add aChannel, bChannel and cChannel to this HDRFrame
	public void copyAddChannelData(HDRChannel aChannel, HDRChannel bChannel, HDRChannel cChannel) {
		// All Channels must have the same size!
		HDRChannel newAChannel = new HDRChannel(aChannel.getWidth(), aChannel.getHeight(), aChannel.getName());
		HDRChannel newBChannel = new HDRChannel(bChannel.getWidth(), bChannel.getHeight(), bChannel.getName());
		HDRChannel newCChannel = new HDRChannel(cChannel.getWidth(), cChannel.getHeight(), cChannel.getName());
		
		copyMultipleChannels(aChannel.getData(), bChannel.getData(), cChannel.getData(),
							 newAChannel.getData(), newBChannel.getData(), newCChannel.getData(), 0, aChannel.getSize() - 1);
		
		addChannel(newAChannel);
		addChannel(newBChannel);
		addChannel(newCChannel);
	}
	
	// Copy aChannel, bChannel and cChannel to an existing channel with name aDes, bDes, cDes
	public void copyChannelData(HDRChannel aChannel, HDRChannel bChannel, HDRChannel cChannel, String aDes, String bDes, String cDes) {
		// aChannel, bChannel, cChannel MUST have size <= to aDes, bDes and cDes
		HDRChannel aDestChannel = channels.get(aDes);
		HDRChannel bDestChannel = channels.get(bDes);
		HDRChannel cDestChannel = channels.get(cDes);
		
		copyMultipleChannels(aChannel.getData(), bChannel.getData(), cChannel.getData(),
							 aDestChannel.getData(), bDestChannel.getData(), cDestChannel.getData(), 0, aChannel.getSize() - 1);
	}
	
	public void copyChannel(final double[] src, final double[] des, final int a, final int b){
		// Base Case
		if(b - a < 2000000){
    		for(int i = a; i <= b; i++){ des[i] = src[i]; }
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ copyChannel(src, des, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ copyChannel(src, des, halfSplit + 1, b); }};
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	public void copyMultipleChannels(final double[] aSrcData, final double[] bSrcData, final double[] cSrcData, 
			 final double[] aDesData, final double[] bDesData, final double[] cDesData, final int a, final int b){
		// Base Case
		if(b - a < 300000){
			for(int i = a; i <= b; i++){
				aDesData[i] = aSrcData[i];
				bDesData[i] = bSrcData[i];
				cDesData[i] = cSrcData[i];
			}
		return;
		}
		// Recurse
		final int halfSplit = (b - a) / 2 + a;
		Thread t1 = new Thread(){ public void run(){ copyMultipleChannels(aSrcData, bSrcData, cSrcData, aDesData, bDesData, cDesData, a, halfSplit); }};
		Thread t2 = new Thread(){ public void run(){ copyMultipleChannels(aSrcData, bSrcData, cSrcData, aDesData, bDesData, cDesData, halfSplit + 1, b); }};    	
		t1.start(); t2.start();
		try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
		try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}

	
}

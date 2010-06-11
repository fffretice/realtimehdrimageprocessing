/*
	c Float to Java Float converter
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

import java.nio.ByteBuffer;

public class DataTypeConverter {
	
	// IEEE Standard 754 Floating Point Number
	public static float cFloatToJavaFloat(byte[] cBytes){
		byte jBytes[] = new byte[4];
		
		jBytes[3] = cBytes[0];
		jBytes[2] = cBytes[1];
		jBytes[1] = cBytes[2];
		jBytes[0] = cBytes[3];
		
		return ByteBuffer.wrap(jBytes).getFloat();
	}
}

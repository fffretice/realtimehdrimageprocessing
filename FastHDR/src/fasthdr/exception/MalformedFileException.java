/*
	MalformedFileException
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

package fasthdr.exception;

public class MalformedFileException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String errorMessage;
	
	public MalformedFileException(String msg) {
		errorMessage = msg;
	}
	
	public String getErrorMessage(){
		return errorMessage;
	}
	
	public String toString(){
		return errorMessage;
	}
}

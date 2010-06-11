/*
	FileFormatInterface
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

package fasthdr.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import fasthdr.exception.MalformedFileException;
import fasthdr.model.HDRFrame;

public interface FileFormatInterface  {
	public HDRFrame read(File infile) throws IOException, MalformedFileException, FileNotFoundException;
	public void write(File outfile);
}

/*
	Logarithm toolbar widget
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

package fasthdr.view;

import javax.swing.JToolBar;

import fasthdr.controller.FastHDR;
import fasthdr.tmo.Log;

public class ToolBarLog implements ToolBarInterface {

	private Log tmo;
	
	public ToolBarLog(){
		if(FastHDR.getFrame() != null){
			tmo = new Log(FastHDR.getFrame(), FastHDR.getImagePanel());
		}
	}
	
	@Override
	public void addWidgets(JToolBar toolBar) {
		// No widgets to add
	}

	@Override
	public void removeWidgets(JToolBar toolBar) {
		// No widgets to remove
	}

	@Override
	public void performTMO() {
		if(tmo != null){ tmo.performTMO(); }
	}

	@Override
	public void cancelTMO() {
		if(tmo != null){ tmo.cancelTMO(); }
	}
}

/*
	Multigrid Partial differential equation solver.
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
    Reference: pfstmo library, pde.cpp
*/

package fasthdr.pde;

import fasthdr.model.HDRChannel;
import fasthdr.util.CancelSignal;

// Multigrid partial differential equation framework for solving in O(n) time, where n is the number of pixels
// Open Source: pde.cpp of pfstmo library
// Steps to MG can be found: http://www.imtek.uni-freiburg.de/simulation/mathematica/IMSweb/imsTOC/Lectures%20and%20Tips/Simulation%20I/HTMLLinks/MultiGrid_introDocu_26.html
public class Multigrid {

	public static final int MIN_SIZE = 16;
	public static final int MODYF = 0;
	public static final int SMOOTHING_ITERATIONS = 1;
	public static final int BICONJUGATE_GRADIENT_STEPS = 20;
	public static final int V_CYCLES = 2;
	
	private CancelSignal cancelSignal;
	
	public Multigrid(CancelSignal signal){
		cancelSignal = signal;
	}
	
	public HDRChannel solve(HDRChannel channel){
		int width = channel.getWidth();
		int height = channel.getHeight();
		
		int i;	// index for simple loops
		int k;	// index for iterating through levels
		int k2;	// index for iterating through levels in V-cycles
		int cycle;
		
		// 1. restrict f to coarse-grid (by the way count the number of levels)
		// k=0: fine-grid = f
		// k=levels: coarsest-grid
		int levels = 0;
		int minWidthHeight = width < height? width : height;
		while(minWidthHeight >= MIN_SIZE){
			levels++;
			minWidthHeight = (minWidthHeight / 2) + MODYF;
		}
		
		// Given function f restricted on levels
		HDRChannel[] RHS = new HDRChannel[levels + 1];
		
		// Approximate initial solutions on levels
		HDRChannel[] IU = new HDRChannel[levels + 1];
		
		// Target functions in cycles (approximate solution error)
		HDRChannel[] VF = new HDRChannel[levels + 1];
		
		VF[0] = new HDRChannel(width, height, "VF");
		
		RHS[0] = new HDRChannel(1, 1, "RHS");
		RHS[0].copyChannelData(channel);
		
		IU[0] = new HDRChannel(width, height, "IU");
		
		int sX = width;
		int sY = height;
		
		for(k = 0; k < levels; k++){
			if(cancelSignal.isCancelled()){ return null; }
			sX = (sX / 2) + MODYF;
			sY = (sY / 2) + MODYF;
			
			RHS[k + 1] = new HDRChannel(sX, sY, "RHS");
			IU[k + 1] = new HDRChannel(sX, sY, "IU");
			VF[k + 1] = new HDRChannel(sX, sY, "VF");
			
			restrict(RHS[k], RHS[k+1]);
		}
		
		// 2. find exact solution at the coarsest-grid (k=levels)
		IU[levels].setAllData(0);
		
		// 3. nested iterations
		for(k = levels - 1; k >= 0; k--){
			if(cancelSignal.isCancelled()){ return null; }
			// 4. interpolate solution from last coarse-grid to finer-grid
			// interpolate from level k+1 to level k (finer-grid)
			prolongate(IU[k+1], IU[k]);
		
			// 4.1. first target function is the equation target function (following target functions are the defect)
			VF[k].copyChannelData(RHS[k]);
			
			// 5. V-cycle (twice repeated)
			for(cycle = 0; cycle < V_CYCLES; cycle++){
				
				// 6. downward stroke of V
				for(k2 = k; k2 < levels; k2++ ){
					if(cancelSignal.isCancelled()){ return null; }
					// 7. pre-smoothing of initial solution using target function zero for initial guess at smoothing (except for level k when iu contains prolongated result)
					if(k2 != k){
						IU[k2].setAllData(0);
					}
				
					for(i=0 ; i < SMOOTHING_ITERATIONS; i++ ){
						if(cancelSignal.isCancelled()){ return null; }
						smooth(IU[k2], VF[k2]);
					}
				
					// 8. calculate defect at level
					//    d[k2] = Lh * ~u[k2] - f[k2]
					HDRChannel D = new HDRChannel(IU[k2].getWidth(), IU[k2].getHeight(), "defects");
					calculate_defect(D, IU[k2], VF[k2]);
				
					// 9. restrict defect as target function for next coarser-grid
					//    def -> f[k2+1]
					restrict(D, VF[k2 + 1]);
				}
				
				// 10. solve on coarsest-grid (target function is the defect) iu[levels] should contain solution for the f[levels] - last defect, iu will now be the correction
				IU[levels].setAllData(0);
				
				// 11. upward stroke of V
				for(k2 = levels - 1; k2 >= k; k2--){
					if(cancelSignal.isCancelled()){ return null; }
					// 12. interpolate correction from last coarser-grid to finer-grid
					//     iu[k2+1] -> cor
					HDRChannel C = new HDRChannel(IU[k2].getWidth(), IU[k2].getHeight(), "c");
					prolongate(IU[k2 + 1], C);
				
					// 13. add interpolated correction to initial solution at level k2
					add_correction(IU[k2], C);
				
					// 14. post-smoothing of current solution using target function
					for(i=0; i < SMOOTHING_ITERATIONS; i++){
						smooth(IU[k2], VF[k2]);
					}
				}
			} //--- end of V-cycle
		}
		
		return IU[0];
	}

	private void add_correction(HDRChannel channel, HDRChannel correction) {
		double[] channelData = channel.getData();
		double[] correctionData = correction.getData();
		for(int i = 0; i < correction.getSize(); i++){
			if(cancelSignal.isCancelled()){ return; }
			channelData[i] = channelData[i] + correctionData[i];
		}
	}

	private void calculate_defect(HDRChannel d, HDRChannel u, HDRChannel f) {
		int width = f.getWidth();
		int height = f.getHeight();
		
		double[] dData = d.getData();
		double[] uData = u.getData();
		double[] fData = f.getData();
		int w, n, e, s;
		for(int y = 0 ; y < height ; y++)
			for(int x = 0 ; x < width ; x++) {
				if(cancelSignal.isCancelled()){ return; }
				w = (x == 0 ? 0 : x - 1);
				n = (y == 0 ? 0 : y - 1);
				s = (y + 1 == height ? y : y + 1);
				e = (x + 1 == width ? x : x + 1);
				dData[(y * width) + x] = fData[(y * width) + x] 
				                         - (uData[(y * width) + e] 
				                         + uData[(y * width) + w] 
				                         + uData[(n * width) + x] 
				                         + uData[(s * width) + x] 
				                         - 4.0 * uData[(y * width) + x]);
	    }
	}

	private void smooth(HDRChannel u, HDRChannel f) {
		new BiconjugateGradientSolver(cancelSignal).linearBiconjugteGradient(u.getSize(), f.getData(), u.getData(), 1, 0.001, BICONJUGATE_GRADIENT_STEPS, u.getHeight(), u.getWidth());
	}
	
	public double fractionPart(double d){
		// Return the fractional part of a given double. e.g. fractionPart(10.55) -> 0.55
		return d - Math.floor(d);
	}
	
	private void prolongate(HDRChannel in, HDRChannel out) {
		double dx = (double)in.getWidth() / (double)out.getWidth();
		double dy = (double)in.getHeight() / (double)out.getHeight();

		int outRows = out.getHeight();
		int outCols = out.getWidth();

		double inRows = in.getHeight();
		double inCols = in.getWidth();

		double filterSize = 1;

		double sx, sy;
		int x, y;
		
		double[] inData = in.getData();
		double[] outData = out.getData();
		
		double pixVal, weight, fx, fy, fval, ix, iy;
		
		for(y = 0, sy = -dy / 2; y < outRows; y++, sy += dy){
			for(x = 0, sx = -dx / 2; x < outCols; x++, sx += dx){
				pixVal = 0;
				weight = 0;
				
				for(ix = Math.max(0, Math.ceil(sx - filterSize)); ix <= Math.min(Math.floor(sx + filterSize), inCols - 1); ix++){
					for(iy = Math.max(0, Math.ceil(sy - filterSize)); iy <= Math.min(Math.floor(sy + filterSize), inRows - 1); iy++) {
						if(cancelSignal.isCancelled()){ return; }
						fx = Math.abs(sx - ix);
						fy = Math.abs(sy - iy);

						fval = (1 - fx) * (1 - fy);
						pixVal = pixVal + (inData[(int)(iy * inCols) + (int)ix] * fval);
						weight = weight + fval;
					}
				}
				
				outData[(y * outCols) + x] = pixVal / weight;
		    }
		}
	}

	private void restrict(HDRChannel in, HDRChannel out) {
		int inHeight = in.getHeight();
		int inWidth = in.getWidth();
		
		int outHeight = out.getHeight();
		int outWidth = out.getWidth();
	
	  	double ratioWidth = (double)inWidth / (double)outWidth;
	  	double ratioHeight = (double)inHeight / (double)outHeight;
	
	  	double filterSize = 0.5;
	  	
	  	double sy, sx;
	  	int x, y;
	  	
	  	double[] inData = in.getData();
		double[] outData = out.getData();
	  	
	  	for(y = 0, sy = ratioHeight / 2 - 0.5; y < outHeight; y++, sy += ratioHeight){
	  		for(x = 0, sx = ratioWidth/ 2 - 0.5; x < outWidth; x++, sx += ratioWidth) {

	  			double pixVal = 0;
	  			double w = 0;
	  			
	  			for(double ix = Math.max(0, Math.ceil(sx - ratioWidth * filterSize)); ix <= Math.min(Math.floor(sx + ratioWidth * filterSize), inWidth - 1); ix++){
	  				for(double iy = Math.max(0, Math.ceil(sy - ratioWidth * filterSize)); iy <= Math.min(Math.floor(sy + ratioWidth * filterSize), inHeight - 1); iy++){
	  					if(cancelSignal.isCancelled()){ return; }
	  					pixVal = pixVal + inData[(int)(iy * inWidth) + (int)ix];
	  					w++;
	  				}
	  			}
	  			
	  			outData[(y * outWidth) + x] = pixVal / w;
	  		}
		}
	}
}

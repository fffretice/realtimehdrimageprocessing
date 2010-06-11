/*
	Partial differential equation solver.
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

import fasthdr.util.CancelSignal;

public class BiconjugateGradientSolver {
	
	public static final double EPS = 1.0e-12;
	
	private CancelSignal cancelSignal;
	
	public BiconjugateGradientSolver(CancelSignal signal){
		cancelSignal = signal;
	}
	
	public void linearBiconjugteGradient(int n, double[] b, double[] x, int itol, double tol, int itmax, int rows, int cols){	
		
		double[] newB = new double[n + 1];
		double[] newX = new double[n + 1];
		
		// Pad arrays b and x with a single element by copying and offsetting the array
		//padArray(b, newB, 1, b.length);
		//padArray(x, newX, 1, x.length);
		for(int j = 1; j <= n; j++){
			if(cancelSignal.isCancelled()){ return; }
			newB[j] = b[j - 1];
			newX[j] = x[j - 1];
		}
		
		double[] bknum = new double[1];
		bknum[0] = 0;
		double[] akden = new double[1];
		akden[0] = 0;
		double ak, bk, bkden = 0, bnrm = 0, dxnrm, xnrm, zm1nrm, znrm;

		double[] p = new double[n + 1];
		double[] pp = new double[n + 1];
		double[] r = new double[n + 1];
		double[] rr = new double[n + 1];
		double[] z = new double[n + 1];
		double[] zz = new double[n + 1];

		int iter = 0;
		double err = 0;
		
		atimes(n, newX, r, 0, rows, cols);
		
		//setR_RR(newB, r, rr, 0, n - 1);
		for(int j = 1; j <= n; j++) {
			if(cancelSignal.isCancelled()){ return; }
			r[j] = newB[j] - r[j];
			rr[j] = r[j];
		}
		
		// Minimum residual
		atimes(n, r, rr, 0, rows, cols);
		
	    znrm = 1.0;
		if (itol == 1){ 
			bnrm = snrm(n, newB, itol); 
		}
		else if(itol == 2) {
			asolve(n, newB, z, 0, rows, cols);
			bnrm = snrm(n, z, itol);
		}
		else if(itol == 3 || itol == 4) {
			asolve(n, newB, z, 0, rows, cols);
			bnrm = snrm(n, z, itol);
			asolve(n, r, z, 0, rows, cols);
			znrm = snrm(n, z, itol);
		} 
		else { 
			System.out.println("Error: Illegal itol in linbcg");
		}
		
		asolve(n, r, z, 0, rows, cols);        

		while(iter <= itmax) {
			if(cancelSignal.isCancelled()){ return; }
			iter++;
			zm1nrm = znrm;
			asolve(n, rr, zz, 1, rows, cols);
			
			bknum[0] = 0;
			//setBKNUM(z, rr, bknum, 1, n - 1);
			for(int j = 1; j <= n; j++){
				if(cancelSignal.isCancelled()){ return; }
				bknum[0] += z[j] * rr[j];
			}
			
			if(iter == 1) {
				//setP_PP(z, zz, p, pp, 1, n - 1);
				for(int j = 1; j <= n; j++) {
					if(cancelSignal.isCancelled()){ return; }
					p[j] = z[j];
					pp[j] = zz[j];
				}
			}
			else {
				bk = bknum[0] / bkden;
				//setP_PP_2(bk, z, zz, p, pp, 1, n - 1);
				for(int j = 1; j <= n; j++) {
					if(cancelSignal.isCancelled()){ return; }
					p[j] = bk * p[j] + z[j];
					pp[j] = bk * pp[j] + zz[j];
				}
			}                
			
			bkden = bknum[0];
			atimes(n, p, z, 0, rows, cols);
			
			akden[0] = 0;
			//setAKDEN(z, pp, akden, 1, n - 1);
			for(int j = 1; j <= n; j++){ 
				if(cancelSignal.isCancelled()){ return; }
				akden[0] += z[j] * pp[j];
			}
			
			ak = bknum[0] / akden[0];
			atimes(n, pp, zz, 1, rows, cols);
			
			//setNewX_R_RR(ak, p, z, zz, newX, r, rr, 1, n - 1);
			for(int j = 1; j <= n; j++) {
				if(cancelSignal.isCancelled()){ return; }
				newX[j] += ak * p[j];
				r[j] -= ak * z[j];
				rr[j] -= ak * zz[j];
			}
			
			asolve(n, r, z, 0, rows, cols);
			
			if(itol == 1 || itol == 2) {
				znrm = 1.0;
				err = snrm(n, r, itol) / bnrm;
			} 
			else if(itol == 3 || itol == 4) {
				znrm = snrm(n, z, itol);
				if(Math.abs(zm1nrm - znrm) > EPS * znrm) {
					dxnrm = Math.abs(ak) * snrm(n, p, itol);
					err = znrm / Math.abs(zm1nrm - znrm) * dxnrm;
				} 
				else {
					err = znrm / bnrm;
					continue;
				}
				
				xnrm = snrm(n, newX, itol);
				if(err <= 0.5 * xnrm){ 
					err = err / xnrm; 
				}
				else {
					err = znrm / bnrm;
					continue;
				}
			}
			if(err <= tol){ 
				break;
			}
		}
		
		// Copy results back to x, removing the single element offset
		//unpadArray(newX, x, 1, n - 1);
		for(int j = 1; j <= n; j++){
			if(cancelSignal.isCancelled()){ return; }
			x[j - 1] = newX[j];
		}
	}
	
	// Needs fixing
	private static int PAD_ARRAY_BASE_CASE = 250000;
	private void padArray(final double[] src, final double[] des, final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < PAD_ARRAY_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			des[i] = src[i - 1];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ padArray(src, des, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ padArray(src, des, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	// Needs fixing
	private static int UNPAD_ARRAY_BASE_CASE = 250000;
	private void unpadArray(final double[] src, final double[] des, final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < UNPAD_ARRAY_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			des[i - 1] = src[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ unpadArray(src, des, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ unpadArray(src, des, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	// Needs fixing
	private static int SET_R_RR_BASE_CASE = 250000;
	private void setR_RR(final double[] newB, final double[] r, final double[] rr, final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < SET_R_RR_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			r[i] = newB[i] - r[i];
    			rr[i] = r[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ setR_RR(newB, r, rr, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ setR_RR(newB, r, rr, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	// Needs fixing
	private static int SET_BKNUM_BASE_CASE = 250000;
	private void setBKNUM(final double[] z, final double[] rr, final double[] bknum, final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < SET_BKNUM_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			bknum[0] += z[i] * rr[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ setBKNUM(z, rr, bknum, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ setBKNUM(z, rr, bknum, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}

	// Needs fixing
	private static int SET_P_PP_BASE_CASE = 250000;
	private void setP_PP(final double[] z, final double[] zz, 
			final double[] p, final double[] pp, 
			final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < SET_P_PP_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			p[i] = z[i];
				pp[i] = zz[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ setP_PP(z, zz, p, pp, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ setP_PP(z, zz, p, pp, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	// Needs fixing
	private static int SET_P_PP_2_BASE_CASE = 250000;
	private void setP_PP_2(final double bk, final double[] z, final double[] zz, 
			final double[] p, final double[] pp, 
			final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < SET_P_PP_2_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			p[i] = bk * p[i] + z[i];
				pp[i] = bk * pp[i] + zz[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ setP_PP_2(bk, z, zz, p, pp, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ setP_PP_2(bk, z, zz, p, pp, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	// Needs fixing
	private static int SET_AKDEN_BASE_CASE = 250000;
	private void setAKDEN(final double[] z, final double[] pp, final double[] akden, 
			final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < SET_AKDEN_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			akden[0] += z[i] * pp[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ setAKDEN(z, pp, akden, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ setAKDEN(z, pp, akden, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	// Needs fixing
	private static int SET_NEWX_R_RR_BASE_CASE = 250000;
	private void setNewX_R_RR(final double ak, final double[] p, final double[] z, final double[] zz, final double[] newX,
			final double[] r, final double[] rr,
			final int a, final int b){
		if(cancelSignal.isCancelled()){ return; }
		// Base Case
		if(b - a < SET_NEWX_R_RR_BASE_CASE){
    		for(int i = a; i <= b; i++){
    			if(cancelSignal.isCancelled()){ return; }
    			newX[i] += ak * p[i];
				r[i] -= ak * z[i];
				rr[i] -= ak * zz[i];
    		}
    		return;
    	}
		// Recurse
    	final int halfSplit = (b - a) / 2 + a;
    	Thread t1 = new Thread(){ public void run(){ setNewX_R_RR(ak, p, z, zz, newX, r, rr, a, halfSplit); }};
    	Thread t2 = new Thread(){ public void run(){ setNewX_R_RR(ak, p, z, zz, newX, r, rr, halfSplit + 1, b); }}; 
    	t1.start(); t2.start();
    	try { t1.join(); } catch(InterruptedException e) { e.printStackTrace(); }
    	try { t2.join(); } catch(InterruptedException e) { e.printStackTrace(); }
	}
	
	private void asolve(int n, double b[], double x[], int itrnsp, int rows, int cols) {
		for(int r = 0; r < rows; r++) {
			for(int c = 0; c < cols; c++) {
				if(cancelSignal.isCancelled()){ return; }
				x[(r * cols) + c + 1] = -4 * b[(r * cols) + c + 1];
			}
		}
	}
	
	private void atimes(long n, double[] x, double[] res, int itrnsp, int rows, int cols) {
		for(int r = 1; r < rows - 1; r++) {
			for(int c = 1; c < cols - 1; c++) {
				if(cancelSignal.isCancelled()){ return; }
				// Core/Center
				res[(r * cols) + c + 1] = x[((r - 1) * cols) + c + 1] 
				                      + x[((r + 1) * cols) + c + 1] 
				                      + x[(r * cols) + c] 
				                      + x[(r * cols) + c + 2] 
				                      - 4 * x[(r * cols) + c + 1];
			}
		}
		
		for(int r = 1; r < rows - 1; r++) {
			if(cancelSignal.isCancelled()){ return; }
			// Left edge
			res[(r * cols) + 1] = x[((r - 1) * cols) + 1] 
			                      + x[((r + 1) * cols) + 1] 
			                      + x[(r * cols) + 2] 
			                      - 3 * x[(r * cols) + 1];
			
			// Right edge
			res[(r * cols) + cols] = x[((r - 1) * cols) + cols] 
			                         + x[((r + 1) * cols) + cols] 
			                         + x[(r * cols) + cols - 1] 
			                         - 3 * x[(r * cols) + cols];
		}
		
		for(int c = 1; c < cols - 1; c++) {
			if(cancelSignal.isCancelled()){ return; }
			// Top edge
			res[c + 1] = x[cols + c + 1] 
			             + x[c] 
			             + x[c + 2] 
			             - 3 * x[c + 1];
			
			// Bottom edge
			res[((rows - 1) * cols) + c + 1] = x[((rows - 2) * cols) + c + 1] 
			                                   + x[((rows - 1) * cols) + c] 
			                                   + x[((rows - 1) * cols) + c + 2] 
			                                   - 3 * x[((rows - 1) * cols) + c + 1];
		}
		
		// 4 Corners
		res[1] = x[cols + 1] 
		         + x[2] 
		         - 2 * x[1];
		
		res[((rows - 1) * cols) + 1] = x[((rows - 2) * cols) + 1] 
		                               + x[((rows - 1) * cols) + 2] 
		                               - 2 * x[((rows - 1) * cols) + 1];
		
		res[cols] = x[cols + cols] 
		            + x[cols - 1] 
		            - 2 * x[cols];
		
		res[((rows - 1) * cols) + cols] = x[((rows - 2) * cols) + cols] 
		                                  + x[((rows - 1) * cols) + cols - 1] 
		                                  - 2 * x[((rows - 1) * cols) + cols];
	}

	private double snrm(int n, double sx[], int itol) {
		int i, isamax;
		double ans;

		if(itol <= 3) {
			ans = 0.0;
			for(i = 1; i <= n; i++) {
				if(cancelSignal.isCancelled()){ return 0; }
				ans = ans + (sx[i] * sx[i]);
			}
			return Math.sqrt(ans);
		} 
		else {
			isamax = 1;
			for(i = 1; i <= n; i++) {
				if(cancelSignal.isCancelled()){ return 0; }
				if(Math.abs(sx[i]) > Math.abs(sx[isamax])) {
					isamax = i;
				}
			}
			return Math.abs(sx[isamax]);
		}
	}
}

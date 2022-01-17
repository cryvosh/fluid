/*
 * Incompressible 2D fluid simulation based on the work of Jos Stam.
 * By Kavosh Jazar
 * 
 * References:
 * 
 * Jos Stam, "Stable Fluids", In SIGGRAPH 99 Conference Proceedings, Annual Conference Series, August 1999, 121-128.
 * Jos Stam, "Real-Time Fluid Dynamics for Games". Proceedings of the Game Developer Conference, March 2003.
 * Dan Morris' Notes on Stable Fluids (Jos Stam, SIGGRAPH 1999)
 *  
 * http://www.gamasutra.com/view/feature/1549/practical_fluid_dynamics_part_1.php?print=1
 * https://www.techhouse.org/~dmorris/projects/summaries/dmorris.stable_fluids.notes.pdf
 * http://www.multires.caltech.edu/teaching/demos/java/stablefluids.htm
 */

public class Solver {

	//Declaring public variables
	int n, size;
	int iterations;
	float timeStep;
	
	boolean openRight = false;

	float viscosity = 0.0f;
	float diff = 0.0001f;

	float[][] temp;

	float[][] drawBnd;
	float[][] dens, dens2;
	float[][] Xvelo, Xvelo2;
	float[][] Yvelo, Yvelo2;

	//Initializes necessary 2d arrays and resets their values
		//drawBnd is a user generated array used to define boundaries
		//dens, dens2 store the current and previous density values
		//Xvelo, Xvelo2 store the X component of the current and previous velocities
		//Yvelo, Yvelo2 store the Y component of the current and previous velocities
	public void setup(int x, float dt, int iter) {
		
		n = x;
		timeStep = dt;
		size = n + 2;
		iterations = iter;

		drawBnd		= new float[size][size];
		dens 		= new float[size][size];
		dens2 		= new float[size][size];
		Xvelo		= new float[size][size];
		Xvelo2 		= new float[size][size];
		Yvelo 		= new float[size][size];
		Yvelo2 		= new float[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				drawBnd[i][j] = dens[i][j] = dens2[i][j] = Xvelo[i][j] = Xvelo2[i][j] = Yvelo[i][j] = Yvelo2[i][j] = 0f;
			}
		}
	}
	
	//A single method used to move the simulation forward by 1 timeStep
	public void step () {
		velocitySolver ();
		densitySolver ();
	}

	//Array s (source) is either a velocity or density array that has
	//been modified by user interaction from the Renderer class.
	//This method allows a modified array to properly influence
	//the target array x.
	public void addSource (float[][] x, float[][] s) {

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				x[i][j] += timeStep * s[i][j];
			}
		}
	}
	
	//Final density solving method for a given timeStep
	public void densitySolver () {
		
		addSource(dens, dens2);
		
		swapD();
		diffuse (0, dens, dens2, diff);
		
		swapD();
		advect (0, dens, dens2, Xvelo, Yvelo);
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				dens2[i][j] = 0;
			}
		}
	}
	
	//Single method to solve final velocity for a given timeStep;
	public void velocitySolver () {
		
		//Add user-generated velocities
		addSource(Xvelo, Xvelo2);
		addSource(Yvelo, Yvelo2);
		
		//Calculating X velocity diffusion
		swapX();
		diffuse (0, Xvelo, Xvelo2, viscosity);
		
		//Calculating Y velocity diffusion
		swapY();
		diffuse (0, Yvelo, Yvelo2, viscosity);
		
		//Enforcing mass conservation
		project (Xvelo, Yvelo, Xvelo2, Yvelo2);
		
		swapX();
		swapY();
		
		//Self advecting velocity
		advect (1, Xvelo, Xvelo2, Xvelo2, Yvelo2);
		advect (2, Yvelo, Yvelo2, Xvelo2, Yvelo2);
		
		//Reinforcing mass conservation
		project (Xvelo, Yvelo, Xvelo2, Yvelo2);
		
		//Resetting input velocities for next timeStep
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Xvelo2[i][j] = 0;
				Yvelo2[i][j] = 0;
			}
		}
	}
	
	//Allows fluid density to spread across neighboring grid cells.
		//The new density array is calculated by finding the densities that,
		//when diffused backwards in time yield the original density array difRay.
		//The linearSolver allows us to solve for the values of difStore.
	public void diffuse (int b, float[][] difStore, float[][] difRay, float diff) {
		
		float a = timeStep * diff * n * n;
		linearSolver (b, difStore, difRay, a, 1 + 4 * a);
		
	}
	
	//Enforces conservation of mass for each fluid cell
		//A mathematical theorem called the Helmholtz-Hodge decomposition states that every
		//velocity field is the sum of an incompressible field and a gradient field. Thus, to simulate an
		//incompressible fluid, we must subtract the gradient field from the current velocity field.
		//To do this, we must solve a linear system known as the Poisson equation. This can be solved using
		//our Gauss-Seidel based linearSolver.
	public void project (float[][] velX, float[][] velY, float[][] temp, float[][] tempDiv) {
		
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				
				tempDiv[i][j] = (velX[i+1][j] - velX[i-1][j] + velY[i][j+1] - velY[i][j-1]) * - 0.5f/n;
				temp[i][j] = 0;
				
			}
		}
		
		setBound (0, tempDiv);
		setBound (0, temp);

		linearSolver (0, temp, tempDiv, 1, 4);

		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				velX[i][j] -= 0.5f * n * (temp[i+1][j] - temp[i-1][j]);
				velY[i][j] -= 0.5f * n * (temp[i][j+1] - temp[i][j-1]);
			}
		}
		
		setBound (1, velX);
		setBound (2, velY);
		
	}
	
	//An iterative implementation of the Gauss-Seidel relaxation
	//technique used to solve linear systems.
	public void linearSolver (int b, float[][] x, float[][] x0, float a, float c) {
		
		for (int k = 0; k < iterations; k++) {
			for (int i = 1; i <= n; i++) {
				for (int j = 1; j <= n; j++) {
					x[i][j] = (a * (x[i-1][j] + x[i+1][j] + x[i][j-1] + x[i][j+1]) + x0[i][j]) / c;
				}
			}
			setBound (b, x);
		}
	}

	//Moves density through the velocity field by looking for particles which,
	//when advected backwards in time, end up at the current cell's center. The initial
	//position of this particle is an float. The simulation, however, is running in a discrete
	//array. Thus, the density at this point must be found by taking a weighted average of densities
	//of the 4 grid cells closes to it. Stam calls this 'linear backtracing'.
	public void advect(int bound, float[][] advected, float[][] ray2Advect, float[][] velX, float[][] velY) {

		int i0, j0, i1, j1;
		float x, y, s0, t0, s1, t1, dt;

		dt = timeStep * n;
		
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {

				//Goes backwards in time through velocity field
				x = i - dt * velX[i][j];
				y = j - dt * velY[i][j];

				//Interpolates results
				if (x > n + 0.5f) {
					x = n + 0.5f;
				} else if (x < 0.5f) {
					x = 0.5f;
				}
				
				i0 = (int) x;
				i1 = i0 + 1;

				if (y > n + 0.5f) {
					y = n + 0.5f;
				} else if (y < 0.5f) {
					y = 0.5f;
				}

				j0 = (int) y;
				j1 = j0 + 1;

				s1 = x - i0;
				s0 = 1 - s1;
				t1 = y - j0;
				t0 = 1 - t1;

				//Modifies target array with a weighted average of the interpolated results
				advected[i][j] = s0 * (t0 * ray2Advect[i0][j0] + t1 * ray2Advect[i0][j1])
							   + s1 * (t0 * ray2Advect[i1][j0] + t1 * ray2Advect[i1][j1]);
			}
		}
		setBound(bound, advected);
	}
	
	//Enforces boundary conditions
		//if b = 1, then the top and bottom edges of array are open
		//if b = 2, then the right and left edges of the array are open
		//else all edges of the array are open
	public void setBound(int b, float[][] x) {
		
		for (int i = 1; i <= n; i++) {
			
			//Condensed if/else notation
			//For example, first line is the same as
			//if (b == 1) {x[0][i] = -x[1][i]} else {x[0][i] = x[1]i]}
			
			x[0][i] 	= b == 1 ? -x[1][i] : x[1][i]; //left edge
			
			//If openRight == true, don't enforce right boundry condition
			if(openRight == false) {
				x[n+1][i] 	= b == 1 ? -x[n][i] : x[n][i]; //right edge
			} else {
				x[n+1][i] = x[n][i];
			}
			
			x[i][0] 	= b == 2 ? -x[i][1] : x[i][1]; //bottom edge
			x[i][n+1] 	= b == 2 ? -x[i][n] : x[i][n]; //top edge
			
			//Enforcing boundaries of user-drawn walls
			for (int j = 1; j <= n; j++) {
				if (drawBnd[i][j] != 0 && i < n && j < n) {
					x[i - 1][j] = b == 1 ? -x[i - 2][j] : x[i - 2][j];
					x[i][j] = 0;
					x[i + 1][j] = b == 1 ? -x[i + 2][j] : x[i + 2][j];
					
					x[i][j - 1] = b == 2 ? -x[i][j - 2] : x[i][j - 2];
					x[i][j] = 0;
					x[i][j + 1] = b == 2 ? -x[i][j + 2] : x[i][j + 2];
				}
			}
			
		}
		
		//Setting boundaries at corner cells to the average neighbors
		x[0][0] 	= 0.5f * (x[1][0] + x[0][1]);
		x[0][n+1] 	= 0.5f * (x[1][n+1] + x[0][n]);
		x[n+1][0] 	= 0.5f * (x[n][0] + x[n+1][1]);
		x[n+1][n+1] = 0.5f * (x[n][n+1] + x[n+1][1]);
		
	}

	//Convenient swap functions
	public void swapX(){
		temp = Xvelo;
		Xvelo = Xvelo2;
		Xvelo2 = temp;
	}
	public void swapY () {
		temp = Yvelo;
		Yvelo = Yvelo2;
		Yvelo2 = temp;
	}
	public void swapD(){
		temp = dens;
		dens = dens2;
		dens2 = temp;
	}
	
}

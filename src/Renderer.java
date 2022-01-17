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

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.Random;
import java.util.ArrayList;
import java.util.Hashtable;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;


public class Renderer extends JPanel implements MouseListener, MouseMotionListener, ActionListener, ChangeListener, KeyListener {
 
 //Declaring all variables
 private static final long serialVersionUID = 1L;
 static Solver solv = new Solver ();
 static Renderer rend;
 static JFrame frame;
 static Container c;
 
 Particle part;
 static JLabel label;
 Thread renderThread;
 
 float cellSizeX, cellSizeY;
 int xPos, yPos, xVect, yVect;
 int offset = 1;
 
 boolean wind = false;
 boolean drawDens = false;
 boolean drawVectors = false;
 boolean drawParticles = true;
 
 int size;
 long time1, time2;
 int xNow, yNow, xPrev, yPrev;
 int xPartIndex, yPartIndex;
 float xPartTemp, yPartTemp;
 float xPartVel, yPartVel;
 int pRed, pGreen, pBlue;
 int xIndexNow, yIndexNow;
 int xIndexOld, yIndexOld;
 int xIndexMid, yIndexMid;
 float xMidTemp, yMidTemp;
 float fps;
 int halfN;
 int button;
 int densColor;
 float partColor;
 
 int densColorOffset = 60;
 int minPartColor = 80;
 int partColorScale = 70;
 int densColorScale = 50;
 int midPoints = 10;
 int drawMidPoints = 50;

 float vectorDrawThreshhold = 3;
 int mouseDragRadius = 3;
 int drawDensRadius = 3;
 int drawBndRadius = 1;
 float densityEmission;
 float densEmissionScale = 0.7f;
 float vectorDrawScale = 1300;
 int minMouseDelta = 0;
 int deltaX, deltaY;
 
 static int n = 80;
 static float timeStep = 0.2f;
 static int iterations = 10;
 
 int particleSize = 1;
 static int particleCount = 20000;
 static float particleMoveScale = 1100;
 
 int currentWidth;
 int currentHeight;
 int currentGridWidth;
 int currentGridHeight;
 float windowScaleX, windowScaleY;
 
 static int width = 700;
 static int height = 700;
 
 static int nSlideMin = 10;
 static int nSlideMax = 200;
 static int iterSlideMin = 1;
 static int iterSlideMax = 50;
 static int pCountSlideMin = 1000;
 static int pCountSlideMax = 100000;
 static int timeSlideMin = 1;
 static int timeSlideMax = 100;
 static int mouseVectorSlideMin = 1;
 static int mouseVectorSlideMax = 20;
 static int partMoveSlideMin = 500;
 static int partMoveSlideMax = 3000;
 
 static float mouseVectorScale = 0.05f;
 
 ArrayList<Particle> particles;
 Random rand = new Random ();
 
 static ArrayList<JSlider> sliders = new ArrayList <JSlider>();
 JSlider nSlide;
 JSlider iterSlide;
 JSlider pCountSlide;
 JSlider timeSlide;
 JSlider mouseVectorSlide;
 JSlider partMoveSlide;
 
 static ArrayList<String> labels = new ArrayList <String>();
 static ArrayList<String> help = new ArrayList <String>();

 
 public static void main (String[] args) {
  
  //Adding labels to arraylist for easy iteration
  labels.add("Grid Resolution");
  labels.add("Solver Iterations");
  labels.add("Particle Count");
  labels.add("TimeStep");
  labels.add("Mouse Force Multiplier");
  labels.add("Particle Motion Multiplier");
  
  //Adding help messages to arraylist for easy iteration
  help.add("Left click and drag to move fluid");
  help.add("Right click and drag to draw boundaries");
  help.add(" ");
  help.add("Press r to reset the simulation");
  help.add("Press q to reset the particles");
  help.add(" ");
  help.add("Press v to add jetstream and");
  help.add("remove right wall");
  help.add(" ");
  help.add("Press 1 to toggle density rendering");
  help.add("Press 2 to toggle velocity vector rendering");
  help.add("Press 3 to toggle particle rendering");
  
  //Setting up frame
  rend = new Renderer ();
  rend.setBackground(Color.black);
  rend.setPreferredSize(new Dimension(width, height));
  
  frame = new JFrame ("Fluid Simulation");
  frame.setLayout(new BorderLayout());
  
  JPanel settings = new JPanel ();
  settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
  settings.setPreferredSize(new Dimension(300, height));
  
  //Adding sliders to arraylist for easy iteration
  sliders.add(rend.nSlide = new JSlider (JSlider.HORIZONTAL, nSlideMin, nSlideMax, n));
  sliders.add(rend.iterSlide = new JSlider (JSlider.HORIZONTAL, iterSlideMin, iterSlideMax, iterations));
  sliders.add(rend.pCountSlide = new JSlider (JSlider.HORIZONTAL, pCountSlideMin, pCountSlideMax, particleCount));
  sliders.add(rend.timeSlide = new JSlider (JSlider.HORIZONTAL, timeSlideMin, timeSlideMax, (int)(timeStep*100)));
  sliders.add(rend.mouseVectorSlide = new JSlider (JSlider.HORIZONTAL, mouseVectorSlideMin, mouseVectorSlideMax, (int)(mouseVectorScale*100)));
  sliders.add(rend.partMoveSlide = new JSlider (JSlider.HORIZONTAL, partMoveSlideMin, partMoveSlideMax, (int)particleMoveScale));

  //For each slider, add it to the settings panel, add labels, and set border
  for (int i = 0; i < sliders.size(); i++) {
   JSlider slide = sliders.get(i);
   slide.addChangeListener(rend);
   
   Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
   labelTable.put(new Integer(slide.getMinimum()), new JLabel(String.valueOf(slide.getMinimum())));
   labelTable.put(new Integer(slide.getValue()), new JLabel(String.valueOf(slide.getValue())));
   labelTable.put(new Integer(slide.getMaximum()), new JLabel(String.valueOf(slide.getMaximum())));
   
   slide.setLabelTable(labelTable);
   
   slide.setPaintLabels(true);
   
   label = new JLabel(labels.get(i));
   settings.add(label);
   label.setAlignmentX(Component.CENTER_ALIGNMENT);
   settings.add(slide);
   slide.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
  }
  
  //VerticalGlue puts everything else at the bottom of the settings panel
  settings.add(Box.createVerticalGlue());
  
  //Add help labels
  for (int i = 0; i < help.size(); i++) {
   label = new JLabel(help.get(i));
   settings.add(label);
   label.setAlignmentX(Component.CENTER_ALIGNMENT);
  }
  
  //Finish setting up frame
  settings.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
  
  frame.add(rend, BorderLayout.CENTER);
  frame.add(settings, BorderLayout.EAST);
  
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  frame.setUndecorated(false);
  frame.pack();  
  frame.setVisible(true);
  
 }
 
 //Initialize public variables and generate particles
 public void setup () {
  solv.setup(n, timeStep, iterations);
  size = n + 2;
  halfN = n/2;
  
  wind = false;
  
  resetParticles ();
 }
 
 //Randomly generate Particle class objects and add them to arraylist for easy iteration
 public void resetParticles () {
  particles = new ArrayList<Particle> ();
  
  for (int i = 0; i < particleCount; i++) {
   particles.add(new Particle(rand.nextInt(width), rand.nextInt(height)));
  }
 }
 
 //Add listeners, run setup method, and start renderThread
 public Renderer () {
  
  addMouseMotionListener(this);
  addMouseListener(this);
  addKeyListener(this);
  setFocusable(true);
  setup ();
  
  renderThread = new renderThread ();
  renderThread.start ();
 }
 
 //Separate class needed to extend Thread without implementing Runnable
 class renderThread extends Thread {
  public void run () {
   while(Thread.currentThread() == renderThread) {
    try {
     Thread.sleep(0);
    } catch (InterruptedException e) {
     e.printStackTrace();
    }
    repaint ();
   }
  }  
 }
 
 //Particle class, stores new and old x,y coordinates
 class Particle {
  
  float x, y;
  float xOld, yOld;
  
  Particle (float x, float y) {
   this.x = x;
   this.y = y;
  }
 }
 
 //Used to create jetStream
 public void addVel () {
  for (int i = -2; i < 2; i++) {
   for(int j = -2; j < 2; j++)
    solv.Xvelo2[5 + i][halfN + j] = 0.1f;
  }
 }
 
 //Paint method, called every frame
 public void paintComponent (Graphics g) {
  
  //Storing current system time to calculate frame rate later
  time1 = System.nanoTime();
  
  //Calling super.paintComponent(g) to auto clear the frame every time repaint() is called
  super.paintComponent(g);
  
  Graphics2D g2 = (Graphics2D) g.create();
  
  if (wind == true) {
   addVel ();
  }
  
  //Calling step method in Solver class to move simulation forward 1 timeStep
  solv.step ();
  
  //Calculating various dimensions to use while rendering cells and resizing the window
  currentWidth = rend.getWidth();
  currentHeight = rend.getHeight();
  
  cellSizeX = (float) currentWidth / n;
  cellSizeY = (float) currentHeight / n;
  
  currentGridWidth = (int) (cellSizeX * n);
  currentGridHeight = (int) (cellSizeY * n);
  
  windowScaleX = (float) currentGridWidth / width;
  windowScaleY = (float) currentGridHeight / height;
  
  //Loop through each cell of the grid
   //xPos and yPos are the coordinates of the center pixel of each cell
  for (int i = 1; i <= n; i += offset) {
   xPos = (int) ((i - 0.5f) * cellSizeX);
   for (int j = 1; j <= n; j += offset) {
    yPos = (int) ((j - 0.5f) * cellSizeY);
    //If density drawing is enabled and the cell has some density
    if (drawDens == true && solv.dens[i][j] > 0.00001) {
     //Calculate color of cell based on its density value
     densColor = (int) (solv.dens[i][j] * densColorScale);

     //Prevents IllegalArgumentException
     if (densColor > 255) {
      densColor = 255;
     }
     
     //Set color and draw cell
     g2.setColor(new Color(densColor, densColor, densColor));
     g2.setStroke(new BasicStroke(0));
     g2.fillRect((int)(xPos - cellSizeX/2), (int)(yPos - cellSizeY/2), (int)cellSizeX, (int)cellSizeY);
    }
    
    //Drawing user-defined boundaries
    //The current cell or any of its 4 neighbors are marked as a boundary, fill this cell with white
    if (solv.drawBnd[i][j] == 1 || solv.drawBnd[i-1][j] == 1 || solv.drawBnd[i+1][j] == 1 || solv.drawBnd[i][j-1] == 1 || solv.drawBnd[i][j+1] == 1) {
     g2.setColor(Color.white);
     g2.fillRect ((int)(xPos - cellSizeX/2), (int)(yPos - cellSizeY/2), (int)cellSizeX, (int)cellSizeY);
    }
    
    //Calculating velocity vectors
    g2.setColor(Color.red);
    if (drawVectors == true ) {
     xVect = (int)(vectorDrawScale * solv.Xvelo[i][j]);
     yVect = (int)(vectorDrawScale * solv.Yvelo[i][j]);
     
     //Only draw vector if its magnitude is greater than vectorDrawThreshold
     if(Math.sqrt(xVect * xVect + yVect * yVect) > vectorDrawThreshhold) {
      g2.drawLine(xPos, yPos, xPos + xVect, yPos + yVect);
     }
    }
   }
  }
  
  //For each particle in particles arraylist
  for (int i = 0; i < particles.size(); i++) {
   
   //If particleDrawing is enabled
   if (drawParticles == true) {
    //Setting public Particle object part equal to the current Particle object
    //This allows me to manipulate 'part' instead of 'particles.get(i)'
    part = particles.get(i);
    
    part.xOld = part.x;
    part.yOld = part.y;
    
    //Caching part variables
    xPartTemp = part.x;
    yPartTemp = part.y;
    
    //Getting the current array index of the particle from its pixel coordinates
    xPartIndex = xIndex ((float)part.x * windowScaleX) + 1;
    yPartIndex = yIndex ((float)part.y * windowScaleY) + 1;
    
    //Calculating particle velocity based on the velocity of the array cell it's in
    xPartTemp += solv.Xvelo[xPartIndex][yPartIndex] * timeStep * particleMoveScale;
    yPartTemp += solv.Yvelo[xPartIndex][yPartIndex] * timeStep * particleMoveScale;
    
    //Keeping particles inside domain
    if (xPartTemp > currentGridWidth / windowScaleX) {
     xPartTemp = currentGridWidth / windowScaleX;
    } else if (xPartTemp < 0) {
     xPartTemp = 0;
    }
    
    if (yPartTemp > currentGridHeight / windowScaleY) {
     yPartTemp = currentGridHeight / windowScaleY;
    } else if (yPartTemp < 0) {
     yPartTemp = 0;
    }
    
    //Updating particle position
    part.x = xPartTemp;
    part.y = yPartTemp;
    
    //Calculating change in particle x,y values
    //Since timeStep is irrelevant here, we can say that displacement = velocity
    xPartVel = Math.abs(part.x - part.xOld);
    yPartVel = Math.abs(part.y - part.yOld);
    
    //partColor = magnitude of the displacement/velocity vectors
    partColor = (float) (Math.sqrt(xPartVel * xPartVel + yPartVel * yPartVel) * partColorScale);
    
    //Setting color of each channel based on a function of partColor (velocity/displacement magnitude)
    pRed  = (int) (Math.pow(partColor, 0.9) + minPartColor);
    pGreen  = (int) (Math.pow(partColor, 0.75) + minPartColor);
    pBlue  = (int) (Math.pow(partColor, 1.3) + minPartColor);
    
    //Prevents IllegalArgumentException
    if (pRed > 255){
     pRed = 255;
    } else if (pRed < 0){
     pRed = 0;
    }
    if (pGreen > 255){
     pGreen = 255;
    } else if (pGreen < 0){
     pGreen = 0;
    }
    if (pBlue > 255){
     pBlue = 255;
    } else if (pBlue < 0){
     pBlue = 0;
    }
    
    //Setting color and drawing line between new particle position and old particle position to simulate motion blur
    g2.setColor(new Color(pRed, pGreen, pBlue));
    
    g2.setStroke(new BasicStroke(particleSize));
    g2.drawLine((int) (part.x * windowScaleX), (int) (part.y * windowScaleY), (int) (part.xOld * windowScaleX), (int) (part.yOld * windowScaleY));
    //g2.drawLine((int) (part.x), (int) (part.y), (int) (part.xOld), (int) (part.yOld)); //deprecated
   }
  }
  
  //Calculating fps (frames per second) and drawing it to the screen
  time2 = System.nanoTime() - time1;
  fps = 1 / (time2 / 1000000000f);
  g2.setColor(Color.yellow);
  g2.setFont(new Font("Ariel", Font.PLAIN, 25));
  g2.drawString("FPS: " + String.valueOf((int)fps), 35, 50);
  
 }
 
 //Calculates the xIndex of a pixel
 public int xIndex (float x) {
  xIndexNow = (int) (x / cellSizeX);
  
  if (xIndexNow > n) {
   xIndexNow = n;
  } else if (xIndexNow < 1) {
   xIndexNow = 1;
  }
  
  return xIndexNow;
 }
 
 //Calculates the yIndex of a pixel
 public int yIndex (float y) {
  yIndexNow = (int) (y / cellSizeY);
  
  if (yIndexNow > n) {
   yIndexNow = n;
  } else if (yIndexNow < 1) {
   yIndexNow = 1;
  }
  
  return yIndexNow;
 }
 
 //Interprets and interpolates user mouse input
 public void mouseSolve (MouseEvent e) {
  
  //Calculates array index of current and previous mouse positions
  xIndexNow = (int) (xNow / cellSizeX);
  yIndexNow = (int) (yNow / cellSizeY);
  
  xIndexOld = (int) (xPrev / cellSizeX);
  yIndexOld = (int) (yPrev / cellSizeY);
  
  //Prevents arrayIndexOutOfBounds exception
  if (xIndexNow > n) {
   xIndexNow = n;
  } else if (xIndexNow < 1) {
   xIndexNow = 1;
  }
  if (yIndexNow > n) {
   yIndexNow = n;
  } else if (yIndexNow < 1) {
   yIndexNow = 1;
  }
  if (xIndexOld > n) {
   xIndexOld = n;
  } else if (xIndexOld < 1) {
   xIndexOld = 1;
  }
  if (yIndexOld > n) {
   yIndexOld = n;
  } else if (yIndexOld < 1) {
   yIndexOld = 1;
  }
  
  //if right click, draw boundaries
  if (button == 3) {
   //MouseDragged event fires slowly, thus we must interpolate mouse motion
   //to prevent gaps in boundary if user moves mouse quickly.
   for (int i = 0; i < drawMidPoints; i++) {
    
    //Drawing boundaries using a radius
    //Allows for drawing of thicker boundaries if desired
    for (int j = 0; j < drawBndRadius; j++) {
     for (int k = 0; k < drawBndRadius; k++) {
      //Calculating current midpoint index
      xMidTemp = xIndexOld + ((xIndexNow - xIndexOld) / (float) drawMidPoints) * i;
      yMidTemp = yIndexOld + ((yIndexNow - yIndexOld) / (float) drawMidPoints) * i;
    
      xIndexMid = (int) xMidTemp + 1;
      yIndexMid = (int) yMidTemp + 1;
      
      //Preventing arrayIndexOutOfBounds exception
      if (xIndexMid >= n) {
       xIndexMid = n - 1;
      } else if (xIndexMid < 1) {
       xIndexMid = 1;
      }
      if (yIndexMid >= n) {
       yIndexMid = n - 1;
      } else if (yIndexMid < 1) {
       yIndexMid = 1;
      }
    
      //Marking the current index as a boundary
      solv.drawBnd[xIndexMid + j][yIndexMid + k] = 1;
     }
    }    
   }
  }
  
  //If right click and dragged, add density and velocity to fluid
  if (button == 1 && e.getID() == MouseEvent.MOUSE_DRAGGED) {

   //Calculating change in x and y mouse positions
   deltaX = xNow - xPrev;
   deltaY = yNow - yPrev;
   
   //If the mouse movement is sufficiently large
   if(Math.abs(deltaX) > minMouseDelta || Math.abs(deltaY) > minMouseDelta) {
    
    //Calculate amount of density to emit based on magnitude of mouse velocity vector
    densityEmission = (float) Math.sqrt((deltaX * deltaX + deltaY * deltaY)) * densEmissionScale;
    //densityEmission = densEmissionScale;
    
    //Interpolating mouse movement using midpoints
    for (int i = 0; i < midPoints; i++) {
     //Calculating midpoints
     xMidTemp = xIndexOld + ((xIndexNow - xIndexOld) / (float) midPoints) * i;
     yMidTemp = yIndexOld + ((yIndexNow - yIndexOld) / (float) midPoints) * i;
     
     //Getting array index of midpoints
     xIndexMid = (int) xMidTemp;
     yIndexMid = (int) yMidTemp;
     
     //Preventing arrayIndexOutOfBounds exception
     if (xIndexMid >= n) {
      xIndexMid = n - 1;
     } else if (xIndexMid < 1) {
      xIndexMid = 1;
     }
     if (yIndexMid >= n) {
      yIndexMid = n - 1;
     } else if (yIndexMid < 1) {
      yIndexMid = 1;
     }
     
     //Adding density to density array
     for (int j = 0; j < drawDensRadius; j++) {
      for (int k = 0; k < drawDensRadius; k++) {
       solv.dens2[xIndexMid + j][yIndexMid + k] = densityEmission;
      }
     }
    
     //Adding velocity to velocity array
     for (int j = 0; j < mouseDragRadius; j++) {
      for (int k = 0; k < mouseDragRadius; k++) {
       solv.Xvelo2[xIndexMid + j][yIndexMid + k] = deltaX * timeStep * mouseVectorScale;
       solv.Yvelo2[xIndexMid + j][yIndexMid + k] = deltaY * timeStep * mouseVectorScale;
      }
     }
    }
   }
  }
  
 }

 //Fires whenever mouse is dragged
 @Override
 public void mouseDragged(MouseEvent e) {
  
  xPrev = xNow;
  yPrev = yNow;
  xNow = e.getX();
  yNow = e.getY();
  
  mouseSolve(e);
  
 }
 
 //Used to modify variables using JSliders
 @Override
 public void stateChanged(ChangeEvent e) {
  if (e.getSource() == nSlide) {
   n = nSlide.getValue();
   setup();
  } else if (e.getSource() == iterSlide) {
   iterations = iterSlide.getValue();
   setup();
  } else if (e.getSource() == pCountSlide) {
   particleCount = pCountSlide.getValue();
   resetParticles();
  } else if (e.getSource() == timeSlide) {
    //JSliders only return integers, thus, to get decimal values from
    //them we divide by 100
   timeStep = ((float)timeSlide.getValue()) / 100f;
   setup();
  } else if (e.getSource() == mouseVectorSlide) {
   mouseVectorScale = ((float)mouseVectorSlide.getValue()) / 100f;
  } else if (e.getSource() == partMoveSlide) {
   particleMoveScale = partMoveSlide.getValue();
  }
  
  rend.requestFocus();
 }
 
 //Fires when keyboard buttons are pressed
 @Override
 public void keyPressed(KeyEvent e) {
  
  if (e.getKeyChar() == 'r') {
   setup();
  } else if (e.getKeyChar() == 'q') {
   resetParticles();
  } else if (e.getKeyChar() == '1') {
   drawDens = !drawDens;
  } else if (e.getKeyChar() == '2') {
   drawVectors = !drawVectors;
  } else if (e.getKeyChar() == '3') {
   drawParticles = !drawParticles;
  } else if (e.getKeyChar() == 'v') {
   if(wind == true){
    wind = false;
    solv.openRight = false;
   } else {
    wind = true;
    solv.openRight = true;
   }
  }
 }
 
 //Fires whenever mouse is pressed
 @Override
 public void mousePressed(MouseEvent e) {
  button = e.getButton();  
  
  xNow = e.getX();
  yNow = e.getY();
  
 }
 
 //Java requires these methods to be here
 @Override
 public void mouseMoved(MouseEvent e) {}
 @Override
 public void mouseClicked(MouseEvent e) {}
 @Override
 public void mouseEntered(MouseEvent e) {}
 @Override
 public void mouseExited(MouseEvent e) {}
 @Override
 public void mouseReleased(MouseEvent e) {}
 @Override
 public void keyReleased(KeyEvent e) {}
 @Override
 public void keyTyped(KeyEvent e) {}
 @Override
 public void actionPerformed(ActionEvent e) {}
 
}
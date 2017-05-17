
import java.awt.Color;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Random;
import java.util.concurrent.Semaphore;

/*
 * @author Eric M Evans
 * 
 * free-body problem. multi-threaded solution. includes main method.
 */
public class FreeBodies extends Observable {
	
	public ArrayList<Body>		bodies;
	
	private double				G				= 10000;
	private double				fps				= 500;
	
	public boolean				gravity			= true;
	public boolean				walls			= false;
	boolean						usingGUI;
	boolean						play			= false;
	
	int							leftWall		= 0;
	int							rightWall		= 10000;
	int							topWall			= 0;
	int							bottomWall		= 10000;
	
	public int					numWorkers		= 8;
	int							numTimeSteps	= 0;			// 0: endless
	                                                            // loop
	private int					numCollisions	= 0;
	
	private int					workersDone		= 0;
	Semaphore[][]				barrierMsgs		= null;
	int							rounds;
	
	private static final String	guiArg			= "--gui";
	
	private static Random		rand			= new Random();
	
	
	
	/*
	 * MAIN
	 */
	public static void main(String[] args) {
		
		FreeBodies freebodies = new FreeBodies(args);
		
		try {
			freebodies.loop();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	
	/*
	 * setup Simulation, either as GUI driven, or command line driven
	 */
	public FreeBodies(String[] args) {
		
		ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
		
		bodies = setUpInitialState(argsList);
		
		/*
		 * FOR GUI MODE
		 */
		if (argsList.contains(guiArg)) {
			System.out.println("GUI Mode");
			usingGUI = true;
			play = false; // to be invoked by GUI
			@SuppressWarnings("unused")
			FreeBodyGUI gui = new FreeBodyGUI(this); // its own thread, observer
		}
		/*
		 * FOR NON-GUI MODE
		 */
		else {
			System.out.println("Non-GUI Mode");
			usingGUI = false;
			play = true;
		}
		
		updateObservers();
	}
	
	
	
	/*
	 * for GUI AND non-GUI modes, creating initial bodies from command line
	 * arguments
	 */
	private ArrayList<Body> setUpInitialState(ArrayList<String> list) {
		
		System.out.println(list);
		
		if (list.size() == 1 && list.contains(guiArg)) {
			rightWall = 750;
			bottomWall = 600;
			return new ArrayList<Body>();
		}
		
		// temporarily remove guiArg arg, if present
		boolean addGUIarg = false;
		if (list.remove(guiArg) == true) {
			addGUIarg = true;
			rightWall = 750;
			bottomWall = 600;
		}
		
		if (list.size() != 4) {
			System.out.println("Error: Command line arguments must be "
			        + "4 integers: \n\t <number of workers, 1 to 32>"
			        + " <number of bodies> <mass of each body> "
			        + "<number of time steps> \n\t invoke \"" + guiArg
			        + "\" for GUI"
			        + "\n\t GUI may be ran with no integer arguments");
			System.exit(0);
		}
		
		/*
		 * Process each command line argument
		 */
		
		// number of workers
		this.numWorkers = Integer.parseInt(list.get(0));
		if (numWorkers < 1 || numWorkers > 32) {
			System.out.println("number of workers must be 1 to 32");
			System.exit(0);
		}
		
		// number of bodies
		int numBodies = Integer.parseInt(list.get(1));
		if (numBodies < 1) {
			System.out.println("number of bodies must be positive");
			System.exit(0);
		}
		
		// mass of each body -- uniform
		int massOfBody = Integer.parseInt(list.get(2));
		if (massOfBody < 1) {
			System.out.println("mass of bodies must be positive");
			System.exit(0);
		}
		
		// number of time steps
		this.numTimeSteps = Integer.parseInt(list.get(3));
		if (numTimeSteps < 1) {
			System.out.println("number of time steps must be positive");
			System.exit(0);
		}
		
		// create bodies
		ArrayList<Body> bodies = new ArrayList<>();
		for (int i = 0; i < numBodies; i++) {
			Body bod = new Body(massOfBody, 20, new Point.Double(0, 0),
			        new Point.Double(0, 0), i, this.numWorkers);
			if (this.placeRandomly(bod, bodies) == false)
				System.out.println("not all " + numBodies
				        + " placed. continuing regardless");
			else
				bodies.add(bod);
		}
		
		if (addGUIarg)
			list.add(guiArg);
		
		return bodies;
	}
	
	
	
	/*
	 * place body in unoccupied random location
	 */
	public boolean placeRandomly(Body bod, ArrayList<Body> list) {
		
		/*
		 * generate random location, not occupied by another body
		 */
		Random rng = new Random();
		int x = 0;
		int y = 0;
		int tries = 0;
		boolean tryAgain;
		do {
			tryAgain = false;
			tries++;
			
			x = (int) (bod.getRadius() + rng.nextInt(
			        (int) (this.getRightWall() - 2 * bod.getRadius())));
			y = (int) (bod.getRadius() + rng.nextInt(
			        (int) (this.getBottomWall() - 2 * bod.getRadius())));
			
			bod.setPosition(new Point.Double(x, y));
			
			for (Body rival : list)
				if (bod.doIntersect(rival, fps, -1)) {
					tryAgain = true;
					break;
				}
			
			if (tries > 10000)
				return false;
		} while (tryAgain);
		
		return true;
	}
	
	
	
	/*
	 * main logic loop of free bodies simulation
	 */
	private void loop() throws InterruptedException {
		
		ArrayList<Worker> workers = new ArrayList<>();
		
		initBarrierMsgs(); // setup Dissemination Barrier
		
		for (int i = 0; i < this.numWorkers; i++)
			workers.add(new Worker(i, this));
		
		final long start = System.currentTimeMillis();
		
		for (Worker w : workers)
			w.start();
		
		// wait for worker threads to terminate
		while (workersDone != numWorkers)
			Thread.sleep(10);
		
		final long finish = System.currentTimeMillis();
		final long elapsed = finish - start;
		final long seconds = elapsed / 1000;
		final long millis = elapsed % 1000;
		
		// System.out.println(elapsed);
		System.out.println("computation time: " + seconds + " seconds " + millis
		        + " milliseconds");
		System.out.println("collisions: " + this.numCollisions);
		
		try {
			writeToFile("output.txt");
		}
		catch (IOException e) {
			System.out.println("Error printing to file");
			e.printStackTrace();
		}
	}
	
	
	
	/*
	 * initialize barrierMsgs -- message passing array
	 */
	void initBarrierMsgs() {
		
		rounds = (int) Math.ceil(Math.log((double) numWorkers) / Math.log(2.0));
		
		barrierMsgs = new Semaphore[rounds][numWorkers];
		
		int r, c;
		for (r = 0; r < rounds; r++)
			for (c = 0; c < numWorkers; c++)
				barrierMsgs[r][c] = new Semaphore(0);
			
	}
	
	
	
	/*
	 * write positions and velocities of bodies to file
	 */
	private void writeToFile(String name) throws IOException {
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(name));
		
		for (Body body : bodies) {
			StringBuilder line = new StringBuilder();
			line.append(String.format("%2d", body.getID()));
			line.append(": Position (");
			line.append(String.format("%+09.3f", body.getPosition().x));
			line.append(",");
			line.append(String.format("%+09.3f", body.getPosition().y));
			line.append("), Velocity (");
			line.append(String.format("%+09.3f", body.getVelocity().x));
			line.append(",");
			line.append(String.format("%+09.3f", body.getVelocity().y));
			line.append(")\n");
			
			bw.write(line.toString());
		}
		bw.close();
	}
	
	
	
	/*
	 * record number of collisions
	 */
	synchronized void recordCollision() {
		
		this.numCollisions++;
	}
	
	
	
	/*
	 * setter
	 */
	public void setRightWall(int rightWall) {
		
		this.rightWall = rightWall;
	}
	
	
	
	/*
	 * setter
	 */
	public void setBottomWall(int bottomWall) {
		
		this.bottomWall = bottomWall;
	}
	
	
	
	/*
	 * update each body's position according to velocities (divided by fps)
	 *
	 * private void moveCollidedBodies() {
	 * 
	 * for (Body body : bodies) {
	 * double x = body.getPosition().x + body.getVelocity().x / fps;
	 * double y = body.getPosition().y + body.getVelocity().y / fps;
	 * body.setPosition(new Point.Double(x, y));
	 * }
	 * }
	 */
	
	public int getRightWall() {
		
		return this.rightWall;
	}
	
	
	
	public int getBottomWall() {
		
		return this.bottomWall;
	}
	
	
	
	/*
	 * wrapper method
	 * called when a Body(s) has been changed
	 */
	public void updateObservers() {
		
		if (!usingGUI)
			return;
		
		setChanged();
		notifyObservers();
	}
	
	
	
	public void addBody(Body body) {
		
		bodies.add(body);
		updateObservers();
	}
	
	
	
	/*
	 * return color for given body
	 */
	public static Color getRandomColor() {
		
		int offset = 45;
		int range = 256 - offset;
		
		return new Color(rand.nextInt(range) + offset,
		        rand.nextInt(range) + offset, rand.nextInt(range) + offset);
	}
	
	
	
	/*
	 * getter
	 */
	public double getG() {
		
		return this.G;
	}
	
	
	
	/*
	 * setter
	 */
	public void setG(int g) {
		
		if (g == 0)
			gravity = false;
		else
			gravity = true;
		
		this.G = g;
	}
	
	
	
	/*
	 * getter
	 */
	public int getFPS() {
		
		return (int) this.fps;
	}
	
	
	
	/*
	 * setter
	 */
	public void setFPS(int fps) {
		
		this.fps = fps;
	}
	
	
	
	/*
	 * setter
	 */
	public void setPlay(boolean b) {
		
		this.play = b;
		updateObservers();
	}
	
	
	
	/*
	 * getter
	 */
	public boolean isPlaying() {
		
		return this.play;
	}
	
	
	
	/*
	 * increment workersDone. called right before a worker is terminated
	 */
	synchronized void oneMoreWorkerDone() {
		
		workersDone++;
		// System.out.println(workersDone + "/" + numWorkers);
	}
}

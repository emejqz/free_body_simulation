
import java.awt.Point;

/*
 * @author Eric M Evans
 * 
 * Worker Thread for Free Bodies
 */
public class Worker extends Thread {
	
	private int			ID;
	private FreeBodies	model;
	private int			steps;
	
	
	
	public Worker(int i, FreeBodies model) {
		this.ID = i;
		this.model = model;
	}
	
	
	
	/*
	 * Dissemination Barrier
	 */
	private void barrier() {
		
		int i;
		int sendID;
		
		for (i = 0; i < model.rounds; i++) {
			
			sendID = (ID + (1 << i)) % model.numWorkers;
			
			model.barrierMsgs[i][sendID].release(); // V(e)
			
			try {
				model.barrierMsgs[i][ID].acquire(); // P(e)
			}
			catch (InterruptedException e) {
				e.printStackTrace(System.out);
				System.exit(1);
			}
		}
		
	}
	
	
	
	/*
	 * main loop
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		
		steps = 0;
		while (true) {
			// System.out.println("Worker: " + ID + ", steps: " + steps);
			if (model.numTimeSteps != 0 && steps >= model.numTimeSteps)
				break;
			
			while (!model.play)
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e1) {
					System.out.println("Error in play/pause sleep");
					e1.printStackTrace();
				}
			
			if (model.gravity) {
				calculateGForces(); // benefits greatly from multi-threading,
				                    // many calculations
				barrier();
			}
			
			if (ID == 0)
				calculateCollisions(); // does not need multi-threading,
				                       // few calculations
				
			barrier();
			
			moveBodies(); // benefits from multi-threading,
			              // many calculations
			
			barrier();
			
			if (ID == 0) // only needs to be called once, by first worker
				model.updateObservers();
			
			if (model.usingGUI) {
				try {
					Thread.sleep((long) (1000 / model.getFPS()));
				}
				catch (InterruptedException e) {
					System.out.println("Error in FPS sleep");
					e.printStackTrace();
				}
			}
			steps++;
		}
		// System.out.println("worker " + ID + " done");
		model.oneMoreWorkerDone();
	}
	
	
	
	/*
	 * figure gravitational force on each object and set new velocities
	 */
	private void calculateGForces() {
		
		Point.Double direction = new Point.Double(0, 0);
		double magnitude = 0;
		double distance = 0;
		final double G = model.getG();
		
		// loop by striping
		for (int i = ID; i < model.bodies.size(); i += model.numWorkers) {
			Body heroI = model.bodies.get(i);
			// System.out.println("hero: " + heroI.getID());
			final double heroPX = heroI.getPosition().x;
			final double heroPY = heroI.getPosition().y;
			final double heroMass = heroI.getMass();
			
			for (int k = i + 1; k < model.bodies.size(); k++) {
				Body rivalJ = model.bodies.get(k);
				// System.out.println("rival: " + rivalJ.getID());
				final double rivalPX = rivalJ.getPosition().x;
				final double rivalPY = rivalJ.getPosition().y;
				final double rivalMass = rivalJ.getMass();
				
				distance = Math.sqrt((heroPX - rivalPX) * (heroPX - rivalPX)
				        + (heroPY - rivalPY) * (heroPY - rivalPY));
				magnitude = (G * heroMass * rivalMass) / (distance * distance);
				
				direction.setLocation(rivalPX - heroPX, rivalPY - heroPY);
				// System.out.println("mag: " + magnitude);
				
				heroI.setForce(ID,
				        new Point.Double(
				                heroI.getForce(ID).x
				                        + magnitude * direction.x / distance,
				                heroI.getForce(ID).y
				                        + magnitude * direction.y / distance));
				
				rivalJ.setForce(ID,
				        new Point.Double(
				                rivalJ.getForce(ID).x
				                        - magnitude * direction.x / distance,
				                rivalJ.getForce(ID).y
				                        - magnitude * direction.y / distance));
			}
		}
		
	}
	
	
	
	/*
	 * update positions according to forces and velocities
	 */
	private void moveBodies() {
		
		Point.Double deltaV = new Point.Double(0, 0);
		Point.Double deltaP = new Point.Double(0, 0);
		
		for (int i = ID; i < model.bodies.size(); i += model.numWorkers) {
			Body body = model.bodies.get(i);
			Point.Double netForce = body.getNetForce();
			
			final double mass = body.getMass();
			final double fps = (double) model.getFPS();
			final double vX = body.getVelocity().x;
			final double vY = body.getVelocity().y;
			
			deltaV.setLocation(netForce.x / mass / fps,
			        netForce.y / mass / fps);
			
			deltaP.setLocation((vX + deltaV.x / 2) / fps,
			        (vY + deltaV.y / 2) / fps);
			
			body.setVelocity(new Point.Double(vX + deltaV.x, vY + deltaV.y));
			
			body.setPosition(new Point.Double(body.getPosition().x + deltaP.x,
			        body.getPosition().y + deltaP.y));
			
			// System.out.println(body.getID() + ": " + body.getForce());
			
			body.zeroOutForces();
			
			/*
			 * Bounce off walls and corral, if applicable
			 */
			if (model.walls) {
				// Left Wall and Right Wall
				if ((body.leftBound() <= model.leftWall
				        && body.getVelocity().x < 0)
				        || (body.rightBound() >= model.rightWall
				                && body.getVelocity().x > 0))
					body.reverseXVelocity();
				
				// Top Wall and Bottom Wall
				if ((body.upperBound() <= model.topWall
				        && body.getVelocity().y < 0)
				        || (body.lowerBound() >= model.bottomWall
				                && body.getVelocity().y > 0))
					body.reverseYVelocity();
			}
		}
		
	}
	
	
	
	/*
	 * check for elastic collisions and set new velocities
	 */
	private void calculateCollisions() {
		
		// System.out.println("checking collisions");
		
		for (int i = 0; i < model.bodies.size() - 1; i++) {
			Body hero = model.bodies.get(i);
			// System.out.println("hero: " + hero.getID());
			
			for (int k = i + 1; k < model.bodies.size(); k++) {
				Body rival = model.bodies.get(k);
				// System.out.println("rival: " + rival.getID());
				if (hero.doIntersect(rival, model.getFPS(), steps))
					collide(hero, rival);
			}
		}
	}
	
	
	
	/*
	 * figure new velocities for hero and rival
	 */
	private void collide(Body hero, Body rival) {
		
		model.recordCollision();
		
		// Masses
		final double m1 = hero.getMass();
		final double m2 = rival.getMass();
		
		// Initial Positions
		final double x1 = hero.getPosition().x;
		final double y1 = hero.getPosition().y;
		final double x2 = rival.getPosition().x;
		final double y2 = rival.getPosition().y;
		
		// Initial Component Velocities
		final double v1x = hero.getVelocity().x;
		final double v1y = hero.getVelocity().y;
		final double v2x = rival.getVelocity().x;
		final double v2y = rival.getVelocity().y;
		
		// define sqrt
		final double sqrt = Math
		        .sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
		
		// Initial Normal Velocities
		final double v1n = (v1x * (x2 - x1) + v1y * (y2 - y1)) / sqrt;
		final double v2n = (v2x * (x2 - x1) + v2y * (y2 - y1)) / sqrt;
		
		// Final Normal Velocities -- dependent on mass
		final double v1nf = (v1n * (m1 - m2) + 2 * m2 * v2n) / (m1 + m2);
		final double v2nf = (v2n * (m1 - m2) + 2 * m1 * v1n) / (m1 + m2);
		
		// Final Normal Component Velocities -- dependent on mass
		final double v1nfx = (v1nf * (x2 - x1)) / sqrt;
		final double v1nfy = (v1nf * (y2 - y1)) / sqrt;
		final double v2nfx = (v2nf * (x2 - x1)) / sqrt;
		final double v2nfy = (v2nf * (y2 - y1)) / sqrt;
		
		// Tangent Velocities -- not dependent on mass
		final double utx = -(y2 - y1) / sqrt;
		final double uty = (x2 - x1) / sqrt;
		final double v1t = (v1x * (-(y2 - y1)) + v1y * (x2 - x1)) / sqrt;
		final double v2t = (v2x * (-(y2 - y1)) + v2y * (x2 - x1)) / sqrt;
		
		// Tangent Component Velocities -- not dependent on mass
		final double v1tx = v1t * utx;
		final double v1ty = v1t * uty;
		final double v2tx = v2t * utx;
		final double v2ty = v2t * uty;
		
		// Final Component Velocities -- dependent on mass
		final double v1fx = v1nfx + v1tx;
		final double v1fy = v1nfy + v1ty;
		final double v2fx = v2nfx + v2tx;
		final double v2fy = v2nfy + v2ty;
		
		// Assign new velocities to bodies
		hero.setVelocity(new Point.Double(v1fx, v1fy));
		rival.setVelocity(new Point.Double(v2fx, v2fy));
		
	}
	
}

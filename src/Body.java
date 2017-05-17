
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Double;

/*
 * @author Eric M Evans
 * 
 * Circular free-body of uniform density, point mass.
 */

public class Body {
	
	private double			mass;
	private double			radius;
	private Point.Double	velocity;
	private Point.Double	position;
	// private Point.Double force;
	private double			epsilon		= -1;
	private Color			color;
	private int				ID;
	private Point.Double[]	forces;
	private int				lastStep	= -1;
	private Double			predicted;
	
	
	
	public Body(int mass, double radius, Point.Double velocity,
	        Point.Double position, int id, int numWorkers) {
		
		this.setMass(mass);
		this.setRadius(radius);
		this.setVelocity(velocity);
		this.setPosition(position);
		// this.force = new Point.Double(0.0, 0.0);
		
		this.color = FreeBodies.getRandomColor();
		this.ID = id;
		
		this.forces = new Point.Double[numWorkers];
		for (int i = 0; i < numWorkers; i++)
			forces[i] = new Point.Double(0, 0);
	}
	
	
	
	/*
	 * getter
	 */
	public double getMass() {
		
		return this.mass;
	}
	
	
	
	/*
	 * setter
	 */
	public void setMass(int mass) {
		
		if (mass < 1)
			mass = 1;
		
		this.mass = mass;
	}
	
	
	
	/*
	 * getter
	 */
	public double getRadius() {
		
		return this.radius;
	}
	
	
	
	/*
	 * setter
	 */
	public void setRadius(double radius) {
		
		if (radius < 1)
			radius = 1;
		
		this.radius = radius;
	}
	
	
	
	/*
	 * getter
	 */
	public Point.Double getVelocity() {
		
		return this.velocity;
	}
	
	
	
	/*
	 * setter
	 */
	public synchronized void setVelocity(Point.Double velocity) {
		
		if (velocity == null)
			velocity = new Point.Double(0, 0);
		
		if (velocity.x == 0)
			velocity.setLocation(0.00001, velocity.y);
		
		if (velocity.y == 0)
			velocity.setLocation(velocity.x, 0.00001);
		
		this.velocity = velocity;
	}
	
	
	
	/*
	 * return relative velocity -- speed
	 */
	public double getSpeed() {
		
		return Math.sqrt((this.velocity.x) * (this.velocity.x)
		        + (this.velocity.y) * (this.velocity.y));
	}
	
	
	
	/*
	 * getter
	 */
	public Point.Double getPosition() {
		
		return this.position;
	}
	
	
	
	/*
	 * setter
	 */
	public synchronized void setPosition(Point.Double position) {
		
		if (position == null)
			position = new Point.Double(0, 0);
		
		this.position = position;
	}
	
	
	
	/*
	 * getter
	 */
	public Point.Double getForce(int row) {
		
		return this.forces[row];
	}
	
	
	
	/*
	 * setter
	 */
	public void setForce(int row, Point.Double force) {
		
		this.forces[row] = force;
	}
	
	
	
	/*
	 * return total force
	 */
	public synchronized Point.Double getNetForce() {
		
		Point.Double total = new Point.Double(0, 0);
		
		for (Point.Double f : forces)
			total.setLocation(total.x + f.x, total.y + f.y);
		
		return total;
	}
	
	
	
	/*
	 * resetter
	 */
	public synchronized void zeroOutForces() {
		
		for (Point.Double f : forces)
			f.setLocation(0, 0);
		
	}
	
	
	
	/*
	 * returns predicted next location
	 */
	private Point.Double getPredictedNext(double fps, int step) {
		
		if (step == lastStep && step != -1) // -1: sentinel value
			return predicted;
		
		lastStep = step;
		
		predicted = new Point.Double(
		        this.position.x + this.velocity.x * 1.5 / fps,
		        this.position.y + this.velocity.y * 1.5 / fps);
		
		return predicted;
	}
	
	
	
	/*
	 * returns true if this and rival intersect
	 */
	public boolean doIntersect(Body rival, double fps, int step) {
		
		final double thisNextX = this.getPredictedNext(fps, step).x;
		final double thisNextY = this.getPredictedNext(fps, step).y;
		final double rivalNextX = rival.getPredictedNext(fps, step).x;
		final double rivalNextY = rival.getPredictedNext(fps, step).y;
		
		final double distCenters = Math
		        .sqrt((thisNextX - rivalNextX) * (thisNextX - rivalNextX)
		                + (thisNextY - rivalNextY) * (thisNextY - rivalNextY));
		
		final double radiiSum = this.radius + rival.radius;
		
		return distCenters - radiiSum <= epsilon;
	}
	
	
	
	/*
	 * getter for GUI
	 */
	public Color getColor() {
		
		return this.color;
	}
	
	
	
	/*
	 * getter for GUI
	 */
	public int getID() {
		
		return this.ID;
	}
	
	
	
	/*
	 * return leftmost x-value of body
	 */
	public int leftBound() {
		
		return (int) (this.getPosition().x - radius);
	}
	
	
	
	/*
	 * return rightmost x-value of body
	 */
	public int rightBound() {
		
		return (int) (this.getPosition().x + radius);
	}
	
	
	
	/*
	 * negate x velocity
	 */
	public synchronized void reverseXVelocity() {
		
		this.setVelocity(new Point.Double(-velocity.x, velocity.y));
	}
	
	
	
	/*
	 * return uppermost y-value of body
	 */
	public int upperBound() {
		
		return (int) (this.getPosition().y - radius);
	}
	
	
	
	/*
	 * return lower-most y-value of body
	 */
	public int lowerBound() {
		
		return (int) (this.getPosition().y + radius);
	}
	
	
	
	/*
	 * negate y velocity
	 */
	public synchronized void reverseYVelocity() {
		
		this.setVelocity(new Point.Double(velocity.x, -velocity.y));
	}
	
}

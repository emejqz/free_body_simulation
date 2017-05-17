
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;

/*
 * @author Eric M Evans
 * 
 * free-body problem. multi-threaded solution. includes main method.
 * for Universe panel, draws bodies.
 */
@SuppressWarnings("serial")
public class Universe extends JPanel implements Observer {
	
	private FreeBodies	model;
	public boolean		showNumbers	= true;
	private boolean		wallsNotSet	= true;
	
	
	
	public Universe(FreeBodies model) {
		this.model = model;
	}
	
	
	
	@Override
	public void paintComponent(Graphics g) {
		
		Graphics2D g2 = (Graphics2D) g;
		
		super.paintComponent(g2);
		
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
		        RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHints(rh);
		
		drawBodies(g2);
	}
	
	
	
	private void drawBodies(Graphics2D g2) {
		
		for (Body body : model.bodies) {
			g2.setColor(body.getColor());
			g2.fillOval((int) (body.getPosition().getX() - body.getRadius()),
			        (int) (body.getPosition().getY() - body.getRadius()),
			        (int) body.getRadius() * 2, (int) body.getRadius() * 2);
		}
		
		if (this.showNumbers) {
			g2.setColor(Color.WHITE);
			for (Body body : model.bodies) {
				g2.setFont(new Font("Courier", Font.BOLD,
				        (int) (10 * (1 + body.getRadius() / 10))));
				g2.drawString("" + body.getID(),
				        (int) (body.getPosition().getX()
				                + body.getRadius() * 0.80),
				        (int) (body.getPosition().getY()
				                - body.getRadius() * 0.80));
			}
		}
		
	}
	
	
	
	@Override
	public void update(Observable arg0, Object arg1) {
		
		if (wallsNotSet) {
			model.setRightWall((int) this.getSize().getWidth());
			model.setBottomWall((int) this.getSize().getHeight());
			wallsNotSet = false;
		}
		
		this.repaint();
	}
	
	/*
	 * @Override
	 * public Dimension getPreferredSize() {
	 * 
	 * return new Dimension(750, 560);
	 * }
	 */
}

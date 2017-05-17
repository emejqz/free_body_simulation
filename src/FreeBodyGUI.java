
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * @author Eric M Evans
 * 
 * GUI for free body simulation
 */
@SuppressWarnings("serial")
public class FreeBodyGUI extends JFrame {
	
	private FreeBodies				model;
	
	// Main JPanel Containers
	private JPanel					mainPanel;
	private Universe				universe;
	private JPanel					controlPanel;
	private JPanel					options;
	private JScrollPane				controlScrollPane;
	
	// Body Control Labels
	private static final String		SPEEDSTR		= "Speed";
	private static final String		XPSTR			= "X-Pos";
	private static final String		YPSTR			= "Y-Pos";
	private static final String		XVSTR			= "X-Vel";
	private static final String		YVSTR			= "Y-Vel";
	private static final String		RSTR			= "Radius";
	private static final String		MSTR			= "Mass";
	private static final String		DELSTR			= "Delete";
	private static final String		format			= "%+.3f";
	
	// Options
	private JCheckBox				optNumbers;
	private JCheckBox				optGravity;
	private JCheckBox				optWalls;
	private JSlider					fps;
	private JSlider					gCon;
	private static final String		STARTSTR		= "Start";
	private static final String		PAUSESTR		= "Pause";
	
	// Body Controls
	private ButtonGroup				groupRadio		= new ButtonGroup();
	private Map<Body, BodyControl>	bodyCtrlMap		= new HashMap<Body, BodyControl>();
	private JPanel					addPanel		= null;
	
	// Body Variables
	private Body					bodySelected	= null;
	private int						bodyCountRaw	= 0;
	
	
	
	public FreeBodyGUI(FreeBodies model) {
		
		this.model = model;
		this.bodyCountRaw = model.bodies.size();
		
		this.setTitle("Free Body Physics Simulation");
		this.setSize(1004, 630);
		this.setLocation(40, 20);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setLayout(null);
		
		this.mainPanel = new JPanel();
		this.mainPanel.setSize(1000, 600);
		this.mainPanel.setLocation(0, 0);
		this.mainPanel.setLayout(new BorderLayout());
		this.add(mainPanel);
		
		this.universe = new Universe(model);
		TitledBorder tbu = BorderFactory.createTitledBorder("Universe");
		tbu.setTitleColor(Color.WHITE);
		this.universe.setBorder(tbu);
		this.universe.setBackground(Color.BLACK);
		this.universe.addMouseListener(new MoveListener());
		mainPanel.add(this.universe, BorderLayout.CENTER);
		model.addObserver((Observer) universe);
		
		this.options = new JPanel();
		TitledBorder tbo = BorderFactory.createTitledBorder("Universe Options");
		tbo.setTitleColor(Color.WHITE);
		this.options.setBorder(tbo);
		// For Adjusting options AND body control size
		this.options.setPreferredSize(new Dimension(100, 225));
		this.options.setBackground(Color.BLACK);
		this.options.setLayout(new GridLayout(0, 1));
		
		this.addOptionButtons();
		
		this.controlPanel = new JPanel();
		TitledBorder tbcp = BorderFactory.createTitledBorder("Controlls");
		tbcp.setTitleColor(Color.WHITE);
		// this.controlPanel.setPreferredSize(new Dimension(0,0));
		this.controlPanel.setBorder(tbcp);
		this.controlPanel.setBackground(Color.BLACK);
		this.controlPanel.setLayout(new GridLayout(0, 2));
		this.controlPanel.add(this.options);
		this.controlScrollPane = new JScrollPane(controlPanel,
		        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.controlScrollPane.setBorder(BorderFactory.createEmptyBorder());
		this.controlScrollPane.setPreferredSize(new Dimension(275, 0));
		mainPanel.add(controlScrollPane, BorderLayout.EAST);
		
		this.addBodyControls();
		
		this.addAddPanel();
		
		this.setVisible(true);
	}
	
	
	
	/*
	 * initialize add panel
	 */
	private void addAddPanel() {
		
		if (addPanel != null) {
			this.controlPanel.remove(addPanel);
		}
		
		addPanel = new JPanel();
		addPanel.setBackground(Color.BLACK);
		addPanel.setLayout(new GridBagLayout());
		this.controlPanel.add(addPanel);
		JButton addBtn = new JButton("+");
		addBtn.setEnabled(true);
		addBtn.setBackground(Color.WHITE);
		addBtn.setFont(new Font("Courier", Font.BOLD, 40));
		addBtn.addActionListener(new AddListener());
		TitledBorder tb = BorderFactory
		        .createTitledBorder("" + (this.bodyCountRaw));
		tb.setTitleJustification(TitledBorder.CENTER);
		tb.setTitleColor(Color.WHITE);
		this.addPanel.setBorder(tb);
		addPanel.add(addBtn);
		
	}
	
	
	
	/*
	 * add various options
	 */
	private void addOptionButtons() {
		
		JButton play = new JButton(STARTSTR);
		play.addActionListener(new StartListener(play));
		this.options.add(play);
		
		this.optNumbers = new JCheckBox("Numbers");
		this.optNumbers.setSelected(true);
		this.optNumbers.addItemListener(new OptionsListener());
		this.optNumbers.setBackground(Color.BLACK);
		this.optNumbers.setForeground(Color.WHITE);
		this.options.add(optNumbers);
		
		this.optWalls = new JCheckBox("Corral");
		this.optWalls.setSelected(model.walls);
		this.optWalls.setBackground(Color.BLACK);
		this.optWalls.setForeground(Color.WHITE);
		this.optWalls.addItemListener(new OptionsListener());
		this.options.add(optWalls);
		
		this.optGravity = new JCheckBox("Gravity");
		this.optGravity.setSelected(model.gravity);
		this.optGravity.setBackground(Color.BLACK);
		this.optGravity.setForeground(Color.WHITE);
		this.optGravity.addItemListener(new OptionsListener());
		this.options.add(optGravity);
		
		String gConStub = "Grav Con: ";
		JLabel gConLabel = new JLabel(gConStub + model.getG());
		gConLabel.setBackground(Color.BLACK);
		gConLabel.setForeground(Color.WHITE);
		gConLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.options.add(gConLabel);
		final int gConMAX = 20000, gConMIN = 5000;
		this.gCon = new JSlider(JSlider.HORIZONTAL, gConMIN, gConMAX,
		        (int) model.getG());
		gCon.setBackground(Color.BLACK);
		gCon.setForeground(Color.WHITE);
		gCon.addChangeListener(new SliderListener(gCon, gConLabel, gConStub));
		this.options.add(gCon);
		Hashtable<Integer, JLabel> gConLabelTable = new Hashtable<>();
		JLabel gConMinLabel = new JLabel("" + gConMIN);
		gConMinLabel.setForeground(Color.WHITE);
		gConLabelTable.put(0, gConMinLabel);
		JLabel gConMaxLabel = new JLabel("" + gConMAX);
		gConMaxLabel.setForeground(Color.WHITE);
		gConLabelTable.put(gConMAX, gConMaxLabel);
		gCon.setLabelTable(gConLabelTable);
		gCon.setPaintLabels(true);
		
		String fpsStub = "Frames/Sec: ";
		JLabel fpsLabel = new JLabel(fpsStub + model.getFPS());
		fpsLabel.setBackground(Color.BLACK);
		fpsLabel.setForeground(Color.WHITE);
		fpsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.options.add(fpsLabel);
		final int fpsMAX = 1000, fpsMIN = 50;
		this.fps = new JSlider(JSlider.HORIZONTAL, fpsMIN, fpsMAX,
		        model.getFPS());
		fps.setBackground(Color.BLACK);
		fps.setForeground(Color.WHITE);
		fps.addChangeListener(new SliderListener(fps, fpsLabel, fpsStub));
		this.options.add(fps);
		Hashtable<Integer, JLabel> fpsLabelTable = new Hashtable<>();
		JLabel fpsMinLabel = new JLabel("" + fpsMIN);
		fpsMinLabel.setForeground(Color.WHITE);
		fpsLabelTable.put(0, fpsMinLabel);
		JLabel fpsMaxLabel = new JLabel("" + fpsMAX);
		fpsMaxLabel.setForeground(Color.WHITE);
		fpsLabelTable.put(fpsMAX, fpsMaxLabel);
		fps.setLabelTable(fpsLabelTable);
		fps.setPaintLabels(true);
		
	}
	
	
	
	/*
	 * add control panel for each body
	 */
	private void addBodyControls() {
		
		for (Body body : model.bodies) {
			BodyControl ctrl = new BodyControl(body.getID(), body);
			this.bodyCtrlMap.put(body, ctrl);
			// System.out.println(bodyCtrlMap.size());
			this.controlPanel.add(ctrl);
		}
		
	}
	
	
	
	/*
	 * for displaying and changing body data values
	 */
	private class BodyControl extends JPanel implements Observer {
		
		private Body		body;
		private JTextField	massField;
		private JTextField	radiusField;
		private JTextField	velXField;
		private JTextField	velYField;
		private JTextField	speedField;
		private JTextField	xField;
		private JTextField	yField;
		
		
		
		public BodyControl(int id, Body body) {
			
			super();
			
			this.body = body;
			
			model.addObserver(this);
			FreeBodyGUI.this.validate();
			FreeBodyGUI.this.controlPanel.validate();
			FreeBodyGUI.this.controlScrollPane.validate();
			
			this.setSize(230, 100);
			this.setBackground(body.getColor());
			
			TitledBorder title = BorderFactory.createTitledBorder("" + id);
			title.setTitleJustification(TitledBorder.CENTER);
			this.setBorder(title);
			this.setLayout(new SpringLayout());
			
			final int ALIGN = JLabel.LEFT;
			final int TXTSZ = 5;
			
			JLabel btnTxt = new JLabel("Move");
			this.add(btnTxt);
			JRadioButton btn = new JRadioButton();
			btn.setHorizontalAlignment(SwingConstants.CENTER);
			btn.setBackground(body.getColor());
			btn.addActionListener(new RadioListener(body));
			FreeBodyGUI.this.groupRadio.add(btn);
			this.add(btn);
			
			JLabel mass = new JLabel(MSTR, ALIGN);
			this.add(mass);
			this.massField = new JTextField(TXTSZ);
			massField.setText(String.format(format, body.getMass()));
			mass.setLabelFor(massField);
			this.add(massField);
			massField.addActionListener(
			        new ControlListener(body, MSTR, massField));
			massField.addFocusListener(
			        new ControlListener(body, MSTR, massField));
			
			JLabel radius = new JLabel(RSTR, ALIGN);
			this.add(radius);
			this.radiusField = new JTextField(TXTSZ);
			radiusField.setText(String.format(format, body.getRadius()));
			radius.setLabelFor(radiusField);
			this.add(radiusField);
			radiusField.addActionListener(
			        new ControlListener(body, RSTR, radiusField));
			radiusField.addFocusListener(
			        new ControlListener(body, RSTR, radiusField));
			
			JLabel velX = new JLabel(XVSTR, ALIGN);
			this.add(velX);
			this.velXField = new JTextField(TXTSZ);
			velXField.setText(String.format(format, body.getVelocity().getX()));
			velX.setLabelFor(velXField);
			this.add(velXField);
			velXField.addActionListener(
			        new ControlListener(body, XVSTR, velXField));
			velXField.addFocusListener(
			        new ControlListener(body, XVSTR, velXField));
			
			JLabel velY = new JLabel(YVSTR, ALIGN);
			this.add(velY);
			this.velYField = new JTextField(TXTSZ);
			velYField.setText(String.format(format, body.getVelocity().getY()));
			velY.setLabelFor(velYField);
			this.add(velYField);
			velYField.addActionListener(
			        new ControlListener(body, YVSTR, velYField));
			velYField.addFocusListener(
			        new ControlListener(body, YVSTR, velYField));
			
			JLabel speed = new JLabel(SPEEDSTR, ALIGN);
			this.add(speed);
			this.speedField = new JTextField(TXTSZ);
			speedField.setText(String.format(format, body.getSpeed()));
			speed.setLabelFor(speedField);
			this.add(speedField);
			speedField.addActionListener(
			        new ControlListener(body, SPEEDSTR, speedField));
			speedField.addFocusListener(
			        new ControlListener(body, SPEEDSTR, speedField));
			
			JLabel x = new JLabel(XPSTR, ALIGN);
			this.add(x);
			this.xField = new JTextField(TXTSZ);
			xField.setText(String.format(format, body.getPosition().getX()));
			x.setLabelFor(xField);
			this.add(xField);
			xField.addActionListener(new ControlListener(body, XPSTR, xField));
			xField.addFocusListener(new ControlListener(body, XPSTR, xField));
			
			JLabel y = new JLabel(YPSTR, ALIGN);
			this.add(y);
			this.yField = new JTextField(TXTSZ);
			yField.setText(String.format(format, body.getPosition().getY()));
			y.setLabelFor(yField);
			this.add(yField);
			yField.addActionListener(new ControlListener(body, YPSTR, yField));
			yField.addFocusListener(new ControlListener(body, YPSTR, yField));
			
			JLabel delLabel = new JLabel("");
			this.add(delLabel);
			JButton delete = new JButton(DELSTR);
			delete.setEnabled(true);
			this.add(delete);
			delete.addActionListener(new ControlListener(body, DELSTR, null));
			
			SpringUtilities.makeCompactGrid(this, 9, 2, 1, 1, 1, 1);
			// rows, columns, initX, initY, xPad, yPad
		}
		
		
		
		@Override
		public void update(Observable o, Object arg) {
			
			massField.setText(String.format(format, body.getMass()));
			radiusField.setText(String.format(format, body.getRadius()));
			velXField.setText(String.format(format, body.getVelocity().getX()));
			velYField.setText(String.format(format, body.getVelocity().getY()));
			speedField.setText(String.format(format, body.getSpeed()));
			xField.setText(String.format(format, body.getPosition().getX()));
			yField.setText(String.format(format, body.getPosition().getY()));
			
			final boolean editable = !model.isPlaying();
			massField.setEditable(editable);
			radiusField.setEditable(editable);
			velXField.setEditable(editable);
			velYField.setEditable(editable);
			speedField.setEditable(editable);
			xField.setEditable(editable);
			yField.setEditable(editable);
			
		}
	}
	
	
	
	/*
	 * for editing universe slider options
	 */
	private class SliderListener implements ChangeListener {
		
		private JSlider	slider;
		private JLabel	label;
		private String	stub;
		
		
		
		public SliderListener(JSlider slider, JLabel label, String stub) {
			this.slider = slider;
			this.label = label;
			this.stub = stub;
		}
		
		
		
		@Override
		public void stateChanged(ChangeEvent e) {
			
			JSlider source = (JSlider) e.getSource();
			// if (source.getValueIsAdjusting())
			// return;
			
			int val = source.getValue();
			
			if (slider.equals(fps)) {
				if (model.isPlaying()) {
					fps.setValue(model.getFPS());
				}
				else {
					label.setText(stub + val);
					model.setFPS(val);
				}
			}
			else if (slider.equals(gCon)) {
				if (model.isPlaying()) {
					gCon.setValue((int) model.getG());
				}
				else {
					label.setText(stub + val);
					model.setG(val);
				}
			}
			
		}
		
	}
	
	
	
	/*
	 * for adding more bodies
	 */
	private class AddListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if (model.isPlaying())
				return;
			
			Body newBody = new Body(10, 25.0, new Point.Double(),
			        new Point.Double(), bodyCountRaw, model.numWorkers);
			
			if (model.placeRandomly(newBody, model.bodies) == false) {
				System.out.println("no available room for new body");
				return;
			}
			else
				bodyCountRaw++;
			
			model.addBody(newBody);
			
			BodyControl ctrl = new BodyControl(newBody.getID(), newBody);
			bodyCtrlMap.put(newBody, ctrl);
			controlPanel.add(ctrl);
			
			addAddPanel();
			
			controlScrollPane.validate();
		}
		
	}
	
	
	
	/*
	 * for moving selected body
	 */
	private class MoveListener implements MouseListener {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			
			if (bodySelected == null || model.isPlaying())
				return;
			
			double x = e.getX();
			double y = e.getY();
			
			Body temp = new Body(0, bodySelected.getRadius(), null,
			        new Point.Double(x, y), -1, 1);
			
			for (Body rival : model.bodies) {
				if (rival.equals(bodySelected))
					continue;
				
				if (temp.doIntersect(rival, model.getFPS(), -1))
					return;
			}
			
			bodySelected.setPosition(new Point.Double(x, y));
			
			model.updateObservers();
		}
		
		
		
		@Override
		public void mouseClicked(MouseEvent e) {
			
		}
		
		
		
		@Override
		public void mouseEntered(MouseEvent e) {
			
		}
		
		
		
		@Override
		public void mouseExited(MouseEvent arg0) {
			
		}
		
		
		
		@Override
		public void mousePressed(MouseEvent e) {
			
		}
		
	}
	
	
	
	/*
	 * for selecting bodies
	 */
	private class RadioListener implements ActionListener {
		
		private Body body;
		
		
		
		public RadioListener(Body body) {
			this.body = body;
		}
		
		
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			bodySelected = body;
		}
		
	}
	
	
	
	/*
	 * listens for the play/pause button
	 */
	private class StartListener implements ActionListener {
		
		private JButton button;
		
		
		
		public StartListener(JButton button) {
			this.button = button;
		}
		
		
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			if (button.getText().equals(STARTSTR)) {
				button.setText(PAUSESTR);
				FreeBodyGUI.this.groupRadio.clearSelection();
				FreeBodyGUI.this.bodySelected = null;
				model.setPlay(true);
			}
			else {
				button.setText(STARTSTR);
				model.setPlay(false);
			}
			
		}
		
	}
	
	
	
	/*
	 * for various universe options
	 */
	private class OptionsListener implements ItemListener {
		
		@Override
		public void itemStateChanged(ItemEvent e) {
			
			JCheckBox box = (JCheckBox) e.getItemSelectable();
			
			if (box.equals(optNumbers)) {
				universe.showNumbers = !universe.showNumbers;
			}
			else if (box.equals(optGravity)) {
				if (model.isPlaying())
					optGravity.setSelected(model.gravity);
				else
					model.gravity = !model.gravity;
			}
			else if (box.equals(optWalls)) {
				if (model.isPlaying())
					optWalls.setSelected(model.walls);
				else
					model.walls = !model.walls;
			}
			
			model.updateObservers();
		}
		
	}
	
	
	
	/*
	 * for inputing values to bodies
	 */
	private class ControlListener implements ActionListener, FocusListener {
		
		private Body		body;
		private String		name;
		private JTextField	textField;
		
		
		
		public ControlListener(Body body, String name, JTextField textField) {
			this.body = body;
			this.name = name;
			this.textField = textField;
		}
		
		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			go();
		}
		
		
		
		@Override
		public void focusLost(FocusEvent arg0) {
			
			go();
		}
		
		
		
		private void go() {
			
			double val = 0.0;
			
			// if there is a textField, get data value
			if (textField != null) {
				try {
					val = Double.parseDouble(textField.getText());
				}
				catch (NumberFormatException e1) {
					val = 0.0;
				}
			}
			
			switch (name) {
				case SPEEDSTR:
					double ratio = val / body.getSpeed();
					body.setVelocity(
					        new Point.Double(body.getVelocity().x * ratio,
					                body.getVelocity().y * ratio));
					break;
				
				case XPSTR:
					body.setPosition(
					        new Point.Double(val, body.getPosition().getY()));
					break;
				
				case YPSTR:
					body.setPosition(
					        new Point.Double(body.getPosition().getX(), val));
					break;
				
				case XVSTR:
					body.setVelocity(
					        new Point.Double(val, body.getVelocity().getY()));
					break;
				
				case YVSTR:
					body.setVelocity(
					        new Point.Double(body.getVelocity().getX(), val));
					break;
				
				case RSTR:
					body.setRadius(val);
					break;
				
				case MSTR:
					body.setMass((int) val);
					break;
				
				case DELSTR:
					if (model.isPlaying())
						return;
					BodyControl ctrl = bodyCtrlMap.get(body);
					controlPanel.remove(ctrl);
					model.bodies.remove(body);
					controlScrollPane.validate();
					break;
				default:
					System.out.println("Error in Control Listener");
					System.exit(1);
			}
			
			model.updateObservers();
		}
		
		
		
		@Override
		public void focusGained(FocusEvent arg0) {
			
		}
		
	}
}

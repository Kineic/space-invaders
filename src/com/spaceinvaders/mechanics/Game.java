package com.spaceinvaders.mechanics;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.w3c.dom.Entity;

public class Game extends Canvas {
	//The strategy that allows us to use accelerate page flipping.
	private BufferStrategy strategy;
	//True if the game is currently "running", ie. the game loop is looping.
	private boolean gameRunning = true;
	//The list of all the entities that exist in our game.
	private ArrayList entities;
	//The list of entities that need to be removed in this loop.
	private ArrayList removeList = new ArrayList();
	//The entity representing the player.
	private Entity ship;
	//The speed in which the players ship should move (pixels/sec).
	private double moveSpeed = 300;
	//The time at which last fired shot.
	private long lastFire = 0;
	//The interval between our players shot (ms).
	private long firingInterval = 500;
	//The number of aliens left on the screen.
	private int alienCount;
	
	//The message to display while waiting for a key press
	private String message = "";
	//True if we are holding up game play until a key is pressed.
	private boolean waitingForKeyPress = true;
	//True if the left cursor key is currently pressed.
	private boolean leftPressed = false;
	//True if the right cursor is currently pressed.
	private boolean rightPressed = false;
	//True if we are firing.
	private boolean firePressed = false;
	//True if game logic needs to be applied this loop, normally as a result of a game event.
	private boolean logicRequiredThisLoop = false;
	
	//Construct our game and set it to running.
	
	public Game() {
		//Create a JFrame to contain our game.
		
		JFrame container = new JFrame("Space Invaders Boiiiis");
		
		//Get hold the content of the frame and set up the resolution of the game.
		
		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(800, 600));
		panel.setLayout(null);
		
		//Setup our canvas size and put it into the content frame.
		
		setBounds(0, 0, 800, 600);
		panel.add(this);
		
		//Tell AWT not to bother repainting our canvas since we're going to do it ourselves in accelerated mode
		
		setIgnoreRepaint(true);
	
		//Finally make the window visible.
		
		container.pack();
		container.setResizable(false);
		container.setVisible(true);
		
		//Add a listener to respond to the suer closing the window. If they do, exit the game.
		
		container.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		//Add a key input system (defined below) to our canvas, so we can respond to key pressed.
		
		addKeyListener(new KeyInputHandler());
		
		//Request the focus so key events come to us.
		
		requestFocus();
		
		//Create the buffering strategy which will allow AWT to manage our accelerated graphics.
		
		createBufferStrategy(2);
		strategy = getBufferStrategy();
		
		//Initialise the entities in our game so there's something to see at startup.
		
		initEntities();	
	}
	
	//Start a fresh game, this should clear out any old data and create a new set.
	
	private void startGame() {
		//Clear out any existing entities and initialise a new set
		
		entities.clear();
		initEntities();
		
		//Blank out any keyboard settings we might currently have.
		
		leftPressed = false;
		rightPressed = false;
		firePressed = false;
	}
	
	//Initialise the starting state of the entities (ship and aliens). Each entity will
	//be added to the overall list of entities in the game.
	
	private void initEntities() {
		//Create the player ship and place it roughly in the centre of the screen.
		
		ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
		entities.add(ship);
		
		//Create a block of aliens (5 rows, by 12 aliens, spaced evenly).
		
		alienCount = 0;
		for (int row = 0; row < 5; row++) {
			for (int i = 0; i < 12; i++) {
				Entity alien = new AlienEntity(this, "sprites/alien.gif", 100 + (i * 50), (50) + row * 30);
				entities.add(alien);
				alienCount++;
			}
		}
	}
	
	/*Notification from a game entity that the logic of the game should be run
	 *at the next opportunity (normally as a result of some game event).*/
	
	public void updateLogic() {
		logicRequiredThisLoop = true;
	}
	
	/*Remove an entity from the game. The entity removed will no longer move or be
	 * drawn.
	 * @param entity The entity that should be removed.
	 */
	
	public void removeEntity(Entity entity) {
		removeList.add(entity);
	}
	
	//Notification that the player has died.
	
	public void notifyDeath() {
		message = "Whoops, looks like you died, try again?";
		waitingForKeyPress = true;
	}
	
	//Notification that the player has won.
	
	public void notifyWin() {
		message = "Congrats, you killed em all!";
		waitingForKeyPress = true;
	}
	
	//Notification that an alien has been killed.
	
	public void notifyAlienKilled() {
		//Reduce the alien count, if there are none left, the player has won.
		
		alienCount--;
		
		if(alienCount == 0) {
			notifyWin();
		}
		
		//If there are still some aliens left, then they all need to speed up.
		
		for (int i = 0; i < entities.size(); i++) {
			Entity entity = (Entity) entities.get(i);
			
			if (entity instanceof AlienEntity) {
				//Speed up by 2%.
				
				entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
			}
		}
	}
	
	/*Attempt to fire a shot from the player. It's called "try" since we must first check
	 * that the player can fire at this point, ie. have they waited long enough since the
	 * previous shot?*/
	
	public void tryToFire() {
		//Check that we have waited long enough to fire.
		
		if (System.currentTimeMillis() - lastFire < firingInterval) {
			return;
		}
		
		//If we have waited long enough, create the shot entity, and record the time.
		
		lastFire = System.currentTimeMillis();
		ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 10, ship.geyY() - 30);
		entities.add(shot);
	}
	
	/*The main game loop. This loop is running during all game play as it's responsible
	 *for the following activities:
	 * - Working out the speed of the game loop to update moves,
	 * - Moving the game entities,
	 * - Drawing the screen contents (entities, text),
	 * - Updating game events,
	 * - Checking Input.
	 * */
	
	public void gameLoop() {
		long lastLoopTime = System.currentTimeMillis();
		
		//Keep looping round until the game ends.
		
		while (gameRunning) {
			
			//Work out how long its been since the last update, this will be used
			//to calculate how far the entities should move this loop
			
			long delta  = System.currentTimeMillis() - lastLoopTime;
			lastLoopTime = System.currentTimeMillis();
			
			//Get hold of a graphics context for the accelerated surface and blank it out
			
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, 800, 600);
			
			//Cycle round asking each entity to move itself
			
			if (!waitingForKeyPress) {
				for (int i  = 0; i < entities.size(); i++) {
					Entity entity = (Entity) entities.get(i);
					
					entity.move(delta);
				}
			}
			
			//Cycle round drawing all the entities we have in the game.
			
			for (int i = 0; i < entities.size(); i++) {
				Entity entity = (Entity) entities.get(i);
				
				entity.draw(g);
			}
			
			//Brute force collisions, compare every entity against every other entity. If
			//any of them collide notify both entities that the collision has occurred.
			
			for (int p = 0; p < entities.size(); p++) {
				for (int s = p + 1; s < entities.size(); s++) {
					Entity me = (Entity) entities.get(p);
					Entity him = (Entity) entities.get(s);
					
					if (me.collidesWith(him)) {
						me.collidedWith(him);
						him.collidedWith(me);
					}
				}
			}
			
			//Remove any entity that has been marked for clear up.
			
			entities.removeAll(removeList);
			removeList.clear();
			
			//If a game event has indicated that the game logic should be resolved, cycle
			//round every entity requesting that their personal logic should be considered.
			
			if (logicRequiredThisLoop) {
				for (int i = 0; i < entities.size(); i++) {
					Entity entity = (Entity) entities.get(i);
					entity.doLogic();
				}
				
				logicRequiredThisLoop = false;
			}
			
			//Finally, we've completed drawing so clear up the graphics and flip the buffer over.
			
			g.dispose();
			strategy.show();
			
			//Resolve the movement of the ship. First assume the ship isn't moving. If either
			//cursor key is pressed then update the movement appropriately.
			
			ship.setHorizontalMovement(0);
			
			if ((leftPressed) && (!rightPressed)) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if ((rightPressed) && (!leftPressed)) {
				ship.setHorizontalMovement(moveSpeed);
			}
			
			//If we're pressing fire, attempt to fire
			
			if (firePressed) {
				tryToFire();
			}
			
			//Finally, pause for a bit. Note: this should run us at about 100 fps but on
			//windows this might vary on each loop due to a bad implementation timer.
			
			try {
				Thread.sleep(10);
			} catch (Exception e) {}	
		}	
	}
	
	/*
	 * A class to handle keyboard input from the user. The class handles both dynamic input
	 * during game play, ie. left/right and shoot, and more static type inputs (ie. press
	 * any key to continue)
	 * 
	 * This has been implemented as an inner class more through habit than anything else.
	 * It's perfectly normal to implement this as a separate Class if slightly less convenient.
	 */
	
	private class KeyInputHandler extends KeyAdapter {
		//The number of key presses we've had while waiting for an "any key" press
		
		private int pressCount = 1;
		
		/*
		 * Notification from AWT what a key has been pressed. Note that a key being pressed
		 * is equal to being pushed down but NOT released. That's where keyTyped() comes in.
		 */
		
		public void keyPressed(KeyEvent e) {
			//If we are waiting for an "any key" typed then we don't want to do anything
			//with just a "press".
			
			if (waitingForKeyPress) {
				return;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = true;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = true;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = true;
			}
		}
		
		/*
		 * Notification from AWT that a key has been released.
		 */
		
		public void keyReleased(KeyEvent e) {
			//If we're waiting for an "any key" typed then we don't want to do anything
			//with just a "released"
			
			if (waitingForKeyPress) {
				return;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = false;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = false;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = false;
			}
		}
		
		/*
		 * Notification from AWT that a key has been typed. Note that typing
		 * a key means to both press and then release it.
		 */
		
		public void keyTyped(KeyEvent e) {
			//If we're waiting for an "any key" type then check if we've received
			//any recently. We may have had a keyType() event from the user releasing
			//the shoot or move keys, hence the use of the "pressCount".
			
			if (waitingForKeyPress) {
				if (pressCount == 1) {
					//Since we've now received  our key typed event we can mark it as such
					//and start our new game.
					
					waitingForKeyPress = false;
					startGame();
					pressCount = 0;
				} else {
					pressCount++;
				}
			}
			
			//If we hit escape, then quit the game
			
			if (e.getKeyChar() == 27) {
				System.exit(0);
			}
		}
	}
	
	/*
	 * The entry point into the game. We'll simply create an instance of a Class which will
	 * start the display and game loop.
	 */
	
	public static void main(String[] args) {
		Game g = new Game();
		
		//Start the main game loop, note: this method will not return until the game has
		//finished running. Hence we are using the actual main thread to run the game.
		
		g.gameLoop();
	}
}

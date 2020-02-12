package com.spaceinvaders.mechanics;

public class GameStart extends Game {

	/*
	 * The entry point into the game. We'll simply create an instance of a Class which will
	 * start the display and game loop.
	 */
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Game g = new Game();
		
		//Start the main game loop, note: this method will not return until the game has
		//finished running. Hence we are using the actual main thread to run the game.
		g.gameLoop();
	}
}

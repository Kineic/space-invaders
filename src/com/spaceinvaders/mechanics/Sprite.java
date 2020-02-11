package com.spaceinvaders.mechanics;

import java.awt.Graphics;
import java.awt.Image;

/*
 * A sprite to be displayed on the screen. Note that a sprite contains no state information
 * , ie. it's just the image and not the location. This allows us to use a single sprite in
 * lots of different places without having to store multiple copies of the image.
 */

public class Sprite {
	//The image to be drawn for this sprite
	
	private Image image;
	
	//Create a new sprite based on an image.
	
	public Sprite(Image image) {
		this.image = image;
	}
	
	//Get the width of the drawn sprite.
	
	public int getWidth() {
		return image.getWidth(null);
	}
	
	//Get the height of the drawn sprite.
	
	public int getHeight() {
		return image.getHeight(null);
	}
	
	//Draw the sprite onto the graphics context provided.
	
	public void draw(Graphics g, int x, int y) {
		g.drawImage(image, x, y, null);
	}
}

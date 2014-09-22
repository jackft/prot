/**
 * @author Jack Terwilliger
 * @date 2/4/2014
 */

package PROT;

import java.util.Hashtable;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class MazeView extends Group {

	private int pixelsPerSquare;
	private Maze maze;
	
	//for drawing the maze with colored tiles
	private Hashtable<Character, Color> colormap;
	
	private Image stripes = new Image("PROT\\stripes.jpg");
    private Image balkcom = new Image("PROT\\BalkcomBot.png");
	
	int currentColor;
	
	public MazeView(Maze m, int pixelsPerSquare) {
		currentColor = 0;
		
		//for daring the maze with colored tiles
		colormap = new Hashtable<Character, Color>();
		colormap.put('r', Color.web("rgba(100%,20%,20%,.3)"));
		colormap.put('g', Color.web("rgba(0%,100%,20%,.3)"));
		colormap.put('b', Color.web("rgba(0%,20%,100%,.3)"));
		colormap.put('y', Color.web("rgba(100%,100%,20%,.3)"));
		
		maze = m;
		this.pixelsPerSquare = pixelsPerSquare;

		//draw maze
		for (int c = 0; c < maze.width; c++) {
			for (int r = 0; r < maze.height; r++) {

				int x = c * pixelsPerSquare;
				int y = (maze.height - r - 1) * pixelsPerSquare;

				Rectangle square = new Rectangle(x, y, pixelsPerSquare,
						pixelsPerSquare);

				square.setStroke(Color.GRAY);
				if(colormap.containsKey(maze.getChar(c, r))) {
					square.setFill(colormap.get(maze.getChar(c,r)));
				} else {
					square.setFill(new ImagePattern(stripes, 0, 0, 1, 1, true));
				}
				

				//Text t = new Text(x, y + 12, "" + Chess.colToChar(c)
					//	+ Chess.rowToChar(r));

				this.getChildren().add(square);
				//this.getChildren().add(t);

		
			}
		
		}

		

	}

	//For Drawing the Agent
	private int squareCenterX(int c) {
		return c * pixelsPerSquare + pixelsPerSquare / 2;
		
	}
	private int squareCenterY(int r) {
		return (maze.height - r) * pixelsPerSquare - pixelsPerSquare / 2;
	}
	
	//For Drawing probability bars
	private int squareCornerY(int r, double prob){
		return ((maze.height - r -1) * pixelsPerSquare);
	}
	
	private int squareCornerX(int c){
		return c * pixelsPerSquare;
	}

	
	// create a new piece on the board.
	//  return the piece as a Node for use in animations
	public Node addPiece(int c, int r) {
		
		int radius = (int)(pixelsPerSquare * .4);

		Circle piece = new Circle(squareCenterX(c), squareCenterY(r), radius);
		piece.setFill(new ImagePattern(balkcom, 0, 0, 1, 1, true));
		
		this.getChildren().add(piece);
		return piece;
		
	}
	
	
	public Rectangle addState(int c, int r, double probability) {
		
		//probability = 0;
		Rectangle prob = new Rectangle(squareCornerX(c), squareCornerY(r, probability), pixelsPerSquare/5, pixelsPerSquare*probability);
		prob.setFill(Color.web("rgba(0,0,0,1.0)"));
		
		this.getChildren().add(prob);
		return prob;
		
	}
	
	public void updateProbability(Rectangle rect, double probability){
		rect.setHeight(pixelsPerSquare*probability);
	}

}

/**
 * @author Jack Terwilliger
 * @date 2/4/2014
 * 
 * Edited from a provided file written by Devin Balkcom
 */

package PROT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.la4j.vector.Vector;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SimpleMazeDriver extends Application {

	Maze maze;
	
	// instance variables used for graphical display
	private static final int PIXELS_PER_SQUARE = 50;
	MazeView mazeView;
	List<AnimationPath> animationPathList;
	
	// some basic initialization of the graphics; needs to be done before 
	//  runSearches, so that the mazeView is available
	private void initMazeView() {
		maze = Maze.readFromFile("4x4WallMaze.maz");
		
		animationPathList = new ArrayList<AnimationPath>();
		// build the board
		mazeView = new MazeView(maze, PIXELS_PER_SQUARE);
		
	}
	
	// assumes maze and mazeView instance variables are already available
	private void runSearches() {
		
		SensorRobot robot = new SensorRobot(maze);

		int[][] robotPath = robot.generateRandomMoves(20);
		int[] obs = robot.generateObservations(robotPath, true);

		ArrayList<Vector>states = robot.smoothing(obs);
		
		animationPathList.add(new AnimationPath(mazeView, robotPath, states));

	}


	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		
		initMazeView();
	
		primaryStage.setTitle("CS 76 Sensor Robot");

		StackPane root = new StackPane();
		root.getChildren().add(mazeView);
		primaryStage.setScene(new Scene(root));

		primaryStage.show();

		runSearches();

		Timeline timeline = new Timeline(1.0);
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.getKeyFrames().add(
				new KeyFrame(Duration.seconds(.05), new GameHandler()));
		timeline.playFromStart();

	}

	// every frame, this method gets called and tries to do the next move
	//  for each animationPath.
	private class GameHandler implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent e) {
			// System.out.println("timer fired");
			for (AnimationPath animationPath : animationPathList) {
				// note:  animationPath.doNextMove() does nothing if the
				//  previous animation is not complete.  If previous is complete,
				//  then a new animation of a piece is started.
				animationPath.doNextMove();
			}
		}
	}

	// each animation path needs to keep track of some information:
	// the underlying search path, the "piece" object used for animation,
	// etc.
	private class AnimationPath {
		private Node piece;
		ArrayList<Vector> probs;
		ArrayList<Rectangle> beliefPieces;
		private int[][] truePath;
		private int currentMove = 0;
	
		private int lastX;
		private int lastY;
	
		boolean animationDone = true;
	
		public AnimationPath(MazeView mazeView, int[][] path, ArrayList<Vector> st) {
			probs = st;
			beliefPieces = new ArrayList<Rectangle>();
			
			truePath = path;
			piece = mazeView.addPiece(truePath[0][0], truePath[0][1]);
			lastX = truePath[0][0];
			lastY = truePath[0][1];
			
			int j=0;
			for (int i=0; i<maze.size(); i++){
				if (maze.isLegal(i)){
					beliefPieces.add(mazeView.addState(i%maze.width, i/maze.width, st.get(0).get(j)));
					j++;
				}
			}
			
		}

		// try to do the next step of the animation. Do nothing if
		// the mazeView is not ready for another step.
		public void doNextMove() {
		
			// animationDone is an instance variable that is updated
			//  using a callback triggered when the current animation
			//  is complete
			if (currentMove < truePath.length && animationDone) {
				int dx = truePath[currentMove][0] - lastX;
				int dy = truePath[currentMove][1] - lastY;
				animateMove(piece, dx, dy);
				lastX = truePath[currentMove][0];
				lastY = truePath[currentMove][1];
				
				//update Probability bars
				Vector timeSlice = probs.get(currentMove + 1);
				int i=0;
				for (Rectangle beliefPiece:beliefPieces){
					mazeView.updateProbability(beliefPiece, timeSlice.get(i));
					i++;
				}
				currentMove++;
			}
		
		}

		// move the piece n by dx, dy cells
		public void animateMove(Node n, int dx, int dy) {
			animationDone = false;
			TranslateTransition tt = new TranslateTransition(
					Duration.millis(300), n);
			tt.setByX(PIXELS_PER_SQUARE * dx);
			tt.setByY(-PIXELS_PER_SQUARE * dy);
			// set a callback to trigger when animation is finished
			tt.setOnFinished(new AnimationFinished());

			tt.play();

		}

		// when the animation is finished, set an instance variable flag
		//  that is used to see if the path is ready for the next step in the
		//  animation
		private class AnimationFinished implements EventHandler<ActionEvent> {
			@Override
			public void handle(ActionEvent event) {
				animationDone = true;
			}
		}
	}
}
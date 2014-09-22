/**
 * @author Jack Terwilliger
 * @date 02/5/14
 * 
 * SensorRobot implements ProbabilisticReasoningAgent
 * 
 * creates transition and observation models. Matrices and Vectors are implemented with the la4j library
 */

package PROT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;

public class SensorRobot extends ProbabilisticReasoningAgent{

	//the set of robot moves
	private static int[][] MOVES = {{1,0},{0,1},{-1,0},{0,-1}};
	
	//the set of possible colors
	private final char[] COLORS = {'r', 'g', 'b', 'y'};
	
	//the max numbers of neighbors per location
	private final double NUM_NEIGHBORS = 4;
	
	//the sensor error rate
	private double error_rate;
	
	private Maze maze; //the robot knows the layout of the maze
	
	public int[] variables; //the list of variables and their locations in the maze
	
	//construct the HMM based off the maze
	public SensorRobot(Maze m){
		
		maze = m;
		
		error_rate = .12;
		
		//Construct the models from the maze
		setVariables();
		setT(transitionModel());
		setO(observationModel());
		setState(variables.length);
	}
	
	//set the rate of sensor error
	public void setErrorRate(double e){
		error_rate = e;
	}

	//create the transition model from an ascii representation of the maze
	//Each entry is: Tij = P(Xt = j | Xt-1 = i)
	//
	//SxS where S is the number of variables
	//   X1        X2        X3
	//X1[P(x1|x1)][P(x1|x2)][P(x1|x2)]...
	//
	//
	//X2[P(x2|x1)][P(x2|x2)][P(x1|x2)]...
	//
	//
	//X3[P(x3|x1)][P(x3|x2)][P(x1|x2)]...
	//
	//
	//X4[P(x4|x1)][P(x4|x2)][P(x1|x2)]...
	//...
	private Matrix transitionModel(){
		
		//Key = set of locations Value = set of neighbors
		Hashtable<Integer, HashSet<Integer>> topography = getTopography();
	
		//size is number_of_variables x number_of_variables
		//i = Xt-1, j = Xt
		double[][] transition_probabilities = new double[variables.length][];
	
		//get P(X_t|X_t-1) for every variable pair
		int i=0;
		for (int variable: variables){
			transition_probabilities[i] = new double[variables.length];
			
			int j=0;
			for (int pastVariable: variables){
				
				//get P(X_t|X_t-1)
				//put it in the matrix
				transition_probabilities[i][j] = getTransitionProbability(pastVariable, variable, topography);
				j++;
			}
			i++;
		}
		return new Basic2DMatrix(transition_probabilities);
	}
	
	//build the observation model
	//a list of matrices
	//each matrix contains the probabilities that a state would emit the given observation variable
	//the matrices are diagonal so that we can do matrix multiplication with the transition model
	//SxS where S is the number of variables
	//   X1  X2  X3  X4
	// X1[p1][0 ][0 ][0 ]
	// X2[0 ][p2][0 ][0 ]
	// X3[0 ][0 ][p3][0 ]
	// X4[0 ][0 ][0 ][p4]
	private Matrix[] observationModel(){
		Matrix[] obs_mod = new Matrix[COLORS.length];
		
		
		int index=0;
		for (char color : COLORS){
			double[][] obs_matrix = new double[variables.length][];
			int i=0;
			for (int variable:variables){
				
				obs_matrix[i] = new double[variables.length]; //create an array full of 0s
				
				//if its a true reading
				if (maze.getChar(variable) == color){
					obs_matrix[i][i] = 1 - error_rate;
				}
				
				//if its an error
				else{
					obs_matrix[i][i] = error_rate/(COLORS.length-1);
				}
				i++;
				
			}
			obs_mod[index] = new Basic2DMatrix(obs_matrix);
			index++;
		}
		return obs_mod;
	}

	//Set the variables as all and only legal locations in the maze
	//by reducing the variables to only legal locations, the matrix size,
	//and therefore runtime of the dynamical programming algorithms is reduced
	private void setVariables(){
		int number_of_variables = maze.legalCoutn();

		variables = new int[number_of_variables];

		int i =0;
		for(int location=0; location< maze.size(); location++){
			if (maze.isLegal(location)){
				variables[i] = location;
				i++;
			}
		}
	}
	
	//get the topography of the maze world
	private Hashtable<Integer, HashSet<Integer>> getTopography(){
		//Key = set of locations Value = set of neighbors
		Hashtable<Integer, HashSet<Integer>> topography = new Hashtable<Integer, HashSet<Integer>>();
		
		//create a representation of the topography of the maze
		for (int location=0 ; location<maze.size(); location++){
			topography.put(location, getNeighbors(location));
		}

		return topography;
	}
	
	//get the neighboring states
	//remaining in the same location is not overtly stored
	//there will always be 4 covert paths, we only store paths to other location
	private HashSet<Integer> getNeighbors(int i){
		HashSet<Integer> neighbors = new HashSet<Integer>();
		int x = maze.width;
		
		//east neighbor
		int loc = i + 1;
		if (i/x == loc/x && maze.isLegal(loc)){ //if there is an east neighbor
			neighbors.add(loc);
		}

		//north neighbor
		loc = i + (1*x);
		if (maze.isLegal(loc)){ //if there is a north neighbor
			neighbors.add(loc);
		}

		//west neighbor
		loc = i - 1;
		if (i/x == loc/x && maze.isLegal(loc)){
			neighbors.add(loc);
		}

		//south neighbor
		loc = i - (1*x);
		if (maze.isLegal(loc)){
			neighbors.add(loc);
		}

		return neighbors;
	}

	//get the probability of transitioning from state v0 to state v1
	private double getTransitionProbability(int v0, int v1, Hashtable<Integer, HashSet<Integer>> topography){
		//if the same location, get the P() of staying still
		if (v1 == v0){
			//probability of a transition * number of self pointing paths
			return (1/NUM_NEIGHBORS)*(NUM_NEIGHBORS - topography.get(v0).size());
		}

		//if v0 is a neighbor of v1, then get the probability
		if (topography.get(v0).contains(v1)){
			return 1/NUM_NEIGHBORS;
		}

		//if there is no possible way to transition from v0 to v1, then P() = 0
		return 0;
	}
	
	//Generate a random set of moves -- a list of coordinates
	public int[][] generateRandomMoves(int num){
		Random random = new Random();
	
		int[][] path = new int[num][];
		
		int var = variables[random.nextInt(variables.length)];
		path[0] = new int[]{var%maze.width, var/maze.width};
	
		for (int i=1; i< num; i++){
			int[] move = MOVES[random.nextInt(MOVES.length)];
			int newx = path[i-1][0] + move[0];
			int newy = path[i-1][1] + move[1];
			if(maze.isLegal(newx, newy)){
				path[i] = new int[]{newx, newy};
			}
			else{
				path[i] = path[i-1];
			}
		}
		return path;
	}
	
	//from a robot path, generate a list of observations
	//parameters: boolean errr. if true the sensor will err
	public int[] generateObservations(int[][] path, boolean errr){
		int[] observations = new int[path.length];
		
		if (!errr){
			int i=0;
			for (int[] location:path){
				char c = maze.getChar(location[0], location[1]);
				
				
				observations[i] = getCharInt(c);
				
				i++;
			}
			return observations;
		}
	
		Random random = new Random();
		
		int i=0;
		for (int[] location:path){
			char c = maze.getChar(location[0], location[1]);
			
			double error = random.nextDouble();
			System.out.println(error + " " + error_rate);
			System.out.println(error < error_rate);
			//get the correct observation
			int charval = getCharInt(c);
			
			//Generate a random error
			if (error < error_rate){
				int errorchar = charval;
				while (errorchar == charval){
					errorchar = random.nextInt(COLORS.length-1);
				}
				observations[i] = errorchar;
			}
			else{
				observations[i] = charval;
			}
			
			
			i++;
		}
		return observations;

	}
	
	//returns the integer representation of the character
	private int getCharInt(char c){
		int i=0;
		for (char color:COLORS){
			if (c==color){
				break;
			}
			i++;
		}
		return i;
	}
	
	public String toString(){
		String s = "";

		ArrayList<Vector> state = getState();
		int i=0;
		for (Vector time_slice: state){
			s += "=========================\n";
			s += "          t:" + Integer.toString(i) + "           \n";
			s += "=========================\n";
			
			int j=0;
			for (int y = 0; y < maze.height; y++) {
				for (int x = 0; x < maze.width; x++) {
					s += time_slice.get(j);
					j++;
				}
				s += "\n";
			}
			i++;
			
			
		}
		
		return s;
	}
	
	public String toString(ArrayList<Vector> state, int[] obs){
		String s = "";

		int i=0;
		for (Vector time_slice: state){
			s += "===============================================================================\n";
			if (i > 0){
				s += "                           t:" + Integer.toString(i) + "   observation:" +  COLORS[obs[i-1]] + "          \n";
			}
			else{
				s += "                           t:" + Integer.toString(i) + "   observation:null        \n";

			}
			s += "===============================================================================\n";
			int j=0;
			for (int y = 0; y < maze.height; y++) {
				s += "| ";
				for (int x = 0; x < maze.width; x++) {
					s += Double.toString(time_slice.get(j));
					s += "\t| ";
					j++;

				}
				s += "\n|";
						
				for (int x= 0; x < maze.width; x++){
					s+= "=======================|";
				}
						
				s+= "\n";
			}
			i++;
			
			
		}
		
		return s;
	}
	
	public static void main(String[] args) throws IOException{
		Maze daMaze = Maze.readFromFile("C:\\Users\\Jack Terwilliger\\AI\\Probablistic Reasoning Over Time\\4x4WallMaze.maz");
		System.out.println(daMaze);
		System.out.println(daMaze.getChar(0));
		SensorRobot robot = new SensorRobot(daMaze);
		System.out.println(robot.variables[9]);
		System.out.println(robot.filter(new int[] {0,0,2,1,1,2,0,0,2,3}));
		System.out.println(robot.smoothing(new int[] {0,0,2,1,1,2,0,0,2,3}));
		System.out.println(Arrays.toString(robot.mostLikelySequence(new int[] {0,0,2,1,1,2,0,0,2,3})));
	}
}
/**
 * @author Jack Terwilliger
 * @date 02/5/14
 * 
 * Modified from provided code from Devin Balkcom
 * 
 * 
 */

package PROT;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Maze {
	final static Charset ENCODING = StandardCharsets.UTF_8;
	private final char WALL = 'X';
	
	public int width;
	public int height;
	
	private char[][] grid;

	public static Maze readFromFile(String filename) {
		Maze m = new Maze();

		try {
			List<String> lines = readFile(filename);
			m.height = lines.size();

			int y = 0;
			m.grid = new char[m.height][];
			for (String line : lines) {
				m.width = line.length();
				m.grid[m.height - y - 1] = new char[m.width];
				for (int x = 0; x < line.length(); x++) {
					
					
					m.grid[m.height - y - 1][x] = line.charAt(x);
				}
				y++;

			}

			return m;
		} catch (IOException E) {
			return null;
		}
	}

	public int[][] coordinatesFromLoc(int[] locations, int[] variableMap){
		
		int[][] coordinates = new int[locations.length][];
		int i =0;
		for (int location: locations){
			int[] coordinate = new int[2];
			
			coordinate[0] = this.getXFromVar(variableMap[location]);
			coordinate[1] = this.getYFromVar(variableMap[location]);
			
			coordinates[i] = coordinate;
			i++;
		}
		return coordinates;
	}
	
	private static List<String> readFile(String fileName) throws IOException {
		Path path = Paths.get(fileName);
		return Files.readAllLines(path, ENCODING);
	}
	
	private int getXFromVar(int variable){
		return variable%width;
	}
	
	private int getYFromVar(int variable){
		return variable/width;
	}
	
	public char getChar(int variable){
		return grid[getYFromVar(variable)][getXFromVar(variable)];
	}
	
	public char getChar(int x, int y) {
		return grid[y][x];
	}
	
	// is the location x, y on the map, and also a legal floor tile (not a wall)?
	public boolean isLegal(int variable) {
		int x = getXFromVar(variable);
		int y = getYFromVar(variable);
		// on the map
		if(x >= 0 && x < width && y >= 0 && y < height) {
			// and it's a floor tile, not a wall tile:
			return getChar(x, y) != WALL;
		}
		return false;
	}
	
	// is the location x, y on the map, and also a legal floor tile (not a wall)?
	public boolean isLegal(int x, int y) {
		// if it's on the map
		if(x >= 0 && x < width && y >= 0 && y < height) {
			// and if it's a floor tile, not a wall tile:
			return getChar(x, y) != WALL; //altered from ...=='.'
		}
		return false;
	}
	
	// is the location x, y on the map, and also a legal floor tile (not a wall)?
	public boolean isLegal(int[] x, int[] y) {
		
		//if not a collision
		if(true) {
			for(int i=0; i<x.length; i++){
				// on the map
				if(x[i] >= 0 && x[i] < width && y[i] >= 0 && y[i] < height) {
					// and it's a floor tile, not a wall tile:
					return getChar(x[i], y[i]) != WALL;
				}
			}
		}
		return false;
	}
	
	public int legalCoutn() {
		
		int legal = 0;
		
		for(int i = 0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if (getChar(i, j) != WALL){
					legal++;
				}
			}
		}
		
		return legal;
	}
	
	public int size(){
		return height*width;
	}
	
	public String toString() {
		String s = "";
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				s += grid[y][x];
			}
			s += "\n";
		}
		return s;
	}

	public static void main(String args[]) {
		Maze m = Maze.readFromFile("C:\\Users\\Jack Terwilliger\\AI\\Probablistic Reasoning Over Time\\2x2Maze.maz");
		System.out.println(m);
	}

}

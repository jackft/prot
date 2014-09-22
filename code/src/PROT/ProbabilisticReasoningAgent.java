/**
 * @author Jack Terwilliger
 * @date 02/5/14
 * 
 * ProbabilisticReasoningAgent is an abstract class containing forward, forward-backward, and viterbi algorithms
 * 
 * I've implemented forward and forward backward algorithms using matrices which is accomplished using the la4j linear algebra library
 * Viterbi is implemented without la4j
 */

package PROT;

import java.util.ArrayList;
import java.util.Arrays;
import org.la4j.matrix.Matrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

public abstract class ProbabilisticReasoningAgent{
	
	//transition and observation models are represented as a lists of matrices, which are instantiated in concrete classes
	
	private Matrix transition_model; //#variables x #variables
	private Matrix[] observation_model; //list of diagonal matrices each matrix corresponds to an observation model for a particular observation value #vars x #vars

	public ArrayList<Vector> state; //probability distribution of state variables over time
	
	//Set the state at time 0. At time 0, the distribution of state variables should be Uniform
	//Parameter: numVariables used to set Vector length and get the Uniform probability
	protected void setState(int numVariables){
		
		state = new ArrayList<Vector>(); 
		double[] init_state_double = new double[numVariables];
		
		//Get a Uniform Distribution
		double probability = 1/((double) numVariables);
		
		//set each state variable probability
		for (int index=0; index<numVariables; index++){
			init_state_double[index] = probability;
		}
		
		//create the vector
		Vector initial_state = new BasicVector(init_state_double);
		state.add(initial_state);
	}
	
	protected void setT(Matrix t){
		transition_model = t;
	}
	
	protected void setO(Matrix[] o){
		observation_model = o;
	}
	
	public Matrix getT(){
		return transition_model;
	}

	public Matrix getO(int i){
		return observation_model[i];
	}

	public ArrayList<Vector> getState(){
		return state;
	}
	
	//Compute the Belief State at time t: P(State_0:t|sequenceOfObservations_1:t)
	//
	//returns a sequence of belief states at each step of the observation
	//parameters: int[] obs is the sequence of observations
	public ArrayList<Vector> filter(int[] obs){
		ArrayList<Vector> forward_vector = new ArrayList<Vector>();
		forward_vector.add(state.get(0));
		return forward(forward_vector.get(0), obs, forward_vector, 0, false);
	}

	//Compute the Belief State at time k: P(State_0:k|sequenceOfObservations_1:k)*PsequenceOfObservations_k+1:t|State_k+1:t)
	//
	//returns a sequence of belief states at each step of the observation
	//parameters: int[] obs is the sequence of observations
	public ArrayList<Vector> smoothing(int[] obs){
		return forwardBackward(obs);
	}
	
	//Compute the Most Likely Path through the state space given a sequence of observations
	//
	//I didn't use la4j in Viterbi
	//
	//returns an array of ints representing state variables
	//parameters: int[] obs is the sequence of observations
	public int[] mostLikelySequence(int[] obs){
		//initialize state at t0: 
		double[] start_probability = new double[transition_model.rows()];
		Arrays.fill(start_probability, 1);
		return viterbi(obs, 0, start_probability, new int[obs.length][]);
	}
	
	//Backtrack through the most likely path
	//at each time t, a variable points backward to which state most likely transitioned to it
	//
	//returns int[] path. an array of ints representing the most likely path through state space
	//parameters: viterbiPath is the set of backpointers at every time t, double[] past_probabilities the distribution of state variables over time t
	private int[] backtracking(int[][] viterbiPath, double[] past_probabilities){
		//System.out.println(past_probabilities[26] + " " + past_probabilities[7] + " " + past_probabilities[9]+ " " + past_probabilities[13]+ " " + past_probabilities[16]+ " " + past_probabilities[20] + " " + past_probabilities[35]);
		int[] path = new int[viterbiPath.length];
		
		//Find the most probable state at time t
		//we will backtrack through the path that let to this state
		int max = 0;
		double max_prob = 0;
		for (int j=0; j<past_probabilities.length; j++){
			if (max_prob<past_probabilities[j]){
				max = j;
				max_prob = past_probabilities[j];
			}
		}
		
		
		path[viterbiPath.length-1] = max;
		return backtracking(viterbiPath, viterbiPath.length-1, path);
	}
	
	//Backtrack through the most likely path
	//at each time t, a variable points backward to which state most likely transitioned to it
	//
	//returns int[] path
	//parameters: viterbiPath is the set of backpointers at every time t, 
	private int[] backtracking(int[][] viterbiPath, int t, int[] path){
		
		//Base Case
		if (t == 0){
			return path;
		}
		
		//recursive case
		path[t-1] = viterbiPath[t][path[t]];
		return backtracking(viterbiPath, t-1, path);
		
	}
	
	//Recursive Viterbi: At time t, find the most probable path for every state variable from t-1 to t
	//
	//returns the most likely path through the state space
	//parameters: int[] obs the sequence of observations, int t the current time slice t,
	//double[] past_probabilities the probability distribution of state variables at time t-1, int[][] viterbiPath set of backPointers for every t
	private int[] viterbi(int[] obs, int t, double[] past_probabilities, int[][] viterbiPath){
		
		//Base Case
		
		//backtrack through the set of backpointers
		if (t >= obs.length){
			return backtracking(viterbiPath, past_probabilities);
		}
		
		//Recursive Case
		
		//get Ot
		Matrix obs_mod = observation_model[obs[t]];
		
		//new set of backpointers and set of probability distribution over state variables
		int[] subPath = new int[past_probabilities.length];
		double[] current_probabilities = new double[past_probabilities.length];
		
		//for every variable x_t, get the max P(x1_t|X_t-1)
		for (int state_variable_current=0; state_variable_current<transition_model.rows(); state_variable_current++){
			
			int max = 0; //the max variable
			double max_prob = 0; //the max probability
			double full_prob = 0;
			
			//for every variable x_t-1
			for (int state_variable_past=0; state_variable_past<transition_model.columns(); state_variable_past++){
				
				double probabilityOfstate_variable = transition_model.get(state_variable_current, state_variable_past)*obs_mod.get(state_variable_current, state_variable_current)*past_probabilities[state_variable_past];
								
				if (probabilityOfstate_variable > max_prob){
					max = state_variable_past;
					max_prob = probabilityOfstate_variable;
				}
				full_prob += probabilityOfstate_variable;
	
			}
			
			//add backpointer
			subPath[state_variable_current] = max;
			
			if (obs.length-1 == t){
				System.out.println(state_variable_current + " " + max_prob);
				//add max P(x_t|X_t-1)
				current_probabilities[state_variable_current] += full_prob;
			}
			else{
				//add P(x_t|X_t-1)
				current_probabilities[state_variable_current] = max_prob;
			}
	
		}
		viterbiPath[t] = subPath;
		
		//recurse
		return viterbi(obs, t+1, current_probabilities, viterbiPath);
	}
	
	//Compute the Belief State at time k: P(State_0:k|sequenceOfObservations_1:k)*PsequenceOfObservations_k+1:t|State_k+1:t)
	//
	//returns a sequence of belief states at each step of the observation
	//parameters: int[] obs is the sequence of observations
	private ArrayList<Vector> forwardBackward(int[] obs){
		
		ArrayList<Vector> forward_vectors = new ArrayList<Vector>();
		forward(state.get(0), obs, forward_vectors, 0, true);
		
		ArrayList<Vector> backward_vectors = new ArrayList<Vector>();
		
		//backwards algorithm starts with a vector filled with ones
		double[] firstBack = new double[state.get(0).length()];
		Arrays.fill(firstBack, 1.0);
		backward_vectors.add(0,new BasicVector(firstBack));
		
		backward(backward_vectors.get(0), obs, obs.length-1, backward_vectors, true);
		
		for (int i=0; i<obs.length; i++){
			state.add(normalize(forward_vectors.get(i).hadamardProduct(backward_vectors.get(i))));
		}
		return state;
	}
	
	private ArrayList<Vector> forward(Vector prev_forward, int[] obs,  ArrayList<Vector> state_to_t, int t, boolean isFB){
		//Base Case
		if (t >= obs.length){
			return state_to_t;
		}
	
		//Recursive Case		
		Vector current = null;
		//not used for forward backward, normalize
		if (isFB == false){
			//new matrix = a*O*Transpose(T)*(prev_forward)
			current = normalize(getO(obs[t]).multiply(getT()).multiply(prev_forward));
		}
		else{
			//new matrix = a*O*Transpose(T)*(prev_forward)
			current = getO(obs[t]).multiply(getT()).multiply(prev_forward);
		}
		
		state_to_t.add(current);
		
		//recursiveCall
		return forward(current, obs, state_to_t, t+1, isFB);
	}
	
	//Normalize a vector
	private Vector normalize(Vector notNormalized){
	return notNormalized.multiply((1/notNormalized.sum()));
	}
	
	private ArrayList<Vector> backward(Vector prev_backward, int[] obs, int t, ArrayList<Vector> state_to_1, boolean isFB){
		//Base Case
		if (t < 1){
			return state_to_1;
		}
	
		//Recursive Case
		
		Vector current = null;
		if (isFB != true){
			//new matrix = a*T*O*(prev_backward)
			current = normalize(getT().multiply(getO(obs[t])).multiply(prev_backward));
		}
		else{
			//new matrix = T*O*(prev_backward)
			current = getT().multiply(getO(obs[t])).multiply(prev_backward);
		}
		
		state_to_1.add(0, current);
		
		//recursiveCall
		return backward(current, obs, t-1, state_to_1, isFB);
	}
	
	//compute the likelihood of a sequence
	public double likelihood(int[] obs){
		filter(obs);
		return state.get(state.size()-1).sum();
	}
	
	//compute the likelihood of a
	public double likelihood(){
		return state.get(state.size()-1).sum();
	}
}
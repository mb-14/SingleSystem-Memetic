package org.isw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;


public class MemeticAlgorithm {
	static ArrayList<Schedule> schedule;
	static ArrayList<Machine> machines;
	static ArrayList<ArrayList<Integer>> pmOs;
	static Hashtable<String, Double>  fitnessCache;
	private int populationSize;
	private int stopCrit;
	Double best;
	private ArrayList<Chromosome> population;
	private ArrayList<Chromosome> offsprings;
	private double totalFitness;
	Random rand;
	EnumeratedDistribution<Chromosome> distribution;
	private int convergenceCount;
	private boolean noLS;
	public MemeticAlgorithm(int populationSize, int stopCrit, ArrayList<Schedule> scheduleList , ArrayList<Machine>machineList, boolean noLS){
		this.populationSize = populationSize;
		this.population = new ArrayList<Chromosome>();
		this.stopCrit = stopCrit;
		schedule = scheduleList;
		machines = machineList;
		rand = new Random();
		best = 0d;
		convergenceCount = 0;
		pmOs = new ArrayList<ArrayList<Integer>>();
		this.noLS = noLS;
		fitnessCache = new Hashtable<String,Double>();
	}

	public ArrayList<Schedule> execute() throws InterruptedException, ExecutionException, NumberFormatException, IOException
	{
		initializePopulation();
		System.out.println("Evaluating initial population");
		evaluateFitness(population);
		int cnt=0;
		while(true){
			totalFitness = 0;
			for(Chromosome individual: population)
				totalFitness += individual.fitnessValue;

			try{
				distribution = new EnumeratedDistribution<Chromosome>(populationDistribution());
			}catch(Exception e){
				e.printStackTrace();

			}
			if(cnt++ >= stopCrit /*|| hasConverged()*/)
				break;
			generatePopulation();
			System.out.format("%d,%f\n",cnt,population.get(0).fitnessValue);
		}

		Collections.sort(population);
		for(int j=0;j<machines.size();j++){
			addPMJobs(schedule.get(j),machines.get(j).compList,j, population.get(0).getCombolist(j));
		}
		return schedule;
	}



	private boolean hasConverged() {
		if(population.get(0).fitnessValue == best){
			convergenceCount++;
			if(convergenceCount == 25)
				return true;
		}
		else{
			convergenceCount = 0;
			best =  population.get(0).fitnessValue;
		}
		return false;
	}

	private void generatePopulation() throws InterruptedException, ExecutionException {
		//TODO: Mutation, crossover ratio
		offsprings = new ArrayList<Chromosome>();
		int numberOfPairs = (populationSize/4%2==0)?populationSize/4:populationSize/4+1; 
		for(int i=0;i<numberOfPairs;i++){
			Chromosome[] parents = selectParents();
			long[] offspring1 = new long[machines.size()];
			long[] offspring2 = new long[machines.size()];
			for(int j=0;j<machines.size();j++){
				if(parents[0].combo[j] != parents[1].combo[j]){
					long[] offspringPart = crossover(parents[0].combo[j],parents[1].combo[j],j);
					offspring1[j] = offspringPart[0];
					offspring2[j] = offspringPart[1];
				}
			}
			offsprings.add(new Chromosome(offspring1));
			offsprings.add(new Chromosome(offspring2));
		}
		//Do mutation here
		for(Chromosome offspring: offsprings){
			if(rand.nextDouble() < 0.4){
				for(int j=0;j<machines.size();j++){
					int limit = machines.get(j).compList.length*pmOs.get(j).size();
					int mutationPoint = rand.nextInt(limit);
					if((offspring.combo[j]^1<<mutationPoint) !=0)
						offspring.combo[j] ^= 1<<mutationPoint;

					mutationPoint = rand.nextInt(limit);
					if((offspring.combo[j]^1<<mutationPoint) !=0)
						offspring.combo[j] ^= 1<<mutationPoint;
				}
			}
		}

		if(!noLS){
			optimizeOffsprings();
		}
		evaluateFitness(offsprings);
		population.addAll(offsprings);
		Collections.sort(population);
		population.subList(populationSize-1, population.size()-1).clear();
	}

	//Single point crossover
	private long[] crossover(long parent1, long parent2, int machineIndex) {
		long[] offsprings = new long[2];
		long combo1[] = {parent1,parent1};
		long combo2[] = {parent2,parent2};
		int limit = machines.get(machineIndex).compList.length * pmOs.get(machineIndex).size();
		int crossoverPoint = rand.nextInt(limit-1)+1;
		for(int i=0; i<limit;i++){
			if(i < crossoverPoint){
				combo2[0] = combo2[0] & ~(1<<i);
				combo1[1] = combo2[1] & ~(1<<i);
			}
			else{
				combo1[0] = combo1[0] & ~(1<<i);
				combo2[1] = combo2[1] & ~(1<<i);
			}
		}

		offsprings[0] = combo1[0]|combo2[0];
		offsprings[1] = combo1[1]|combo2[1];
		return offsprings;
	}


	private Chromosome[] selectParents() {
		Chromosome parents[] = new Chromosome[2];
		distribution.sample(2,parents);
		return parents;
	}

	//Distribution for the roulette wheel selection
	private List<Pair<Chromosome, Double>> populationDistribution() {
		ArrayList<Pair<Chromosome, Double>> dist = new ArrayList<Pair<Chromosome, Double>>();
		for(int i=0;i<populationSize;i++){
			dist.add(new Pair<Chromosome, Double>(population.get(i),population.get(i).fitnessValue/totalFitness));
		}
		return dist;
	}

	private void evaluateFitness(ArrayList<Chromosome> list) throws InterruptedException, ExecutionException {
		//TODO
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		for(Chromosome chromosome : list){
			if(fitnessCache.containsKey(stringRep(chromosome.combo))){
				chromosome.fitnessValue = fitnessCache.get(stringRep(chromosome.combo));
			}
			else{
				ArrayList<Schedule> tempSchedules = new ArrayList<Schedule>();
				for(int j=0;j<machines.size();j++){
					tempSchedules.add(new Schedule(schedule.get(j)));
					addPMJobs(tempSchedules.get(j),machines.get(j).compList,j, chromosome.getCombolist(j));
				}
				threadPool.execute(new ScheduleExecutionThread(tempSchedules,machines,chromosome));
			}

		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());

	}


	private void optimizeOffsprings() {
		for(Chromosome chromosome: offsprings){
			chromosome.applyLocalSearch();
		}	
	}

	private static void addPMJobs(Schedule schedule, Component[] compList, int j , long[] compCombo) {
		/*
		 * Add PM jobs to given schedule.
		 */
		int cnt = 0;
		ArrayList<Integer> pmOpportunity = pmOs.get(j);

		for(int pmOpp = 0; pmOpp<pmOpportunity.size(); pmOpp++)
		{
			for(int i=0;i< compList.length;i++)
			{
				int pos = 1<<i;
				if((pos& compCombo[pmOpp])!=0) //for each component in combo, add a PM job
				{
					long pmttr = Component.notZero(compList[i].getPMTTR()*Macros.TIME_SCALE_FACTOR); 

					Job pmJob = new Job("PM",pmttr,compList[i].getPMLabourCost(),Job.JOB_PM);
					pmJob.setCompNo(i);

					if(cnt==0){
						// consider fixed cost only once, for the first job
						pmJob.setFixedCost(compList[i].getPMFixedCost());
					}

					// add job to schedule
					schedule.addPMJob(new Job(pmJob),pmOpportunity.get(pmOpp)+cnt);

					cnt++;
				}
			}
		}
	}

	static String stringRep(long[] num){
		String str = "";
		for(int i=0;i<num.length;i++){
			str += String.valueOf(num[i])+".";
		}
		return str;

	}
	private void initializePopulation() throws NumberFormatException, IOException 
	{
		System.out.println("Initialize population");
		long num[] = new long[machines.size()];
		long upper[] = new long[machines.size()];
		for(int i=0;i<machines.size();i++)
		{
			pmOs.add(schedule.get(i).getPMOpportunities());
			upper[i] =(long) Math.pow(2, machines.get(i).compList.length*pmOs.get(i).size())-1;
		}
		Hashtable<String, Boolean> hashTable = new Hashtable<String, Boolean>();
		for(int i=0;i<populationSize;i++)
		{
			do{
				for(int j = 0; j < machines.size();j++)
					num[j] = (long)(Math.random()*upper[j]);
			}while(hashTable.containsKey(stringRep(num)));
			population.add(new Chromosome(num));
			hashTable.put(stringRep(num), new Boolean(true));
		}	
	}
}




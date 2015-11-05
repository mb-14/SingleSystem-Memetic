package org.isw;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ParticleSwarm {
	static ArrayList<Schedule> schedule;
	static ArrayList<Machine> machines;
	static ArrayList<ArrayList<Integer>> pmOs;
	static Hashtable<String, Double>  fitnessCache;
	private int populationSize;
	private int stopCrit;
	private ArrayList<Particle> population;

	Particle globalBest;
	static double W, Cp, Cg;
	public static long factor;

	public ParticleSwarm(int populationSize, int stopCrit, ArrayList<Schedule> scheduleList , ArrayList<Machine>machineList){
		this.populationSize = populationSize;
		this.population = new ArrayList<Particle>();
		this.stopCrit = stopCrit;
		schedule = scheduleList;
		machines = machineList;
		pmOs = new ArrayList<ArrayList<Integer>>();
		fitnessCache = new Hashtable<String,Double>();

		globalBest = new Particle();
		globalBest.cost = -1;
	}

	public ArrayList<Schedule> execute() throws InterruptedException, ExecutionException, NumberFormatException, IOException
	{

		System.out.format("Spawning Particle Swarm with Cg=%f, Cp=%f, W=%f, generations=%d, pop size=%d\n",Cg, Cp, W, stopCrit, populationSize);

		initializeParticles();

		System.out.println("Evaluating initial population");
		evaluateFitness(population);
		System.out.format("Gen: %d, Best: %f\n", 0, globalBest.cost);

		for(int generation=1; generation<=stopCrit; generation++)
		{
			// move particles
			for(Particle particle: population)
			{
				if(particle.x == globalBest.x)
				{
					for(int i=0; i<particle.x.length; i++)
					{
						particle.x[i] += Math.round(Math.random()*particle.upper[i]);
						particle.v[i] = Math.round(particle.upper[i]/2);
					}
				}
				
				for(int dim=0; dim<particle.x.length; dim++)
				{
					//60 6 3 1 2 3 3 1 2 5 3 5 6 7 3 13 14 15 3 17 18 19 3 17 18 20
					//60 8 3 1 2 3 3 1 2 5 3 5 6 7 3 8 9 10 3 9 10 11 3 13 14 15 3 17 18 19 3 9 17 21

					// accelerate stationary particle
					if(particle.v[dim] == 0)
						particle.v[dim] = 1+particle.upper[dim]/10000;

					//accelerate particle
					particle.v[dim] = Math.round(W*particle.v[dim] 
							+ Math.random()*Cp*(particle.bestX[dim]-particle.x[dim]) 
							+ Math.random()*Cg*(globalBest.x[dim]-particle.x[dim]));

					//limit velocity to upper bound
					if(particle.v[dim] > particle.upper[dim])
					{
						particle.v[dim] = Math.round(particle.upper[dim]/2);
					}

					// update particle x
					particle.x[dim] += particle.v[dim];

					
					//keep particle within bounds
					if(particle.x[dim]<0 || particle.x[dim]>particle.upper[dim])
					{
						particle.x[dim] = Math.round(Math.random()* particle.upper[dim]);
						particle.v[dim] = Math.round(particle.v[dim]*-1);
					}
				}
			}

			evaluateFitness(population);
			System.out.format("Gen: %d, Best: %f\n", generation, globalBest.cost);
		}
		for(int j=0;j<machines.size();j++)
		{
			addPMJobs(schedule.get(j),machines.get(j).compList,j, globalBest.getCombolist(j));
		}

		Scanner scan = new Scanner(new InputStreamReader(System.in));
		scan.nextLine();
		scan.close();
		return schedule;
	}

	private void evaluateFitness(ArrayList<Particle> list) throws InterruptedException, ExecutionException {
		System.out.println("Evaluating fitness");
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		for(Particle particle : list)
		{
			if(fitnessCache.containsKey(stringRep(particle.x)))
			{
				particle.cost = fitnessCache.get(stringRep(particle.x));
			}
			else
			{

				boolean invalid = false;
				for(int dim=0; dim<particle.x.length; dim++)
				{
					if(particle.x[dim] > particle.upper[dim] || particle.x[dim]<0)
					{
						invalid = true;
						break;
					}
				}
				if(invalid)
				{
					System.out.println("Invalid value");
					particle.cost = Double.MAX_VALUE;
					ParticleSwarm.fitnessCache.put(ParticleSwarm.stringRep(particle.x), Double.MAX_VALUE);
					for(int dim =0; dim<particle.x.length; dim++)
						if(particle.x[dim]>particle.upper[dim] || particle.x[dim]<0)
							particle.v[dim] *= -1;
					particle.updateBest();
				}
				else
				{

					ArrayList<Schedule> tempSchedules = new ArrayList<Schedule>();
					for(int j=0;j<machines.size();j++)
					{
						tempSchedules.add(new Schedule(schedule.get(j)));
						addPMJobs(tempSchedules.get(j),machines.get(j).compList,j, particle.getCombolist(j));
					}
					threadPool.execute(new ScheduleExecutionThread(tempSchedules,machines,particle));
				}
			}

		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());

		// update global
		for(Particle particle: list)
		{
			if (globalBest.cost == -1 || particle.bestCost < globalBest.cost)
			{
				globalBest.cost = particle.bestCost;
				globalBest.x = particle.bestX;
			}
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
	private void initializeParticles() throws NumberFormatException, IOException 
	{
		System.out.println("Initialize particles");
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
			population.add(new Particle(num, upper));
			hashTable.put(stringRep(num), new Boolean(true));
		}	
	}
}




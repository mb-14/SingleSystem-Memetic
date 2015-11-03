package org.isw;

import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduleExecutionThread implements Runnable{
	ArrayList<Schedule> scheduleList;
	ArrayList<Machine> machineList;
	Particle particle;
	public ScheduleExecutionThread(ArrayList<Schedule> scheduleList, ArrayList<Machine> machineList, Particle particle){
		this.scheduleList = scheduleList;
		this.particle = particle;
		this.machineList = machineList;
	}
	

	@Override
	public void run() 
	{
		ExecutorService threadPool = Executors.newFixedThreadPool(machineList.size());
		CompletionService<Double> pool = new ExecutorCompletionService<Double>(threadPool);
		CyclicBarrier sync = new CyclicBarrier(machineList.size());
		Object lock = new Object();
		int[] labour = new int[]{2,4,8};
		for(int i=0;i<machineList.size();i++)
		{
			pool.submit(new JobExecThread(scheduleList.get(i),machineList.get(i),true,sync,lock,labour));
		}
		Double cost = 0d;
		for(int i=0;i<machineList.size();i++){
			try {
				cost += pool.take().get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		particle.cost = cost;
		ParticleSwarm.fitnessCache.put(ParticleSwarm.stringRep(particle.x), cost);
		
		particle.updateBest();
	}

}

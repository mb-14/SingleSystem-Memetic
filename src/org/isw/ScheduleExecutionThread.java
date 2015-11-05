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
	Chromosome chromosome;
	public ScheduleExecutionThread(ArrayList<Schedule> scheduleList, ArrayList<Machine> machineList, Chromosome chromosome){
		this.scheduleList = scheduleList;
		this.chromosome = chromosome;
		this.machineList = machineList;
	}
	

	@Override
	public void run() {
		ExecutorService threadPool = Executors.newFixedThreadPool(machineList.size());
		CompletionService<Double[]> pool = new ExecutorCompletionService<Double[]>(threadPool);
		CyclicBarrier sync = new CyclicBarrier(machineList.size());
		Object lock = new Object();
		int[] labour = new int[]{2,4,8};
		for(int i=0;i<machineList.size();i++){
			pool.submit(new JobExecThread(scheduleList.get(i),machineList.get(i),true,sync,lock,labour));
		}
		Double cost = 0d;
		Double[] arr = new Double[4];
		arr[0] = 0d;
		arr[1] = 0d;
		arr[2] = 0d;
		arr[3] = 0d;
			for(int i=0;i<machineList.size();i++){
			try {
				Double[] totalCost = pool.take().get();
				arr[0] += totalCost[0];
				arr[1] += totalCost[1];
				arr[2] += totalCost[2];
				arr[3] += totalCost[3];
				cost += totalCost[0]+totalCost[1]+totalCost[2];
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		//chromosome.fitnessValue = cost;
		System.out.format("%f,%f,%f,,%f\n", arr[0],arr[1],arr[2],arr[3]);
		//MemeticAlgorithm.fitnessCache.put(MemeticAlgorithm.stringRep(chromosome.combo), cost);
	}

}

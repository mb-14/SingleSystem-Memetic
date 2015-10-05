package org.isw;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;


public class JobExecThread implements Callable<Double>{
	Schedule schedule;
	Machine machine;
	boolean isPlanning;
	CyclicBarrier sync;
	public JobExecThread(Schedule schedule, Machine machine, boolean isPlanning, CyclicBarrier sync){
		this.schedule = schedule;
		this.machine = machine;
		this.isPlanning = isPlanning;
		this.sync = sync;
	}
	public Double call() throws InterruptedException, BrokenBarrierException{
		long time = 0;
		Component[] compList;
		Schedule jobList;
		if(isPlanning)
		{
			compList = new Component[machine.compList.length];
			for(int i=0;i< machine.compList.length;i++)
				compList[i] = new Component(machine.compList[i]);
			jobList = new Schedule(schedule);	
		}
		else{
			jobList = schedule;
			compList = machine.compList;
		}
		Double cost = 0d;
		// find all machine failures and CM times for this shift
		LinkedList<FailureEvent> failureEvents = new LinkedList<FailureEvent>();
		FailureEvent upcomingFailure = null;
		for(int compNo=0; compNo<compList.length; compNo++)
		{
			long ft = (long) compList[compNo].getCMTTF();
			if(ft < Macros.SHIFT_DURATION)
			{
				// this component fails in this shift
				failureEvents.add(new FailureEvent(compNo, ft*Macros.TIME_SCALE_FACTOR));
			}
		}
		if(!failureEvents.isEmpty())
		{
			Collections.sort(failureEvents, new FailureEventComparator());
			upcomingFailure =  failureEvents.pop();
		}

		while(true)
		{
			
			if(time == Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR) {
				time = 0;
				if(!jobList.isEmpty()) {
					int i = jobList.indexOf(jobList.peek());
					while(i < jobList.getSize()){
						if(!isPlanning)
							machine.penaltyCost += jobList.jobAt(i++).getPenaltyCost()*jobList.jobAt(i-1).getJobTime();
						else
							cost += jobList.jobAt(i++).getPenaltyCost()*jobList.jobAt(i-1).getJobTime();
					}
				}
				return cost;
			}
			
			if(jobList.isEmpty()){
				timeSync();
				time++;
				if(!isPlanning)
					machine.idleTime++;
				continue;
			}

			Job current = jobList.peek(); 

			/*
			 * Perform action according to what job is running
			 * Increment costs or wait for labour to arrive for CM/PM
			 */
			if(current.getJobType()!= Job.JOB_CM&&current.getJobType()!= Job.JOB_PM && upcomingFailure!=null && time == upcomingFailure.failureTime)
			{
				/*
				 * Machine fails. 
				 * Add CM job to top of schedule and run it. 
				 */
				if(!isPlanning){
					System.out.println("Machine Failed. Requesting maintenance...");
				}
				Job cmJob = new Job("CM", upcomingFailure.repairTime, machine.compList[upcomingFailure.compNo].getCMLabourCost(), Job.JOB_CM);
				cmJob.setFixedCost(machine.compList[upcomingFailure.compNo].getCMFixedCost());
				cmJob.setCompNo(upcomingFailure.compNo);
				jobList.addJobTop(cmJob);
				machine.setStatus(Macros.MACHINE_WAITING_FOR_CM_LABOUR);
				current = jobList.peek();
			}

			if(machine.getStatus() == Macros.MACHINE_WAITING_FOR_CM_LABOUR || machine.getStatus() == Macros.MACHINE_WAITING_FOR_PM_LABOUR)
			{
				
				if(Main.labour.get())
				{
					Main.labour.set(false);
					// labour is available, perform maintenance job
					if(current.getJobType() == Job.JOB_CM)
						machine.setStatus(Macros.MACHINE_CM);
					if(current.getJobType() == Job.JOB_PM)
						machine.setStatus(Macros.MACHINE_PM);
					continue;
				}
				else if(!Main.labour.get())
				{
					
					if(!isPlanning){
					System.out.println("Request denied. Not enough labour " + time);
					//Logger.log(Machine.getStatus(),"Request denied. Not enough labour");
					}
					// machine waits for labour
					// increment cost models accordingly
					if(!isPlanning){
					machine.downTime++;
					machine.waitTime++;
					}
				}
				else if(!isPlanning){
					System.out.println("\n\n**************\nERROR WHILE REQUESTING MAINTENANCE FOR STATUS\n**************\n");
					}
				}

			else if(current.getJobType() == Job.JOB_NORMAL)
			{
				machine.setStatus(Macros.MACHINE_RUNNING_JOB);
				current.setStatus(Job.STARTED);
				
				// no failure, no maintenance. Just increment cost models normally.
				if(!isPlanning)
					machine.procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				cost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				for(Component comp : compList)
					comp.initAge++;
				if(!isPlanning)
					machine.runTime++;
			}

			else if(current.getJobType() == Job.JOB_PM)
			{
				if(machine.getStatus() != Macros.MACHINE_PM)
				{
					// request PM if labours not yet allocated
					machine.setStatus(Macros.MACHINE_WAITING_FOR_PM_LABOUR);
					continue;
				}
				if(!isPlanning){
				System.out.println(current.getJobTime());
				}
				// since an actual PM job is a series of PM jobs of each comp in compCombo
				// we set all jobs in series to SERIES_STARED
				if(current.getStatus() == Job.NOT_STARTED)
				{
					current.setStatus(Job.STARTED);
					for(int i=1; i<jobList.getSize(); i++)
					{
						Job j = jobList.jobAt(i);
						if(j.getJobType() != Job.JOB_PM)
							break;
						j.setStatus(Job.SERIES_STARTED);
					}
				}
				else if(current.getStatus() == Job.SERIES_STARTED)
					current.setStatus(Job.STARTED);
				if(!isPlanning)
					machine.pmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				cost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				if(!isPlanning){
					machine.pmDownTime++;
					machine.downTime++;			
				}
			}
			else if(current.getJobType() == Job.JOB_CM && machine.getStatus() == Macros.MACHINE_CM)
			{
				current.setStatus(Job.STARTED);
				if(!isPlanning)
					machine.cmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				cost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				if(!isPlanning){
				machine.downTime++;
				machine.cmDownTime++;
				}
			}

			// decrement job time by unit time
			try{
				if(machine.getStatus()==Macros.MACHINE_RUNNING_JOB || machine.getStatus()==Macros.MACHINE_CM || machine.getStatus()==Macros.MACHINE_PM)
					{
					jobList.decrement(1);
					}
				}
			catch(IOException e){
				e.printStackTrace();
				System.exit(0);
			}
			time++;	
			// if job has completed remove job from schedule
			if(current.getJobTime()<=0)
			{
				switch(current.getJobType())
				{
				case Job.JOB_PM:
					Component comp1 = compList[current.getCompNo()];
					comp1.initAge = (1-comp1.pmRF)*comp1.initAge;
					if(!isPlanning){
						machine.compPMJobsDone[current.getCompNo()]++;
						machine.pmJobsDone++;
					}
					// let maintenance know how much labour has been released (for logging purpose only)
					if(jobList.getSize()<=1 || jobList.jobAt(1).getStatus()!=Job.SERIES_STARTED)
					{
						Main.labour.set(true);
					}

					// recompute component failures
					failureEvents = new LinkedList<FailureEvent>();
					upcomingFailure = null;
					for(int compNo=0; compNo< compList.length; compNo++)
					{
						long ft = time + (long) compList[compNo].getCMTTF()*Macros.TIME_SCALE_FACTOR;
						if(ft < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
						{
							// this component fails in this shift
							failureEvents.add(new FailureEvent(compNo, ft));
						}
					}

					if(!failureEvents.isEmpty())
					{
						Collections.sort(failureEvents, new FailureEventComparator());
						upcomingFailure =  failureEvents.pop();
					}
					break;
					
				case Job.JOB_CM:
					// 
					Component comp = compList[current.getCompNo()];
					comp.initAge = (1 - comp.cmRF)*comp.initAge;
					if(!isPlanning){
						machine.cmJobsDone++;
						machine.compCMJobsDone[current.getCompNo()]++;
					}
					// let maintenance know how much labour has been released (for logging purpose only)
					Main.labour.set(true);
					// recompute component failures
					failureEvents = new LinkedList<FailureEvent>();
					upcomingFailure = null;
					for(int compNo=0; compNo< compList.length; compNo++)
					{
						long ft = time + (long) compList[compNo].getCMTTF()*Macros.TIME_SCALE_FACTOR;
						if(ft < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
						{
							// this component fails in this shift
							failureEvents.add(new FailureEvent(compNo, ft));
						}
					}
					if(!failureEvents.isEmpty())
					{
						Collections.sort(failureEvents, new FailureEventComparator());
						upcomingFailure =  failureEvents.pop();
					}
					break;
				case Job.JOB_NORMAL:
					if(!isPlanning)
						machine.jobsDone++;
					break;
				}
				try{
					Job job = jobList.remove();
					// job is complete, remove from joblist
					if(!isPlanning){
						System.out.println("Job "+ job.getJobName()+" complete");
					}
					// update Machine status on job completion
					if(jobList.isEmpty())
						machine.setStatus(Macros.MACHINE_IDLE);
				
				}
				catch(IOException e){
					e.printStackTrace();
					System.exit(0);
				}
			}		
			
			//Poll for synchronization		
			timeSync();
			
		
		} //Loop ends 
	
	}
	private void timeSync() throws InterruptedException, BrokenBarrierException {
		// TODO Auto-generated method stub
		sync.await();
	}
	
	class FailureEvent
	{
		public int compNo;
		public long repairTime;
		public long failureTime;

		public FailureEvent(int compNo, long failureTime)
		{
			this.compNo = compNo;
			this.repairTime = Component.notZero(machine.compList[compNo].getCMTTR()*Macros.TIME_SCALE_FACTOR);
			this.failureTime = failureTime;
		}
	}

	class FailureEventComparator implements Comparator<FailureEvent> {
		/*
		 * Sort events in ascending order of failure time
		 */
		@Override
		public int compare(FailureEvent a, FailureEvent b) 
		{
			return Long.compare(a.failureTime,b.failureTime);
		}

	}

}


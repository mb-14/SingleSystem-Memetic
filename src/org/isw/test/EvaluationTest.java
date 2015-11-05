package org.isw.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.Chromosome;
import org.isw.Component;
import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;
import org.isw.ScheduleExecutionThread;

public class EvaluationTest {
	static ArrayList<Machine> machines;
	static ArrayList<ArrayList<Integer>> pmOs = new ArrayList<ArrayList<Integer>>();
	static int noOfMachines;
	private static ArrayList<Schedule> mainSchedules = new ArrayList<Schedule>();
	private static ArrayList<Job> jobArray;
	public static void main(String[] args) {
		Macros.loadMacros();
		Macros.SIMULATION_COUNT = 1000;
		machines = new ArrayList<Machine>();
		machines.add(new Machine(0,new int[]{1,2,3}));
		machines.add(new Machine(1,new int[]{1,2,5}));
		machines.add(new Machine(2,new int[]{5,6,7}));
		machines.add(new Machine(3,new int[]{8,9,10}));
		machines.add(new Machine(4,new int[]{9,10,11}));
		machines.add(new Machine(5,new int[]{13,14,15}));
		machines.add(new Machine(6,new int[]{9,17,21}));
		noOfMachines = machines.size();
		parseJobs();
		int count=0;
		long solution[] = new long[]{8328,262464,268288,17537,265,8200,135,321};
		Chromosome chromosome = new Chromosome(solution);
		for(int j=0;j<noOfMachines;j++){
			Schedule sched = new Schedule();
			for(int i=0;i<7;i++){
				sched.addJob(jobArray.get(count++));		
			}
			mainSchedules.add(sched);
			pmOs.add(sched.getPMOpportunities());
			addPMJobs(sched,machines.get(j).compList,j,getCombolist(solution[j],j));
		}
		
		int n = 100;
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		for(int i = 0;i<n;i++){
			ArrayList<Schedule> tempSchedules = new ArrayList<Schedule>();
			for(int j=0;j<machines.size();j++){
				tempSchedules.add(new Schedule(mainSchedules.get(j)));
			}
			threadPool.execute(new ScheduleExecutionThread(tempSchedules,machines,chromosome));
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
	public static long[] getCombolist(long combo, int j) {
		//J is machineIndex
		long combos[] = new long[pmOs.get(j).size()];
		for(int i =0;i< pmOs.get(j).size();i++){
			combos[i] = (combo>>(machines.get(j).compList.length*i))&((int)Math.pow(2,machines.get(j).compList.length)-1);
		}
		return combos;
	}
	public static void parseJobs() 
	{
		jobArray = new ArrayList<Job>();
		try
		{
			FileInputStream file = new FileInputStream(new File("Jobs.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			for(int i=1;i<= noOfMachines*7;i++)
			{
				Row row = sheet.getRow(i);
				String jobName = row.getCell(0).getStringCellValue();
				long jobTime = (long)(row.getCell(1).getNumericCellValue()*Macros.TIME_SCALE_FACTOR);
				//long jobTime = 206;
				double jobCost = row.getCell(3).getNumericCellValue();	
				Job job = new Job(jobName,jobTime,jobCost,Job.JOB_NORMAL);
				job.setPenaltyCost(row.getCell(4).getNumericCellValue());
				jobArray.add(job);
			}
			file.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}	
	}

}

package org.isw;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class Macros {
	public static final int START_MAINTENANCE_PLANNING = 19;
	public static final int REQUEST_MAINTENANCE = 20;
	public static final int MAINTENANCE_UNAVAILABLE = 21;
	public static final int MAINTENANCE_AVAILABLE = 22;
	public static final int LABOUR_GRANTED = 23;
	public static final int LABOUR_DENIED = 24;
	public static final int MACHINE_IDLE = 25;
	public static final int MACHINE_RUNNING_JOB = 26;
	public static final int MACHINE_WAITING_FOR_PM_LABOUR = 27;
	public static final int MACHINE_WAITING_FOR_CM_LABOUR = 28;
	public static final int MACHINE_PM = 29;
	public static final int MACHINE_CM = 30;
	public static final int MACHINE_PLANNING = 33;
	public static final int START_SCHEDULING = 31;
	public static final int INIT = 32;
	public static final int REQUEST_NEXT_PERMUTATION = 34;	
	
	public static int TIME_SCALE_FACTOR = 1;
	public static int SHIFT_DURATION = 1440;
	public static int SIMULATION_COUNT = 1000;
	public static int MA_POPULATION_SIZE = 100;
	public static int MA_GENERATIONS = 100;
	public static int NO_OF_JOBS = 7;
	public static int[] MAX_LABOUR={2,4,8};
	public static void loadMacros(){	
		try {
			Properties prop = new Properties();
			InputStream input = new FileInputStream("config.properties");
			prop.load(input);
			TIME_SCALE_FACTOR = Integer.parseInt(prop.getProperty("scaleFactor"));
			NO_OF_JOBS = Integer.parseInt(prop.getProperty("jobs"));
			SHIFT_DURATION = Integer.parseInt(prop.getProperty("shiftDuration"));
			SIMULATION_COUNT = Integer.parseInt(prop.getProperty("simulationCount"));
			MA_POPULATION_SIZE = Integer.parseInt(prop.getProperty("populationSize"));
			MA_GENERATIONS = Integer.parseInt(prop.getProperty("generations"));
			MAX_LABOUR[0] = Integer.parseInt(prop.getProperty("labourSkilled"));
			MAX_LABOUR[1] = Integer.parseInt(prop.getProperty("labourSemiSkilled"));
			MAX_LABOUR[2] = Integer.parseInt(prop.getProperty("labourUnskilled"));
			System.out.format("Shift Duration: %dhrs\n", SHIFT_DURATION);
			System.out.println("SIM COUNT:"+SIMULATION_COUNT);
			System.out.format("Labour: %d %d %d\n", MAX_LABOUR[0], MAX_LABOUR[1], MAX_LABOUR[2]);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
}

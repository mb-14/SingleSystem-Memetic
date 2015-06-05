package org.isw;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;



/*
 * Scheduling listens on 8888
 * Machine listens on 8889
 */

public class Macros {

	public static int TIME_SCALE_FACTOR = 1;
	public static int SHIFT_DURATION = 8;
	public static int SIMULATION_COUNT = 1000;
	public static void loadMacros(){
		
		try {
			Properties prop = new Properties();
			InputStream input = new FileInputStream("config.properties");
			prop.load(input);
			TIME_SCALE_FACTOR = Integer.parseInt(prop.getProperty("scaleFactor"));
			SHIFT_DURATION = Integer.parseInt(prop.getProperty("shiftDuration"));
			SIMULATION_COUNT = Integer.parseInt(prop.getProperty("simulationCount"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
}

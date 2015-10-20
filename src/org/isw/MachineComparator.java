package org.isw;

import java.util.ArrayList;
import java.util.Comparator;


public class MachineComparator implements Comparator<ArrayList<SimulationResult>> {

	@Override
	public int compare(ArrayList<SimulationResult> o1,
			ArrayList<SimulationResult> o2) {
		return Integer.compare(o1.get(0).id,o2.get(0).id);

	}

}

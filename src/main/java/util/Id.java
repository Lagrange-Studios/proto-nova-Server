package util;

import java.util.Set;

public class Id {
	public static int getNewId(Set<Integer> keySet) {
		int id = 1;
		
		while (true) {
			if (keySet.contains(id)) {
				id++;
			}
			else {
				break;
			}
		}
		
		return id;
	}
}

package name.drahflow.utilator;

import java.util.*;

public class Distribution {
	public static class Entry {
		public int priority;
	}

	public static class Muldays extends Entry {
		public Set<String> days;
		public int value;

		public String toString() {
			StringBuilder res = new StringBuilder();
			res.append(priority + "muldays:");
			boolean any = false;
			for(String day: days) {
				if(any) res.append(",");
				res.append(day);
				any = true;
			}
			res.append(";" + value);
			return res.toString();
		}
	}

	public static Muldays parseMuldays(String spec) {
		Muldays r = new Muldays();
		r.priority = Integer.parseInt(spec.substring(0, 1));
		spec = spec.split(":")[1];
		String[] parts = spec.split(";");
		r.value = Integer.parseInt(parts[1]);

		r.days = new TreeSet<String>();
		for(String day: parts[0].split(",")) {
			r.days.add(day);
		}

		return r;
	}

	public static class Mulhours extends Entry {
		public Set<String> hours;
		public int value;

		public String toString() {
			StringBuilder res = new StringBuilder();
			res.append(priority + "mulhours:");
			boolean any = false;
			for(String hour: hours) {
				if(any) res.append(",");
				res.append(hour);
				any = true;
			}
			res.append(";" + value);
			return res.toString();
		}
	}

	public static Mulhours parseMulhours(String spec) {
		Mulhours r = new Mulhours();
		r.priority = Integer.parseInt(spec.substring(0, 1));
		spec = spec.split(":")[1];
		String[] parts = spec.split(";");
		r.value = Integer.parseInt(parts[1]);

		r.hours = new TreeSet<String>();
		for(String hour: parts[0].split(",")) {
			r.hours.add(hour);
		}

		return r;
	}

}

package name.drahflow.utilator;

import java.util.*;
import android.util.*;
import android.content.*;
import android.widget.*;

import static name.drahflow.utilator.Util.*;

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
		spec = spec.split(":", 2)[1];
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
		spec = spec.split(":", 2)[1];
		String[] parts = spec.split(";");
		r.value = Integer.parseInt(parts[1]);

		r.hours = new TreeSet<String>();
		for(String hour: parts[0].split(",")) {
			r.hours.add(hour);
		}

		return r;
	}

	public static float calculateImportance(Context ctx, Database db, Date time, Map<String, Object> task) {
		float timeEstimate;

		try {
			final int secondsTaken = loadInt(task, "seconds_taken");
			final int status = loadInt(task, "status");
			if(status > 0 && secondsTaken > 0) {
				timeEstimate = (float)secondsTaken * status / 100;
			} else {
				timeEstimate = (float)loadInt(task, "seconds_estimate");
			}

			float utility = calculateTimeDistribution(ctx, time, 0,
					loadStringColumn(db.loadTaskUtilities(loadString(task, "gid")), "distribution")) / 1000.0f;
			float likelyhoodTime = calculateTimeDistribution(ctx, time, 990,
					loadStringColumn(db.loadTaskLikelyhoodTime(loadString(task, "gid")), "distribution")) / 1000.0f;

			// Log.i("Utilator", "Task: " + loadString(task, "title"));
			// Log.i("Utilator", "  timeEstimate: " + timeEstimate);
			// Log.i("Utilator", "  utility: " + utility);
			// Log.i("Utilator", "  likelyhoodTime: " + likelyhoodTime);

			return utility * likelyhoodTime / timeEstimate;
		} catch(Exception e) {
			e.printStackTrace();
			Toast toast = Toast.makeText(ctx, "Error in database: " + e.toString(), Toast.LENGTH_SHORT);
			toast.show();

			return 999999;
		}
	}

	private static class SortedList {
		List<String> list;
	}

	public static void compileDistributions(Map<String, Object> likelyhoods) {
		for(String key: likelyhoods.keySet()) {
			Object o = likelyhoods.get(key);
			if(o instanceof List) {
				List<String> entries = (List<String>)o;

				if(entries.size() != 1) {
					Collections.sort(entries);

					SortedList sl = new SortedList();
					sl.list = entries;
					likelyhoods.put(key, sl);
					continue;
				}

				String[] data = entries.get(0).substring(1).split(":", 2);

				if(!data[0].equals("constant")) continue;
				likelyhoods.put(key, Float.parseFloat(data[1]));
			}
		}
	}

	public static float calculateImportance(Context ctx, Database db, Date time, Task task) {
		try {
			float timeEstimate;

			if(task.status > 0 && task.seconds_taken > 0) {
				timeEstimate = (float)task.seconds_taken * task.status / 100;
			} else {
				timeEstimate = (float)task.seconds_estimate;
			}

			float utility = calculateTimeDistribution(ctx, time, 0, task.task_utility);
			float likelyhoodTime = calculateTimeDistribution(ctx, time, 990, task.task_likelyhood_time);

			// Log.i("Utilator", "Task: " + loadString(task, "title"));
			// Log.i("Utilator", "  timeEstimate: " + timeEstimate);
			// Log.i("Utilator", "  utility: " + utility);
			// Log.i("Utilator", "  likelyhoodTime: " + likelyhoodTime);

			return utility * likelyhoodTime / timeEstimate / 1000000f;
		} catch(Exception e) {
			e.printStackTrace();
			Toast toast = Toast.makeText(ctx, "Error in database: " + e.toString(), Toast.LENGTH_SHORT);
			toast.show();

			return 999999;
		}
	}

	private static Date lastIsoTimeDate = null;
	private static String lastIsoTime = null;

	private static float calculateTimeDistribution(Context ctx, Date time, int d, Object distr) {
		List<String> distribution;

		if(distr == null) {
			return d;
		} else if(distr instanceof Float) {
			return (Float)distr;
		} else if(distr instanceof SortedList) {
			distribution = ((SortedList)distr).list;
		} else {
			distribution = (List<String>)distr;
			if(distribution.isEmpty()) return d;

			Collections.sort(distribution);
		}

		int value = 0;

		for(String entry: distribution) {
			int colon = entry.indexOf(':');
			String[] data = new String[] { entry.substring(1, colon), entry.substring(colon + 1) };

			if(data[0].equals("constant")) {
				value += Integer.parseInt(data[1]);
			} else if(data[0].equals("mulrange")) {
				int semicolon1 = data[1].indexOf(';');
				int semicolon2 = data[1].indexOf(';', semicolon1 + 1);
				String[] parts  = new String[] {
					data[1].substring(0, semicolon1),
					data[1].substring(semicolon1 + 1, semicolon2),
					data[1].substring(semicolon2 + 1)
				};

				if(time != lastIsoTimeDate) {
					lastIsoTime = isoFullDate(time);
					lastIsoTimeDate = time;
				}

				if(parts[0].compareTo(lastIsoTime) <= 0 && parts[1].compareTo(lastIsoTime) > 0) {
					value = value * Integer.parseInt(parts[2]) / 1000;
				}
			} else if(data[0].equals("mulhours")) {
				Mulhours e = parseMulhours(entry);
				GregorianCalendar cal = new GregorianCalendar();
				cal.setTime(time);
				int minutesSinceDayStart = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

				boolean matched = false;
				for(String hour: e.hours) {
					String[] parts = hour.split("\\+");
					String[] parts2 = parts[0].split(":");
					int start = Integer.parseInt(parts2[0]) * 60 + Integer.parseInt(parts2[1]);
					int end = start + Integer.parseInt(parts[1]);

					if(start <= minutesSinceDayStart && minutesSinceDayStart < end) {
						matched = true;
						break;
					}
				}

				value = value * (matched? e.value: 0) / 1000;
			} else if(data[0].equals("muldays")) {
				Muldays e = parseMuldays(entry);
				final String currentDay = isoDate(time);
				Log.i("Utilator", "currentDay: " + currentDay);

				boolean matched = false;
				for(String day: e.days) {
					Log.i("Utilator", "entry day: " + day);
					if(currentDay.equals(day)) {
						matched = true;
						break;
					}
				}

				Log.i("Utilator", "matched: " + matched);
				Log.i("Utilator", "value: " + e.value);
				value = value * (matched? e.value: 0) / 1000;
			} else {
				throw new IllegalArgumentException("Unknown time distribution spec: " + entry);
			}
		}

		return value;
	}
}

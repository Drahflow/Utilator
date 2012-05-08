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
			if(loadInt(task, "status") > 0 && loadInt(task, "time_taken") > 0) {
				timeEstimate = (float)loadInt(task, "seconds_taken") * loadInt(task, "status") / 100;
			} else {
				timeEstimate = (float)loadInt(task, "seconds_estimate");
			}

			float utility = calculateTimeDistribution(ctx, time, 0, db.loadTaskUtilities(loadString(task, "gid"))) / 1000.0f;
			float likelyhoodTime = calculateTimeDistribution(ctx, time, 990, db.loadTaskLikelyhoodTime(loadString(task, "gid"))) / 1000.0f;

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

	public static float calculateTimeDistribution(Context ctx, Date time, int d, List<Map<String, Object>> distribution) {
		if(distribution.isEmpty()) return d;

		final String isoTime = isoFullDate(time);
		int value = 0;

		Set<String> entries = new TreeSet<String>();
		for(Map<String, Object> entry: distribution) {
			entries.add(loadString(entry, "distribution"));
		}

		for(String entry: entries) {
			String data = entry.substring(1);

			if(data.matches("^constant:.*")) {
				value += Integer.parseInt(data.split(":", 2)[1]);
			} else if(data.matches("^mulrange:.*")) {
				String parts[] = data.split(":", 2)[1].split(";");
				if(parts[0].compareTo(isoTime) <= 0 && parts[1].compareTo(isoTime) > 0) {
					value = value * Integer.parseInt(parts[2]) / 1000;
				}
			} else if(data.matches("^mulhours:.*")) {
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
			} else if(data.matches("^muldays:.*")) {
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

package name.drahflow.utilator;

import java.util.*;
import android.util.*;
import android.content.*;
import android.widget.*;

import static name.drahflow.utilator.Util.*;

public class DistributionUtil {
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

			FakeCalendar cal = new FakeCalendar();
			cal.setTime(time);

			float utility = TimeDistribution.compile(0,
					loadStringColumn(db.loadTaskUtilities(loadString(task, "gid")), "distribution")).evaluate(time.getTime(), cal);
			float likelyhoodTime = TimeDistribution.compile(990,
					loadStringColumn(db.loadTaskLikelyhoodTime(loadString(task, "gid")), "distribution")).evaluate(time.getTime(), cal);

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

	private static class SortedList {
		List<String> list;
	}
}

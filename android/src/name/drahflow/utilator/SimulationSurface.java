package name.drahflow.utilator;

import android.app.*;
import android.graphics.*;
import android.widget.*;
import android.view.*;
import android.view.inputmethod.*;
import android.content.*;
import android.util.*;
import java.util.*;

import static name.drahflow.utilator.Util.*;

abstract public class SimulationSurface extends WidgetView {
	protected Utilator ctx;

	protected Calendar windowStart = new GregorianCalendar();
	protected List<Task> allTasks;

	protected List<Task> schedule;
	protected List<Long> scheduleTime;
	protected List<Integer> importance;
	protected int minImportance;
	protected int maxImportance;

	protected Integer currentSelection;
	protected int currentSelectionX;
	protected int currentSelectionY;

	public SimulationSurface(Utilator ctx) {
		this(ctx, new Date());
	}

	public SimulationSurface(Utilator ctx, Date windowStart) {
		super(ctx);
		this.ctx = ctx;

		this.windowStart = new GregorianCalendar();
		this.windowStart.setTime(windowStart);
		this.windowStart.set(Calendar.HOUR_OF_DAY, 0);
		this.windowStart.set(Calendar.MINUTE, 0);
		this.windowStart.set(Calendar.SECOND, 0);

		allTasks = ctx.db.loadAllTasks();
		Map<String, List<String>> taskUtilities = ctx.db.loadManyTaskUtilities("WHERE status < 100");
		Map<String, List<String>> taskLikelyhoodTime = ctx.db.loadManyTaskLikelyhoodTime("WHERE status < 100");

		Map<String, Date> dateCache = new HashMap<String, Date>();

		for(Task t: allTasks) {
			t.task_utility = TimeDistribution.compile(0, taskUtilities.get(t.gid), dateCache);
			t.task_likelyhood_time = TimeDistribution.compile(990, taskLikelyhoodTime.get(t.gid), dateCache);
		}

		calculateSchedule();
	}

	protected abstract int timeRange();

	protected void calculateSchedule() {
		Log.i("Utilator", "Simulation: start at " + new Date());
		schedule = new ArrayList<Task>();
		scheduleTime = new ArrayList<Long>();
		importance = new ArrayList<Integer>();
		maxImportance = 0;
		minImportance = 0;

		Map<String, Integer> secondsUsed = new HashMap<String, Integer>();

		final Date startDate = new Date();
		final Date endDate = new Date(windowStart.getTime().getTime() + timeRange() * 1000l);

		List<Task> nonConstantTasks = new ArrayList<Task>();
		List<Task> constantTasks = new ArrayList<Task>();
		for(Task t: allTasks) {
			if(t.hasConstantImportance(startDate, endDate)) {
				constantTasks.add(t);
			} else {
				nonConstantTasks.add(t);
			}
		}

		final GregorianCalendar startCal = new GregorianCalendar();

		Collections.sort(constantTasks, new Comparator<Task>() {
			public int compare(Task a, Task b) {
				return b.calculateImportance(startDate, startCal) - a.calculateImportance(startDate, startCal);
			}
		});

		List<Integer> constantValues = new ArrayList<Integer>();
		for(Task t: constantTasks) {
			constantValues.add(t.calculateImportance(startDate, startCal));
		}
		if(constantValues.isEmpty()) {
			Toast toast = Toast.makeText(ctx, "No constant tasks available.", Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		Task[] relevantTasks = nonConstantTasks.toArray(new Task[0]);
		int relevantTaskCount = relevantTasks.length;
		final long windowStartTime = windowStart.getTime().getTime();
		final long endTime = endDate.getTime();

		Log.i("Utilator", "Simulation: start date " + startDate);
		Log.i("Utilator", "Simulation: end date " + endDate);

		final GregorianCalendar tCal = new GregorianCalendar();
		tCal.setTime(startDate);

		for(Date t = new Date(startDate.getTime()); t.getTime() < endTime; ) {
			int bestIndex = -1;
			int bestImportance = constantValues.get(0);

			for(int j = 0; j < relevantTaskCount; ++j) {
				final int importance = relevantTasks[j].calculateImportance(t, tCal);

				if(importance > bestImportance) {
					bestIndex = j;
					bestImportance = importance;
				}
			}

			Task bestTask;

			if(bestIndex < 0) {
				bestTask = constantTasks.get(0);
			} else {
				bestTask = relevantTasks[bestIndex];
			}

			schedule.add(bestTask);
			scheduleTime.add(t.getTime());
			importance.add(bestImportance);
			if(t.getTime() > windowStartTime) {
				if(bestImportance < minImportance) {
					minImportance = bestImportance;
				}
				if(bestImportance > maxImportance) {
					maxImportance = bestImportance;
				}
			}

			int delta = bestTask.seconds_estimate;
			if(delta > 15 * 60) delta = 15 * 60;

			if(secondsUsed.get(bestTask.gid) != null) {
				secondsUsed.put(bestTask.gid, secondsUsed.get(bestTask.gid) + delta);
			} else {
				secondsUsed.put(bestTask.gid, delta);
			}

			if(secondsUsed.get(bestTask.gid) >= bestTask.seconds_estimate) {
				if(bestTask == constantTasks.get(0)) {
					if(constantTasks.size() > 1) {
						constantTasks.remove(0);
						constantValues.remove(0);
					}
				} else {
					relevantTasks[bestIndex] = relevantTasks[--relevantTaskCount];
				}
			}

			t.setTime(t.getTime() + delta * 1000);
			// FIXME: This takes way too much CPU
			tCal.add(Calendar.SECOND, delta);
		}

		Log.i("Utilator", "Simulation: schedule length " + schedule.size());
		Log.i("Utilator", "Simulation: end at " + new Date());
	}
}

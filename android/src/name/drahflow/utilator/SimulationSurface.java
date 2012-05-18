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

class SimulationSurface extends WidgetView {
	private Utilator ctx;

	protected Calendar start = new GregorianCalendar();
	protected List<Task> allTasks;
	protected List<Task> schedule;

	public SimulationSurface(Utilator ctx) {
		super(ctx);
		this.ctx = ctx;

		allTasks = ctx.db.loadAllTasks();
		Map<String, List<String>> taskUtilities = ctx.db.loadManyTaskUtilities("WHERE status < 100");
		Map<String, List<String>> taskLikelyhoodTime = ctx.db.loadManyTaskLikelyhoodTime("WHERE status < 100");

		Map<String, Date> dateCache = new HashMap<String, Date>();

		for(Task t: allTasks) {
			t.task_utility = TimeDistribution.compile(0, taskUtilities.get(t.gid), dateCache);
			t.task_likelyhood_time = TimeDistribution.compile(990, taskLikelyhoodTime.get(t.gid), dateCache);
		}

		start.set(Calendar.HOUR_OF_DAY, 0);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		calculateSchedule();
	}

	public void onDraw(Canvas c) {
		super.onDraw(c);

		int y = 20;
		for(int i = 0; i < 7; ++i) {
			final String isoDate = isoDateFormat.format(start.getTime());
			c.drawText(isoDate, 0, y, PRIMARY_COLOR);
			start.add(Calendar.DAY_OF_MONTH, 1);

			for(int h = 1; h < 24; ++h) {
				final int x = getWidth() * h / 24;
				c.drawLine(x, y + 4, x, y + 24, PRIMARY_COLOR);
			}

			y += 40;
		}
	}

	@Override protected void setupWidgets() {
		super.setupWidgets();

		// FIXME: put stuff here of stop inheriting from WidgetView
	}

	protected void calculateSchedule() {
		Log.i("Utilator", "Simulation: start at " + new Date());
		schedule = new ArrayList<Task>();
		Calendar it = (Calendar)start.clone();

		Map<String, Integer> secondsUsed = new HashMap<String, Integer>();

		final Date startDate = it.getTime();
		final Date endDate = new Date(startDate.getTime() + 7 * 24 * 60 * 60 * 1000);

		List<Task> nonConstantTasks = new ArrayList<Task>();
		List<Task> constantTasks = new ArrayList<Task>();
		for(Task t: allTasks) {
			if(t.task_utility.isConstant(startDate, endDate)) {
				constantTasks.add(t);
			} else {
				nonConstantTasks.add(t);
			}
		}

		List<Integer> constantValues = new ArrayList<Integer>();
		for(Task t: constantTasks) {
			constantValues.add(t.calculateImportance(startDate));
		}

		Task[] relevantTasks = nonConstantTasks.toArray(new Task[0]);
		int relevantTaskCount = relevantTasks.length;

		final Date t = it.getTime();
		for(int i = 0; i < 7 * 24 * 60 * 60; ) {
			int bestIndex = -1;
			int bestImportance = constantValues.get(0);

			for(int j = 0; j < relevantTaskCount; ++j) {
				final int importance = relevantTasks[j].calculateImportance(t);

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

			i += delta;
			t.setTime(t.getTime() + delta * 1000);
		}

		Log.i("Utilator", "Simulation: end at " + new Date());
	}
}

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
	protected Map<String, Object> taskUtilities;
	protected Map<String, Object> taskLikelyhoodTime;
	protected List<Task> schedule;

	public SimulationSurface(Utilator ctx) {
		super(ctx);
		this.ctx = ctx;

		allTasks = ctx.db.loadAllTasks();
		taskUtilities = ctx.db.loadManyTaskUtilities("WHERE status < 100");
		taskLikelyhoodTime = ctx.db.loadManyTaskLikelyhoodTime("WHERE status < 100");

		Distribution.compileDistributions(taskUtilities);
		Distribution.compileDistributions(taskLikelyhoodTime);

		for(Task t: allTasks) {
			t.task_utility = taskUtilities.get(t.gid);
			t.task_likelyhood_time = taskLikelyhoodTime.get(t.gid);
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

		List<Task> relevantTasks = new ArrayList<Task>(allTasks);
		Map<String, Integer> secondsUsed = new HashMap<String, Integer>();

		for(int i = 0; i < 7 * 24 * 60 * 60; ) {
			Task bestTask = null;
			float bestImportance = 0;
			final Date t = it.getTime();

			for(Task task: relevantTasks) {
				float importance = Distribution.calculateImportance(ctx, ctx.db, t, task);

				if(importance > bestImportance) {
					bestTask = task;
					bestImportance = importance;
				}
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
				relevantTasks.remove(bestTask);
			}

			i += delta;
			it.add(Calendar.SECOND, delta);

			Log.i("Utilator", "Simulation: " + it.getTime() + ": " + bestTask.title);
		}

		Log.i("Utilator", "Simulation: end at " + new Date());
	}
}

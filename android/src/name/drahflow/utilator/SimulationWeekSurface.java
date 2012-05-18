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

class SimulationWeekSurface extends WidgetView {
	private Utilator ctx;

	protected Calendar start = new GregorianCalendar();
	protected List<Task> allTasks;

	protected List<Task> schedule;
	protected List<Long> scheduleTime;
	protected List<Integer> importance;
	protected int minImportance;
	protected int maxImportance;

	protected Integer currentSelection;
	protected int currentSelectionX;
	protected int currentSelectionY;

	public SimulationWeekSurface(Utilator ctx) {
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

		Paint taskColor = new Paint();
		taskColor.setStyle(Paint.Style.FILL);
		taskColor.setAntiAlias(true);
		taskColor.setColor(0xffff0000);

		final int importanceDifference = maxImportance - minImportance + 1;
		Date drawStart = start.getTime();

		if(currentSelection != null) {
			c.drawLine(0, currentSelectionY, getWidth(), currentSelectionY, SECONDARY_COLOR);
			c.drawLine(currentSelectionX, 20, currentSelectionX, getHeight(), SECONDARY_COLOR);
		}

		int y = 20;
		for(int i = 0; i < 7; ++i) {
			final String isoDate = isoDateFormat.format(drawStart);
			c.drawText(isoDate, 0, y, PRIMARY_COLOR);

			for(int h = 1; h < 24; ++h) {
				final int x = getWidth() * h / 24;
				c.drawLine(x, y + 4, x, y + 24, PRIMARY_COLOR);
			}

			drawStart.setTime(drawStart.getTime() + 86400 * 1000);
			y += 40;
		}

		long s = start.getTime().getTime();
		long e = s + 86400 * 1000 * 7;
		Task last = null;
		for(int j = 0; j < scheduleTime.size() - 1; ++j) {
			long ts = scheduleTime.get(j);
			long te = scheduleTime.get(j + 1);
			if(ts > e) break;

			int ty = (int)(20 + ((ts - s) / 1000) / 86400 * 40);
			int xs = (int)(getWidth() * (((ts - s) / 1000) % 86400) / 86400);
			int xe = (int)(getWidth() * (((te - s) / 1000) % 86400) / 86400);
			if(xe < xs) xe = getWidth();

			c.drawRect(xs, ty + 20 - 12 * (importance.get(j) - minImportance) / importanceDifference, xe, ty + 24, taskColor);
		}

		if(currentSelection != null) {
			c.drawText(schedule.get(currentSelection).title, 100, 20, PRIMARY_COLOR);
			c.drawText(importance.get(currentSelection) * 0.0000036f + " u/h", 100, 260, PRIMARY_COLOR);
		}
	}

	public boolean onTouchEvent(MotionEvent e) {
		int x = (int)e.getX(0);
		int y = (int)e.getY(0);

		switch(e.getActionMasked()) {
			case MotionEvent.ACTION_UP:
				if(currentSelection != null) {
					ctx.switchToTask(schedule.get(currentSelection).gid);
					return true;
				}
				break;

			default:
				Integer newSelection = null;
				if((y - 20) % 40 > 8 && (y - 20) % 40 < 24) {
					int day = (y - 20) / 40;
					long t = start.getTime().getTime() + day * 86400 * 1000;
					t += 86400 * x / getWidth() * 1000;

					for(int i = 1; i < scheduleTime.size(); ++i) {
						if(t > scheduleTime.get(i)) {
							newSelection = i - 1;
						}
					}
				}

				if(newSelection != currentSelection) {
					currentSelection = newSelection;
					currentSelectionX = x;
					currentSelectionY = y;
					invalidate();
				}
				return true;
		}

		return false;
	}

	protected void calculateSchedule() {
		Log.i("Utilator", "Simulation: start at " + new Date());
		schedule = new ArrayList<Task>();
		scheduleTime = new ArrayList<Long>();
		importance = new ArrayList<Integer>();
		maxImportance = 0;
		minImportance = 999999;

		Map<String, Integer> secondsUsed = new HashMap<String, Integer>();

		final Date startDate = new Date();
		final Date endDate = new Date(startDate.getTime() + 7 * 24 * 60 * 60 * 1000);

		List<Task> nonConstantTasks = new ArrayList<Task>();
		List<Task> constantTasks = new ArrayList<Task>();
		for(Task t: allTasks) {
			if(t.hasConstantImportance(startDate, endDate)) {
				constantTasks.add(t);
			} else {
				nonConstantTasks.add(t);
			}
		}

		Collections.sort(constantTasks, new Comparator<Task>() {
			public int compare(Task a, Task b) {
				return b.calculateImportance(startDate) - a.calculateImportance(startDate);
			}
		});

		List<Integer> constantValues = new ArrayList<Integer>();
		for(Task t: constantTasks) {
			constantValues.add(t.calculateImportance(startDate));
		}
		if(constantValues.isEmpty()) {
			Toast toast = Toast.makeText(ctx, "No constant tasks available.", Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		Task[] relevantTasks = nonConstantTasks.toArray(new Task[0]);
		int relevantTaskCount = relevantTasks.length;

		final Date t = new Date(startDate.getTime());
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
			scheduleTime.add(t.getTime());
			importance.add(bestImportance);
			if(bestImportance < minImportance) {
				minImportance = bestImportance;
			}
			if(bestImportance > maxImportance) {
				maxImportance = bestImportance;
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

			i += delta;
			t.setTime(t.getTime() + delta * 1000);
		}

		Log.i("Utilator", "Simulation: end at " + new Date());
	}
}

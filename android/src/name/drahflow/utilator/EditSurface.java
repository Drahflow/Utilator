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

class EditSurface extends WidgetView {
	private Utilator ctx;

	private Map<String, Object> currentTask;
	private List<Map<String, Object>> currentTaskUtilities;
	private List<Map<String, Object>> currentTaskLikelyhoodTime;

	public EditSurface(Utilator ctx) {
		super(ctx);
		this.ctx = ctx;
	}

	public void onDraw(Canvas c) {
		super.onDraw(c);

		if(currentTask == null) return;

		int y = 16;
		for(String line: loadString(currentTask, "title").split("\n")) {
			y = drawWrapped(c, line, 10, 320, y, PRIMARY_COLOR) + 20;
		}

		for(String line: loadString(currentTask, "description").split("\n")) {
			y = drawWrapped(c, line, 10, 320, y, PRIMARY_COLOR) + 20;
		}

		String estStr = "Est. time: " + humanTime(loadInt(currentTask, "seconds_estimate"));
		y = drawWrapped(c, estStr, 10, 320, y, PRIMARY_COLOR) + 20;

		for(Map<String, Object> entry: currentTaskUtilities) {
			y = drawWrapped(c, "U: " + loadString(entry, "distribution"), 10, 320, y, PRIMARY_COLOR) + 20;
		}

		for(Map<String, Object> entry: currentTaskLikelyhoodTime) {
			y = drawWrapped(c, "T: " + loadString(entry, "distribution"), 10, 320, y, PRIMARY_COLOR) + 20;
		}
	}

	public void setTask(String gid) {
		currentTask = ctx.db.loadTask(gid);
		currentTaskUtilities = ctx.db.loadTaskUtilities(gid);
		currentTaskLikelyhoodTime = ctx.db.loadTaskLikelyhoodTime(gid);

		Log.i("Utilator", "EditSurface, loaded task: " + currentTask);
		Log.i("Utilator", "EditSurface, loaded utilities: " + currentTaskUtilities);
		Log.i("Utilator", "EditSurface, loaded time likelyhood: " + currentTaskLikelyhoodTime);
		invalidate();
	}

	@Override protected void setupWidgets() {
		super.setupWidgets();

		widgets.add(new Button() {
			{
				activateZone = new Rect(getWidth() * 8 / 10, 0, getWidth() * 9 / 10, 80);
				title = "text";
				actions = new Rect[] {
					new Rect(0, 0, getWidth() * 8 / 10, 40),
					new Rect(0, 40, getWidth() * 8 / 10, 80)
				};
				actionNames = new String[] {
					"title", "description"
				};
			}

			@Override public void invokeAction(int n) {
				switch(n) {
					case 0: {
						final EditText input = new EditText(ctx);
						input.setText(loadString(currentTask, "title"));

						new AlertDialog.Builder(ctx)
								.setTitle("Edit title")
								.setView(input)
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											String title = input.getText().toString(); 
											Log.i("Utilator", "EditSurface, title changed to: " + title);

											ctx.db.setTitle(loadString(currentTask, "gid"), title);
											ctx.db.touchTask(loadString(currentTask, "gid"));
											setTask(loadString(currentTask, "gid"));
										}
								}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) { }
								}).show();

						InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
					} break;
					case 1: {
						final EditText input = new EditText(ctx);
						input.setText(loadString(currentTask, "description"));

						new AlertDialog.Builder(ctx)
								.setTitle("Edit description")
								.setView(input)
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											String description = input.getText().toString(); 
											Log.i("Utilator", "EditSurface, description changed to: " + description);

											ctx.db.setDescription(loadString(currentTask, "gid"), description);
											ctx.db.touchTask(loadString(currentTask, "gid"));
											setTask(loadString(currentTask, "gid"));
										}
								}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) { }
								}).show();

						InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
					} break;
				}
			}
		});
		widgets.add(new Button() {
			{
				activateZone = new Rect(getWidth() * 9 / 10, 0, getWidth(), 80);
				title = "est";
				actions = new Rect[] {
					new Rect(0, 0, getWidth() * 9 / 10, getHeight()),
				};
				actionNames = new String[] {
					"0s"
				};
			}

			private long selectedTime = 0;

			@Override public void onMove(int x, int y) {
				selectedTime = exponentialMap(y, getHeight(), x, getWidth() * 9 / 10);
				actionNames[0] = humanTime(selectedTime);
			}

			@Override public void invokeAction(int n) {
				switch(n) {
					case 0:
						Log.i("Utilator", "EditSurface, time estimated set: " + selectedTime);
						ctx.db.setSecondsEstimate(loadString(currentTask, "gid"), selectedTime);
						ctx.db.touchTask(loadString(currentTask, "gid"));
						setTask(loadString(currentTask, "gid"));
				}
			}
		});
		widgets.add(new Button() {
			{
				activateZone = new Rect(getWidth() * 9 / 10, 80, getWidth(), 160);
				title = "const U";
				actions = new Rect[] {
					new Rect(0, 0, getWidth() * 9 / 10, getHeight()),
				};
				actionNames = new String[] {
					"set to"
				};
			}

			private long selectedUtility = 0;

			@Override public void onMove(int x, int y) {
				selectedUtility = exponentialMap(y, getHeight(), x, getWidth() * 9 / 10);
				actionNames[0] = (float)selectedUtility / 1000 + " u";
			}

			@Override public void invokeAction(int n) {
				switch(n) {
					case 0:
						String entry = "0constant:" + selectedUtility;
						Log.i("Utilator", "EditSurface, utility added: " + entry);
						ctx.db.addUtility(loadString(currentTask, "gid"), entry);
						ctx.db.touchTask(loadString(currentTask, "gid"));
						setTask(loadString(currentTask, "gid"));
				}
			}
		});
		widgets.add(new Button() {
			{
				activateZone = new Rect(getWidth() * 8 / 10, 80, getWidth() * 9 / 10, 160);
				title = "clr";
				actions = new Rect[] {
					new Rect(0, 0, getWidth() * 1 / 10, getHeight()),
				};
				actionNames = new String[] {
					"all"
				};
			}

			@Override public void invokeAction(int n) {
				switch(n) {
					case 0:
						Log.i("Utilator", "EditSurface, utilities cleared");
						ctx.db.deleteUtilities(loadString(currentTask, "gid"));
						ctx.db.touchTask(loadString(currentTask, "gid"));
						setTask(loadString(currentTask, "gid"));
				}
			}
		});
		widgets.add(new Button() {
			{
				activateZone = new Rect(getWidth() * 9 / 10, 160, getWidth(), 240);
				title = "day";
				actions = new Rect[] {
					new Rect(0, 0, getWidth() * 9 / 10, getHeight()),
				};
				actionNames = new String[] {
					"1970-01-01"
				};
			}

			int selectedDay = 1;
			int selectedMonth = 1;
			int selectedYear = 0;
			GregorianCalendar cal = new GregorianCalendar();

			@Override public void onMove(int x, int y) {
				selectedYear = cal.get(Calendar.YEAR);
				selectedMonth = y * 12 / getHeight() + 1;
				selectedDay = x * 31 / (getWidth() * 9 / 10) + 1;

				if(selectedMonth < cal.get(Calendar.MONTH) + 1) {
					++selectedYear;
				}

				actionNames[0] = selectedYear + "-" + selectedMonth + "-" + selectedDay;
			}

			@Override public void invokeAction(int n) {
				switch(n) {
					case 0:
						GregorianCalendar cal = new GregorianCalendar();
						cal.set(Calendar.YEAR, selectedYear);
						cal.set(Calendar.MONTH, selectedMonth - 1);
						cal.set(Calendar.DAY_OF_MONTH, selectedDay);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);

						if(currentTaskLikelyhoodTime.isEmpty()) {
							ctx.db.addLikelyhoodTime(loadString(currentTask, "gid"), "0constant:990");
						}
						boolean existed = false;
						for(Map<String, Object> entry: currentTaskLikelyhoodTime) {
							Log.i("Utilator", "EditSurface, checking: " + loadString(entry, "distribution"));
							if(loadString(entry, "distribution").matches("^.muldays:.*")) {
								DistributionUtil.Muldays e = DistributionUtil.parseMuldays(loadString(entry, "distribution"));
								e.days.add(isoDate(cal.getTime()));
								String mask = e.toString();
								Log.i("Utilator", "EditSurface, day mask updated: " + mask);
								ctx.db.updateLikelyhoodTime(loadString(entry, "id"), mask);
								ctx.db.touchTask(loadString(currentTask, "gid"));
								existed = true;
							}
						}
						if(!existed) {
							String mask = "1muldays:" + isoDate(cal.getTime()) + ";1000";
							Log.i("Utilator", "EditSurface, day mask added: " + mask);
							ctx.db.addLikelyhoodTime(loadString(currentTask, "gid"), mask);
							ctx.db.touchTask(loadString(currentTask, "gid"));
						}

						setTask(loadString(currentTask, "gid"));
				}
			}
		});
		widgets.add(new Button() {
			{
				activateZone = new Rect(getWidth() * 8 / 10, 160, getWidth() * 9 / 10, 240);
				title = "time";
				actions = new Rect[] {
					new Rect(0, 0, getWidth() * 8 / 10, getHeight()),
				};
				actionNames = new String[] {
					"00:00"
				};
			}

			int selectedHour = 0;
			int selectedMinute = 0;
			int selectedDuration = 0;
			final int SELECTABLE_MINUTES[] = new int[] { 0, 15, 30, 45 };
			final int SELECTABLE_DURATIONS[] = new int[] { 15, 30, 60, 2 * 60, 3 * 60, 5 * 60, 8 * 60, 10 * 60 };

			@Override public void onMove(int x, int y) {
				selectedHour = y * 24 / getHeight();
				selectedMinute = SELECTABLE_MINUTES[
					Math.max(0, ((x * SELECTABLE_MINUTES.length * SELECTABLE_DURATIONS.length / (getWidth() * 8 / 10)) / SELECTABLE_DURATIONS.length) % SELECTABLE_MINUTES.length)];
				selectedDuration = SELECTABLE_DURATIONS[
					Math.max(0, (x * SELECTABLE_MINUTES.length * SELECTABLE_DURATIONS.length / (getWidth() * 8 / 10)) % SELECTABLE_DURATIONS.length)];

				actionNames[0] = selectedHour + ":" + selectedMinute + "( + " + selectedDuration + ")";
			}

			@Override public void invokeAction(int n) {
				switch(n) {
					case 0:
						if(currentTaskLikelyhoodTime.isEmpty()) {
							ctx.db.addLikelyhoodTime(loadString(currentTask, "gid"), "0constant:990");
						}
						boolean existed = false;
						for(Map<String, Object> entry: currentTaskLikelyhoodTime) {
							Log.i("Utilator", "EditSurface, checking: " + loadString(entry, "distribution"));
							if(loadString(entry, "distribution").matches("^.mulhours:.*")) {
								DistributionUtil.Mulhours e = DistributionUtil.parseMulhours(loadString(entry, "distribution"));
								e.hours.add(String.format("%02d:%02d+%d", selectedHour, selectedMinute, selectedDuration));
								String mask = e.toString();
								Log.i("Utilator", "EditSurface, hour mask updated: " + mask);
								ctx.db.updateLikelyhoodTime(loadString(entry, "id"), mask);
								ctx.db.touchTask(loadString(currentTask, "gid"));
								existed = true;
							}
						}
						if(!existed) {
							String mask = String.format("1mulhours:%02d:%02d+%d;1000", selectedHour, selectedMinute, selectedDuration);
							Log.i("Utilator", "EditSurface, hour mask added: " + mask);
							ctx.db.addLikelyhoodTime(loadString(currentTask, "gid"), mask);
							ctx.db.touchTask(loadString(currentTask, "gid"));
						}

						setTask(loadString(currentTask, "gid"));
				}
			}
		});
		widgets.add(new Button() {
			{
				activateZone = new Rect(getWidth() * 7 / 10, 160, getWidth() * 8 / 10, 240);
				title = "clr";
				actions = new Rect[] {
					new Rect(0, 0, getWidth() * 1 / 10, getHeight()),
				};
				actionNames = new String[] {
					"all"
				};
			}

			@Override public void invokeAction(int n) {
				switch(n) {
					case 0:
						Log.i("Utilator", "EditSurface, time masks cleared");
						ctx.db.deleteLikelyhoodsTime(loadString(currentTask, "gid"));
						ctx.db.touchTask(loadString(currentTask, "gid"));
						setTask(loadString(currentTask, "gid"));
				}
			}
		});
	}
}

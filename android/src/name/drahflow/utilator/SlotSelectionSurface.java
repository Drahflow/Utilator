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

public class SlotSelectionSurface extends SimulationSurface {
	public int slotImportance;
	public long slotTime;
	public long slotUtility;

	public List<Long> starts = new ArrayList<Long>();
	public List<Long> ends = new ArrayList<Long>();

	public SlotSelectionSurface(Utilator ctx, long slotTime, long slotUtility) {
		super(ctx);

		this.slotTime = slotTime;
		this.slotUtility = slotUtility;

		slotImportance = (int)(slotUtility * 1000000 / slotTime);

		long start = new Date().getTime();
		long end = start + 365l * 24 * 60 * 60 * 1000;

		for(int i = 0; i < importance.size(); ++i) {
			if(importance.get(i) < slotImportance) {
				if(starts.size() == ends.size()) {
					starts.add(scheduleTime.get(i));
				} else if(scheduleTime.get(i) > starts.get(starts.size() - 1) + 8 * 60 * 60 * 1000) {
					ends.add(scheduleTime.get(i));
					starts.add(scheduleTime.get(i));
				}
			} else if(starts.size() > ends.size()) {
				if(scheduleTime.get(i) - starts.get(starts.size() - 1) > slotTime) {
					ends.add(scheduleTime.get(i));
				} else {
					starts.remove(starts.size() - 1);
				}
			} 

			if(starts.size() > 20) break;
		}

		if(starts.size() > ends.size()) {
			ends.add(scheduleTime.get(scheduleTime.size() - 1));
		}
	}

	public static Date drawDate = new Date();

	public void onDraw(Canvas c) {
		super.onDraw(c);

		for(int i = 0; i < ends.size(); ++i) {
			drawDate.setTime(starts.get(i));
			c.drawText(isoFullDate(drawDate), 4, 20 + 20 * i, PRIMARY_COLOR);

			drawDate.setTime(ends.get(i));
			String isoDate = isoFullDate(drawDate);
			c.drawText(isoDate, getWidth() - PRIMARY_COLOR.measureText(isoDate), 20 + 20 * i, PRIMARY_COLOR);
		}

		if(currentSelection != null) {
			drawDate.setTime(currentSelection);
			String isoDate = isoFullDate(drawDate);

			c.drawText(isoDate, (getWidth() - PRIMARY_COLOR.measureText(isoDate)) / 2, 20, PRIMARY_COLOR);
			c.drawText(isoDate, (getWidth() - PRIMARY_COLOR.measureText(isoDate)) / 2, getHeight() - 20, PRIMARY_COLOR);
		}
	}

	public Long currentSelection;
	private static GregorianCalendar rounding = new GregorianCalendar();

	public boolean onTouchEvent(MotionEvent e) {
		int x = (int)e.getX(0);
		int y = (int)e.getY(0);

		switch(e.getActionMasked()) {
			case MotionEvent.ACTION_UP:
				if(currentSelection != null) {
					Map<String, Object> task = new HashMap<String, Object>();
					task.put("title", "");
					task.put("description", "");
					task.put("seconds_taken", 0);
					task.put("seconds_estimate", slotTime);
					task.put("status", 0);

					String gid = ctx.db.createTask(task);
					Task t = ctx.db.loadTask(gid);
					ctx.db.touchTask(gid);

					ctx.db.addLikelyhoodTime(t, "0constant:990");
					ctx.db.addLikelyhoodTime(
							t,
							"2mulrange:1970-01-01T00:00:00" +
							";" + isoFullDate(new Date(currentSelection)) +
							";0");
					ctx.db.addUtility(gid, "0constant:" + (slotUtility + 5000 + 1000 * slotTime / 3600));

					EditSurface editor = new EditSurface(ctx);
					editor.setTask(gid);

					ctx.setContentView(editor);
				}
				break;

			default:
				currentSelection = null;

				int slot = y / 20;
				if(slot < 0 || slot >= ends.size() || x < 0 || x > getWidth()) {
					invalidate();
					break;
				}

				long start = starts.get(slot);
				long end = ends.get(slot);

				long sel = start + (end - start) * x / getWidth();
				drawDate.setTime(sel);
				rounding.setTime(drawDate);
				rounding.set(Calendar.SECOND, 0);
				rounding.set(Calendar.MINUTE, rounding.get(Calendar.MINUTE) / 15 * 15);
				currentSelection = rounding.getTime().getTime();

				invalidate();
				return true;
		}

		return false;
	}

	@Override protected int timeRange() {
		return 120 * 24 * 60 * 60;
	}

	@Override protected int timeStep() {
		return 30 * 60;
	}
}

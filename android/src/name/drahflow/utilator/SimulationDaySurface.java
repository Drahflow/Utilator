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

class SimulationDaySurface extends SimulationSurface {
	public SimulationDaySurface(Utilator ctx) {
		super(ctx);
	}

	public SimulationDaySurface(Utilator ctx, Date windowStart) {
		super(ctx, windowStart);
	}

	public void onDraw(Canvas c) {
		super.onDraw(c);

		Paint taskColor = new Paint();
		taskColor.setStyle(Paint.Style.FILL);
		taskColor.setAntiAlias(true);
		taskColor.setColor(0xffff0000);

		final int importanceDifference = maxImportance - minImportance + 1;

		long s = windowStart.getTime().getTime();
		long e = s + 86400 * 1000;
		Task last = null;
		for(int j = 0; j < scheduleTime.size() - 1; ++j) {
			long ts = scheduleTime.get(j);
			long te = scheduleTime.get(j + 1);
			if(te < s) continue;
			if(ts > e) break;

			int ty = (int)(1 + ((ts - s) / 1000) / 43200) * getHeight() / 2;
			int xs = (int)(getWidth() * (((ts - s) / 1000) % 43200) / 43200);
			int xe = (int)(getWidth() * (((te - s) / 1000) % 43200) / 43200);
			if(xe < xs) xe = getWidth();

			c.drawRect(xs, ty - (long)getHeight() / 2 * (importance.get(j) - minImportance) / importanceDifference, xe, ty, taskColor);
		}

		c.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, PRIMARY_COLOR);
		for(int i = 1; i < 12; ++i) {
			c.drawLine(getWidth() / 12 * i, 0, getWidth() / 12 * i, getHeight(), i % 3 == 0? PRIMARY_COLOR: SECONDARY_COLOR);
		}

		if(currentSelection != null) {
			c.drawLine(0, currentSelectionY, getWidth(), currentSelectionY, SECONDARY_COLOR);
			c.drawLine(currentSelectionX, 20, currentSelectionX, getHeight(), SECONDARY_COLOR);

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
				if(x > 0 && y < getHeight()) {
					long t = windowStart.getTime().getTime() + (y > getHeight() / 2? 43200 * 1000: 0);
					t += 43200 * x / getWidth() * 1000;

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
				}

				invalidate();
				return true;
		}

		return false;
	}

	@Override protected int timeRange() {
		return 24 * 60 * 60;
	}
}

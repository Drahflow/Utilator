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

class SimulationYearSurface extends SimulationSurface {
	public SimulationYearSurface(Utilator ctx) {
		super(ctx);
	}

	static final String[] monthInitial = new String[] {
		"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"
	};

	public long scrollOffset = 0;
	
	public void onDraw(Canvas c) {
		super.onDraw(c);

		Paint taskColor = new Paint();
		taskColor.setStyle(Paint.Style.FILL);
		taskColor.setAntiAlias(true);
		taskColor.setColor(0xffff0000);

		final int importanceDifference = maxImportance - minImportance + 1;
		long drawStart = windowStart.getTime().getTime() + scrollOffset;

		int y = 20;
		for(int i = 0; i < 7; ++i) {
			final String isoDate = isoDateFormat.format(drawStart);
			c.drawText(isoDate, 0, y, PRIMARY_COLOR);

			for(int h = 1; h < 24; ++h) {
				final int x = 60 + (getWidth() - 60) * h / 24;
				c.drawLine(x, y + 4, x, y + 24, h % 6 == 0? PRIMARY_COLOR: SECONDARY_COLOR);
			}

			drawStart += 86400 * 1000;
			y += 40;
		}

		long s = windowStart.getTime().getTime() + scrollOffset;
		long e = s + 86400 * 1000 * 7;

		final Paint smallFont = new Paint(PRIMARY_COLOR);
		smallFont.setTextSize(8);
		Task last = null;
		int lastTy = -1;
		float lastImportance = -1;
		int labelOffsetY = 0;

		for(int j = 0; j < scheduleTime.size() - 1; ++j) {
			long ts = scheduleTime.get(j);
			long te = scheduleTime.get(j + 1);
			if(te < s) continue;
			if(ts > e) break;

			int ty = (int)(20 + ((ts - s) / 1000) / 86400 * 40);
			int xs = 60 + (int)((getWidth() - 60) * (((ts - s) / 1000) % 86400) / 86400);
			int xe = 60 + (int)((getWidth() - 60) * (((te - s) / 1000) % 86400) / 86400);
			if(xs < 60) xs = 60;
			if(xe < xs) xe = getWidth();

			c.drawRect(xs, ty + 20 - 12l * (importance.get(j) - minImportance) / importanceDifference, xe, ty + 24, taskColor);

			if(last != schedule.get(j)) {
				if(importance.get(j) > lastImportance) {
					if(ty == lastTy) {
						labelOffsetY = labelOffsetY + 10;
					} else {
						labelOffsetY = 0;
					}

					c.drawText(isoTime(ts), 100, ty - 5 + labelOffsetY, smallFont);
					c.drawText(schedule.get(j).title, 130, ty - 5 + labelOffsetY, smallFont);

					lastTy = ty;
				}

				lastImportance = importance.get(j);
			}

			last = schedule.get(j);
		}

		if(currentSelection != null) {
			c.drawLine(60, currentSelectionY, getWidth() - 60, currentSelectionY, SECONDARY_COLOR);
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
				if(x > 60) {
					if((y - 20) % 40 > 8 && (y - 20) % 40 < 24) {
						int day = (y - 20) / 40;
						long t = windowStart.getTime().getTime() + scrollOffset + day * 86400 * 1000;
						t += 86400 * (x - 60) / (getWidth() - 60) * 1000;

						for(int i = 1; i < scheduleTime.size(); ++i) {
							if(t > scheduleTime.get(i)) {
								newSelection = i;
							}
						}
					}

					if(newSelection != currentSelection) {
						currentSelection = newSelection;
						currentSelectionX = x;
						currentSelectionY = y;
					}
				} else if(x > 30) {
					scrollOffset = (long)(y / 2) * 86400 * 1000;

					currentSelection = null;
				} else if(x > 0) {
					scrollOffset = (long)(150 + y / 2) * 86400 * 1000;

					currentSelection = null;
				}

				invalidate();
				return true;
		}

		return false;
	}

	@Override protected int timeRange() {
		return 366 * 24 * 60 * 60;
	}

	@Override protected int timeStep() {
		return 60 * 60;
	}
}

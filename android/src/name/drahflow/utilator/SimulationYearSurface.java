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

		final int monthHeight = getHeight() / 12;
		long s = start.getTime().getTime();
		long e = s + 86400 * 1000 * 366;
		Task last = null;

		for(int j = 0; j < scheduleTime.size() - 1; ++j) {
			long ts = scheduleTime.get(j);
			long te = scheduleTime.get(j + 1);
			if(ts > e) break;

			int ty = (int)(((ts - s) / 1000) / (30 * 86400) * monthHeight) + monthHeight;
			int xs = (int)(getWidth() * (((ts - s) / 1000) % (30 * 86400)) / (30 * 86400));
			int xe = (int)(getWidth() * (((te - s) / 1000) % (30 * 86400)) / (30 * 86400));
			if(xe < xs) xe = getWidth();

			c.drawRect(xs, ty - monthHeight * (importance.get(j) - minImportance) / importanceDifference, xe, ty, taskColor);
		}

		if(currentSelection != null) {
			c.drawText(schedule.get(currentSelection).title, 100, 20, PRIMARY_COLOR);
			c.drawText(importance.get(currentSelection) * 0.0000036f + " u/h", 100, 260, PRIMARY_COLOR);
		}
	}

	public boolean onTouchEvent(MotionEvent e) {
//		int x = (int)e.getX(0);
//		int y = (int)e.getY(0);
//
//		switch(e.getActionMasked()) {
//			case MotionEvent.ACTION_UP:
//				if(currentSelection != null) {
//					ctx.switchToTask(schedule.get(currentSelection).gid);
//					return true;
//				}
//				break;
//
//			default:
//				Integer newSelection = null;
//				if((y - 20) % 40 > 8 && (y - 20) % 40 < 24) {
//					int day = (y - 20) / 40;
//					long t = start.getTime().getTime() + day * 86400 * 1000;
//					t += 86400 * x / getWidth() * 1000;
//
//					for(int i = 1; i < scheduleTime.size(); ++i) {
//						if(t > scheduleTime.get(i)) {
//							newSelection = i - 1;
//						}
//					}
//				}
//
//				if(newSelection != currentSelection) {
//					currentSelection = newSelection;
//					currentSelectionX = x;
//					currentSelectionY = y;
//					invalidate();
//				}
//				return true;
//		}
//
		return false;
	}

	@Override protected int timeRange() {
		return 366 * 24 * 60 * 60;
	}
}

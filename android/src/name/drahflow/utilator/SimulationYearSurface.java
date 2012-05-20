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

	protected Date currentSelectedDate = null;

	public void onDraw(Canvas c) {
		super.onDraw(c);

		Paint taskColor = new Paint();
		taskColor.setStyle(Paint.Style.FILL);
		taskColor.setAntiAlias(true);
		taskColor.setColor(0xffff0000);

		final int monthHeight = getHeight() / 12;
		final int dayWidth = getWidth() / 31;

		final GregorianCalendar s = new GregorianCalendar();
		s.setTime(windowStart.getTime());

		Task last = null;

		int month = 0;
		int j = 0;
		for(int i = 0; i < 366; ++i) {
			final int dayOfMonth = s.get(Calendar.DAY_OF_MONTH);
			int xs = dayOfMonth * dayWidth;
			int ys = month * monthHeight;

			if(dayOfMonth == 1 || i == 0) {
				c.drawText(monthInitial[s.get(Calendar.MONTH)], 0, ys + monthHeight - 8, PRIMARY_COLOR);
			}

			c.drawRect(xs, ys, xs + dayWidth - 2, ys + monthHeight - 2,
					s.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY? PRIMARY_COLOR: SECONDARY_COLOR);

			for(int k = 0; k < 4; ++k) {
				long endTime = s.getTime().getTime() + (k + 1) * 6 * 60 * 60 * 1000;
				int maxRangeImportance = 0;

				while(j < scheduleTime.size() && scheduleTime.get(j) < endTime) {
					final int importanceJ = importance.get(j);
					if(importanceJ > maxRangeImportance) {
						maxRangeImportance = importanceJ;
					}

					++j;
				}

				int drawImportance = (int)Math.log((double)maxRangeImportance / (maxImportance + 1));
				//Log.i("Utilator", "Simulation: drawImportance " + drawImportance);

				if(drawImportance < -(dayWidth - 3)) drawImportance = -(dayWidth - 3);
				c.drawRect(xs, ys + (monthHeight - 2) * k / 4,
						xs + (dayWidth - 2) + drawImportance, ys + (monthHeight - 2) * (k + 1) / 4, taskColor);
			}

			s.add(Calendar.DAY_OF_MONTH, 1);
			if(s.get(Calendar.DAY_OF_MONTH) == 1) {
				++month;
			}
		}

		if(currentSelectedDate != null) {
			c.drawLine(0, currentSelectionY, getWidth(), currentSelectionY, SECONDARY_COLOR);
			c.drawLine(currentSelectionX, 0, currentSelectionX, getHeight(), SECONDARY_COLOR);

			c.drawText("" + currentSelectedDate, 20, 20, PRIMARY_COLOR);
			c.drawText("" + currentSelectedDate, 20, getHeight() - 20, PRIMARY_COLOR);
		}
	}

	public boolean onTouchEvent(MotionEvent e) {
		int x = (int)e.getX(0);
		int y = (int)e.getY(0);

		switch(e.getActionMasked()) {
			case MotionEvent.ACTION_UP:
				if(currentSelectedDate != null) {
					ctx.setContentView(new SimulationDaySurface(ctx, currentSelectedDate));
					return true;
				}
				return false;

			default:
				currentSelectedDate = null;

				if(x > 0 && y > 0) {
					final int monthHeight = getHeight() / 12;
					final int dayWidth = getWidth() / 31;

					final GregorianCalendar s = new GregorianCalendar();
					s.setTime(windowStart.getTime());

					int month = 0;
					for(int i = 0; i < 366; ++i) {
						final int dayOfMonth = s.get(Calendar.DAY_OF_MONTH);
						int xs = dayOfMonth * dayWidth;
						int ys = month * monthHeight;

						if(x >= xs && y >= ys && x < xs + dayWidth - 2 && y < ys + monthHeight - 2) {
							currentSelectedDate = s.getTime();
						}

						s.add(Calendar.DAY_OF_MONTH, 1);
						if(s.get(Calendar.DAY_OF_MONTH) == 1) {
							++month;
						}
					}

					currentSelectionX = x;
					currentSelectionY = y;
				}

				invalidate();
				return true;
		}
	}

	@Override protected int timeRange() {
		return 366 * 24 * 60 * 60;
	}

	@Override protected int timeStep() {
		return 60 * 60;
	}
}

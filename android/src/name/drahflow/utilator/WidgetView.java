package name.drahflow.utilator;

import android.app.*;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.util.*;
import java.util.*;

abstract class WidgetView extends View {
	static final public Paint PRIMARY_COLOR = new Paint();
	static final public Paint SECONDARY_COLOR = new Paint();
	static final public Paint ACTIVE_COLOR = new Paint();
	static final public Paint ACTIVE_COLOR2 = new Paint();
	static {
		PRIMARY_COLOR.setColor(0xff00ff00);
		PRIMARY_COLOR.setTextSize(16);
		PRIMARY_COLOR.setStyle(Paint.Style.FILL);
		PRIMARY_COLOR.setAntiAlias(true);
		SECONDARY_COLOR.setColor(0xff004000);
		SECONDARY_COLOR.setTextSize(16);
		SECONDARY_COLOR.setStyle(Paint.Style.FILL);
		SECONDARY_COLOR.setAntiAlias(true);
		ACTIVE_COLOR.setColor(0x8000ff00);
		ACTIVE_COLOR.setTextSize(16);
		ACTIVE_COLOR.setStyle(Paint.Style.FILL);
		ACTIVE_COLOR.setAntiAlias(true);
		ACTIVE_COLOR2.setColor(0x80ff0000);
		ACTIVE_COLOR2.setTextSize(16);
		ACTIVE_COLOR2.setStyle(Paint.Style.FILL);
		ACTIVE_COLOR2.setAntiAlias(true);
	}

	static abstract class Widget {
		public Rect activateZone;

		public void onDraw(Canvas c) { }
		public void onActiveDraw(Canvas c, int x, int y) { onDraw(c); }
		public void onMove(int x, int y) { }
		public void onLift(int x, int y) { }
	};

	static abstract class Button extends Widget {
		public String title;
		public Rect[] actions;
		public String[] actionNames;

		@Override public void onDraw(Canvas c) {
			c.drawRect(activateZone, SECONDARY_COLOR);
			c.drawText(title, activateZone.centerX() - PRIMARY_COLOR.measureText(title) / 2, activateZone.centerY(), PRIMARY_COLOR);
		}

		@Override public void onActiveDraw(Canvas c, int x, int y) {
			c.drawRect(activateZone, ACTIVE_COLOR);
			c.drawText(title, activateZone.centerX() - PRIMARY_COLOR.measureText(title) / 2, activateZone.centerY(), PRIMARY_COLOR);

			for(int i = 0; i < actions.length; ++i) {
				c.drawRect(actions[i], actions[i].contains(x, y)? ACTIVE_COLOR: SECONDARY_COLOR);

				final String name = actionNames[i];
				float textStart = actions[i].centerX() - PRIMARY_COLOR.measureText(name) / 2;
				if(textStart < 0) textStart = 0;

				c.drawText(name, textStart, actions[i].centerY(), PRIMARY_COLOR);

				if(actions[i].height() > 100) {
					c.drawText(name, textStart, actions[i].top + 20, PRIMARY_COLOR);
					c.drawText(name, textStart, actions[i].bottom - 5, PRIMARY_COLOR);
				}
			}
		}

		@Override public void onLift(int x, int y) {
			for(int i = 0; i < actions.length; ++i) {
				if(actions[i].contains(x, y)) {
					invokeAction(i);
				}
			}
		}

		abstract public void invokeAction(int n);
	};

	protected Widget currentWidget = null;
	protected int currentX = -1;
	protected int currentY = -1;
	protected List<Widget> widgets;

	public void onDraw(Canvas c) {
		if(widgets == null) setupWidgets();

		for(Widget w: widgets) {
			if(currentWidget == w) continue;

			w.onDraw(c);
		}

		if(currentWidget != null) {
			currentWidget.onActiveDraw(c, currentX, currentY);
		}
	}

	protected void setupWidgets() {
		widgets = new ArrayList<Widget>();
	}

	public boolean onTouchEvent(MotionEvent e) {
		if(widgets == null) return false; // wait for setup first, i.e. wait for first drawing

		int x = currentX = (int)e.getX(0);
		int y = currentY = (int)e.getY(0);

		switch(e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				//Log.i("Utilator", "MainSurface, DOWN: " + x + ", " + y);
				for(Widget w: widgets) {
					if(w.activateZone.contains(x, y)) {
						currentWidget = w;
						invalidate();
						return true;
					}
				}
				break;

			case MotionEvent.ACTION_UP:
				//Log.i("Utilator", "MainSurface, UP: " + x + ", " + y);
				if(currentWidget != null) {
					currentWidget.onLift(x, y);
				}

				currentWidget = null;
				invalidate();
				return true;
		}

		if(currentWidget != null) {
			//Log.i("Utilator", "MainSurface, MOVE: " + x + ", " + y);
			//Log.i("Utilator", "  history: " + e.getHistorySize());
			//Log.i("Utilator", "  size: " + e.getSize());
			//Log.i("Utilator", "  pressure: " + e.getPressure());
			currentWidget.onMove(x, y);
			invalidate();
			return true;
		}

		return false;
	}

	public WidgetView(Context ctx) {
		super(ctx);
	}
}

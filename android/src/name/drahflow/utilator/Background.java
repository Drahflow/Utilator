package name.drahflow.utilator;

import android.content.*;
import android.app.*;
import java.util.*;
import android.widget.*;
import android.util.*;

import static name.drahflow.utilator.Util.*;

public class Background extends BroadcastReceiver {
	public static void start(Context ctx) {
		AlarmManager alarmMgr = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(ctx, Background.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);
		alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 5 * 60 * 1000, pendingIntent);
		//alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 1000, 60 * 1000, pendingIntent);
		Log.i("Utilator", "Background, alarm set");
	}

	@Override public void onReceive(Context ctx, Intent intent) {
		Log.i("Utilator", "Background, received");

		Intent notificationIntent = new Intent(ctx, Utilator.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

		Database db = new Database(ctx);
		try {
			String o = db.getTaskSeenLast();
			String n = db.getBestTask(ctx, new Date());

			if(n == null || (o != null && n.equals(o))) {
				Log.i("Utilator", "Background, old == new task: " + n);
				return;
			}

			Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());
			notification.defaults = Notification.DEFAULT_SOUND;
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(ctx, "Task switch", db.loadTask(n).title, contentIntent);

			((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE))
				.notify(0, notification);
		} finally {
			db.close();
		}
	}
}

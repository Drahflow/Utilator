package name.drahflow.utilator;

import android.app.Activity;
import android.os.Bundle;
import android.util.*;
import android.view.*;
import java.util.*;

import static name.drahflow.utilator.Util.*;

public class Utilator extends Activity
{
	public Database db;
	private MainSurface main;

	/** Called when the activity is first created. */
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TrustManager.install();

		db = new Database(this);

		main = new MainSurface(this);
		switchToBestTask();
		setContentView(main);
	}

	private String getBestTask(Database db) {
		Collection<Map<String, Object>> tasks = db.loadOpenTasks();

		// Log.i("Utilator", "Tasks: " + tasks);

		Map<String, Object> bestTask = null;
		float bestImportance = 0;

		Date now = new Date();

		for(Map<String, Object> task: tasks) {
			float importance = Distribution.calculateImportance(this, now, task);
			// Log.i("Utilator", "Task: " + loadString(task, "title"));
			// Log.i("Utilator", "  importance: " + importance);

			if(importance > bestImportance) {
				bestTask = task;
				bestImportance = importance;
			}
		}

		return bestTask == null? null: loadString(bestTask, "gid");
	}

	public void switchToBestTask() {
		main.setTask(getBestTask(db));
	}

	@Override public void onBackPressed() {
		setContentView(main);
		main.reloadTask();
	}

	private Map<MenuItem, Runnable> optionAction = new HashMap<MenuItem, Runnable>();
	@Override public boolean onCreateOptionsMenu(Menu menu) {
		optionAction.put(menu.add("check"),
				new Runnable() {
					public void run() {
						switchToBestTask();
					}
				});
		optionAction.put(menu.add("sync"),
				new Runnable() {
					public void run() {
						//Log.i("Utilator", "Syncing");

						new Synchronization(Utilator.this).perform();

						switchToBestTask();
					}
				});

		return true;
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		optionAction.get(item).run();
		return true;
	}
}

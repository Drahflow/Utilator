package name.drahflow.utilator;

import android.app.Activity;
import android.os.Bundle;
import android.util.*;
import android.view.*;
import java.util.*;

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
		Collection<Map<String, Object>> tasks = db.loadTasks();

		Log.i("Utilator", "Tasks: " + tasks);

		if(tasks.isEmpty()) {
			Map<String, Object> task = new HashMap<String, Object>();
			task.put("title", "App zu Ende bauen");
			task.put("description", "Die app muss noch zuende gebaut werden, mindestens asoeucahsoeuctahoseuntahoseuntahoseunatoseuatohu au\n1\n2\n3");
			task.put("seconds_taken", 0);
			task.put("seconds_estimate", 120);
			task.put("status", 0);
			
			return db.createTask(task);
		}

		return tasks.iterator().next().get("gid").toString();
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
		optionAction.put(menu.add("sync"),
				new Runnable() {
					public void run() {
						Log.i("Utilator", "Syncing");

						new Synchronization(Utilator.this).perform();
					}
				});

		return true;
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		optionAction.get(item).run();
		return true;
	}
}

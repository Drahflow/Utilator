package name.drahflow.utilator;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
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
		optionAction.put(menu.add("search"),
				new Runnable() {
					public void run() {
						final EditText input = new EditText(Utilator.this);

						new AlertDialog.Builder(Utilator.this)
								.setTitle("Search")
								.setView(input)
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											String regex = input.getText().toString(); 
											Log.i("Utilator", "Utilator, searched for " + regex);

											List<Map<String, Object>> allTasks = db.loadAllTasks();
											Set<String> matching = new HashSet<String>();

											for(Map<String, Object> task: allTasks) {
												if(loadString(task, "title").matches(".*" + regex + ".*") ||
													loadString(task, "description").matches(".*" + regex + ".*")) {
													matching.add(loadString(task, "gid"));
												}
											}

											if(matching.size() > 1) {
												Toast toast = Toast.makeText(Utilator.this, "multiple tasks match", Toast.LENGTH_SHORT);
												toast.show();

												main.setTask(matching.iterator().next());
											} else if(matching.isEmpty()) {
												Toast toast = Toast.makeText(Utilator.this, "no tasks match", Toast.LENGTH_SHORT);
												toast.show();
											} else {
												main.setTask(matching.iterator().next());
											}
										}
								}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) { }
								}).show();

						InputMethodManager imm = (InputMethodManager) Utilator.this.getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
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

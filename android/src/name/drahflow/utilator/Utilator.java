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
		Background.start(this);

		db = new Database(this);

		main = new MainSurface(this);
		switchToBestTask();
		setContentView(main);
	}

	@Override public void onDestroy() {
		db.close();

		super.onDestroy();
	}

	private String getBestTask(Database db) {
		return db.getBestTask(this, new Date());
	}

	public void switchToBestTask() {
		String best = getBestTask(db);
		db.setTaskSeenLast(best);
		main.setTask(best);
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
											final List<String> matching = new ArrayList<String>();

											for(Map<String, Object> task: allTasks) {
												if(loadString(task, "title").matches(".*" + regex + ".*") ||
													loadString(task, "description").matches(".*" + regex + ".*")) {
													matching.add(loadString(task, "gid") + ":" + loadString(task, "title"));
												}
											}

											if(matching.size() > 1) {
												AlertDialog.Builder builder = new AlertDialog.Builder(Utilator.this);
												builder.setTitle("multiple matches");
												builder.setItems(matching.toArray(new String[0]), new DialogInterface.OnClickListener(){
													public void onClick(DialogInterface dialogInterface, int item) {
														main.setTask(matching.get(item).substring(0, matching.get(item).indexOf(':')));
														return;
													}
												});
												builder.create().show();
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

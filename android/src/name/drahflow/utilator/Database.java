package name.drahflow.utilator;

import android.app.*;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.util.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

import static name.drahflow.utilator.Util.*;

public class Database {
	private SQLiteDatabase db;

	public Database(Context ctx) {
		db = new DatabaseOpenHelper(ctx).getWritableDatabase();
	}

	public void close() {
		db.close();
	}

	public List<Task> loadAllTasks() {
		List<Task> r = new ArrayList<Task>();

		AbstractWindowedCursor res = (AbstractWindowedCursor)db.rawQuery(
				"SELECT gid, title, description, author, seconds_estimate, seconds_taken, status, closed_at, publication, last_edit FROM task",
				new String[] { });

		// this actually exists to initialize the underlying CursorWindow
		if(!res.moveToNext()) return r;

		final CursorWindow data = res.getWindow();
		final int rows = data.getNumRows();

		for(int row = 0; row < rows; ++row) {
			Task t = new Task();

			t.gid = data.getString(row, 0);
			t.title = data.getString(row, 1);
			t.description = data.getString(row, 2);
			t.author = data.getString(row, 3);
			t.seconds_estimate = data.getInt(row, 4);
			t.seconds_taken = data.getInt(row, 5);
			t.status = data.getInt(row, 6);
			t.closed_at = data.getString(row, 7);
			t.publication = data.getInt(row, 8);
			t.last_edit = data.getString(row, 9);
			t.updateCachedFields();

			r.add(t);
		}

		res.close();
		return r;
	}

	private List<Task> loadOpenTasks() {
		List<Task> r = new ArrayList<Task>();

		Cursor res = db.rawQuery(
				"SELECT gid, seconds_estimate, seconds_taken, status FROM task WHERE status < 100",
				new String[] { });
		String[] cols = lowerCaseArray(res.getColumnNames());

		while(res.moveToNext()) {
			Task t = new Task();

			t.gid = res.getString(0);
			t.seconds_estimate = res.getInt(1);
			t.seconds_taken = res.getInt(2);
			t.status = res.getInt(3);
			t.updateCachedFields();

			r.add(t);
		}

		res.close();
		return r;
	}

	public Map<String, List<String>> loadManyTaskUtilities(String where) {
		Map<String, List<String>> r = new HashMap<String, List<String>>();

		Cursor res = db.rawQuery(
				"SELECT t.gid, u.distribution FROM task t JOIN task_utility u ON t.id = u.task " + where,
				new String[] { });

		while(res.moveToNext()) {
			final String gid = res.getString(0);
			List<String> distribution = r.get(gid);
			if(distribution == null) {
				r.put(gid, distribution = new ArrayList<String>());
			}

			distribution.add(res.getString(1));
		}

		res.close();
		return r;
	}

	public Map<String, List<String>> loadManyTaskLikelyhoodTime(String where) {
		Map<String, List<String>> r = new HashMap<String, List<String>>();

		Cursor res = db.rawQuery(
				"SELECT t.gid, l.distribution FROM task t JOIN task_likelyhood_time l ON t.id = l.task " + where,
				new String[] { });

		while(res.moveToNext()) {
			final String gid = res.getString(0);
			List<String> distribution = r.get(gid);
			if(distribution == null) {
				r.put(gid, distribution = new ArrayList<String>());
			}

			distribution.add(res.getString(1));
		}

		res.close();
		return r;
	}

	public Map<String, List<String>> loadManyTaskExternal(String where) {
		Map<String, List<String>> r = new HashMap<String, List<String>>();

		Cursor res = db.rawQuery(
				"SELECT t.gid, e.external FROM task t JOIN task_external e ON t.id = e.task " + where,
				new String[] { });

		while(res.moveToNext()) {
			final String gid = res.getString(0);
			List<String> external = r.get(gid);
			if(external == null) {
				r.put(gid, external = new ArrayList<String>());
			}

			external.add(res.getString(1));
		}

		res.close();
		return r;
	}

	private String[] lowerCaseArray(String[] a) {
		for(int i = 0; i < a.length; ++i) {
			a[i] = a[i].toLowerCase();
		}

		return a;
	}

	private Map<String, Object> loadCursorRow(String[] cols, Cursor res) {
			Map<String, Object> row = new HashMap<String, Object>();

			for(int i = 0; i < cols.length; ++i) {
				row.put(cols[i], res.getString(i));
			}

			return row;
	}

	public Map<String, Object> loadTask(String gid) {
		Map<String, Object> r = new HashMap<String, Object>();

		Cursor res = db.rawQuery(
				"SELECT gid, title, description, author, seconds_estimate, seconds_taken, status, closed_at, publication, last_edit FROM task WHERE gid = ?",
				new String[] { gid });
		String[] cols = lowerCaseArray(res.getColumnNames());

		while(res.moveToNext()) {
			Map<String, Object> row = loadCursorRow(cols, res);
			if(res.moveToNext()) throw new Error("duplicate GID: " + gid);

			res.close();
			return row;
		}

		res.close();
		return null;
	}

	public List<Map<String, Object>> loadTaskUtilities(String gid) {
		List<Map<String, Object>> r = new ArrayList<Map<String, Object>>();

		Cursor res = db.rawQuery(
				"SELECT distribution FROM task_utility WHERE task = (SELECT id FROM task WHERE gid = ?)",
				new String[] { gid });
		String[] cols = lowerCaseArray(res.getColumnNames());

		while(res.moveToNext()) {
			r.add(loadCursorRow(cols, res));
		}

		res.close();
		return r;
	}

	public List<Map<String, Object>> loadTaskLikelyhoodTime(String gid) {
		List<Map<String, Object>> r = new ArrayList<Map<String, Object>>();

		Cursor res = db.rawQuery(
				"SELECT id, distribution FROM task_likelyhood_time WHERE task = (SELECT id FROM task WHERE gid = ?)",
				new String[] { gid });
		String[] cols = lowerCaseArray(res.getColumnNames());

		while(res.moveToNext()) {
			r.add(loadCursorRow(cols, res));
		}

		res.close();
		return r;
	}

	public String createTask(Map<String, Object> task) {
		if(task.get("gid") == null) task.put("gid", createGid());
		if(task.get("status") == null) task.put("status", 0);
		if(task.get("publication") == null) task.put("publication", 1);

		db.beginTransaction();
		try {
			db.execSQL(
					"INSERT INTO task (gid, title, description, author, seconds_estimate, seconds_taken, status, closed_at, publication, last_edit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					mapArrayDeref(task, "gid", "title", "description", "author", "seconds_estimate", "seconds_taken", "status", "closed_at", "publication", "last_edit"));
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		Log.i("Utilator", "Task newly created: " + task.get("gid"));

		return task.get("gid").toString();
	}

	public void createEmptyTask(String gid) {
		db.beginTransaction();
		try {
			db.execSQL("INSERT INTO task (gid) VALUES (?)", new String[] { gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void touchTask(String gid) {
		db.beginTransaction();
		try {
			db.execSQL("UPDATE task SET last_edit = ? WHERE gid = ?", new String[] { isoFullDate(new Date()), gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void updateTask(Map<String, Object> task) {
		db.beginTransaction();
		try {
			db.execSQL(
					"UPDATE task SET title = ?, description = ?, author = ?, seconds_estimate = ?, seconds_taken = ?, status = ?, closed_at = ?, publication = ?, last_edit = ? WHERE gid = ?",
					mapArrayDeref(task, "title", "description", "author", "seconds_estimate", "seconds_taken", "status", "closed_at", "publication", "last_edit", "gid"));
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void addTimeTaken(String gid, int time) {
		db.beginTransaction();
		try {
			db.execSQL("UPDATE task SET seconds_taken = seconds_taken + ? WHERE gid = ?", new Object[] { time, gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void setTitle(String gid, String title) {
		db.beginTransaction();
		try {
			db.execSQL("UPDATE task SET title = ? WHERE gid = ?", new Object[] { title, gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void setDescription(String gid, String description) {
		db.beginTransaction();
		try {
			db.execSQL("UPDATE task SET description = ? WHERE gid = ?", new Object[] { description, gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void setSecondsEstimate(String gid, long secondsEstimate) {
		db.beginTransaction();
		try {
			db.execSQL("UPDATE task SET seconds_estimate = ? WHERE gid = ?", new Object[] { secondsEstimate, gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void setStatus(String gid, int status) {
		db.beginTransaction();
		try {
			if(status == 100) {
				db.execSQL("UPDATE task SET status = 100, closed_at = ? WHERE gid = ?", new Object[] { new Date().getTime() / 1000, gid });
			} else {
				db.execSQL("UPDATE task SET status = ? WHERE gid = ?", new Object[] { status, gid });
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void setUtility(String gid, int utility) {
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM task_utility WHERE task = (SELECT id FROM task WHERE gid = ?)", new Object[] { gid });
			db.execSQL("INSERT INTO task_utility (task, distribution) VALUES ((SELECT id FROM task WHERE gid = ?), ?)", 
					new Object[] { gid, "0constant:" + utility });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void addUtility(String gid, String entry) {
		db.beginTransaction();
		try {
			db.execSQL("INSERT INTO task_utility (task, distribution) VALUES ((SELECT id FROM task WHERE gid = ?), ?)", 
					new Object[] { gid, entry });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void deleteUtilities(String gid) {
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM task_utility WHERE task = (SELECT id FROM task WHERE gid = ?)", 
					new Object[] { gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void addLikelyhoodTime(String gid, String distribution) {
		db.beginTransaction();
		try {
			db.execSQL("INSERT INTO task_likelyhood_time (task, distribution) VALUES ((SELECT id FROM task WHERE gid = ?), ?)",
					new Object[] { gid, distribution });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void deleteLikelyhoodsTime(String gid) {
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM task_likelyhood_time WHERE task = (SELECT id FROM task WHERE gid = ?)",
					new Object[] { gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void addExternal(String gid, String external) {
		db.beginTransaction();
		try {
			db.execSQL("INSERT INTO task_external (task, external) VALUES ((SELECT id FROM task WHERE gid = ?), ?)",
					new Object[] { gid, external });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void deleteExternal(String gid) {
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM task_external WHERE task = (SELECT id FROM task WHERE gid = ?)", 
					new Object[] { gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void updateLikelyhoodTime(String id, String distribution) {
		db.beginTransaction();
		try {
			db.execSQL("UPDATE task_likelyhood_time SET distribution = ? WHERE id = ?", new Object[] { distribution, id });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public String[] mapArrayDeref(Map<String, Object> map, String... keys) {
		String[] res = new String[keys.length];

		for(int i = 0; i < keys.length; ++i) {
			Object o = map.get(keys[i]);
			res[i] = o != null? o.toString(): null;
		}

		return res;
	}

	static private final String GID_TEMPLATE = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx";

	public String createGid() {
		StringBuilder res = new StringBuilder();

		for(int i = 0; i < GID_TEMPLATE.length(); ++i) {
			switch(GID_TEMPLATE.charAt(i)) {
				case 'x': res.append("0123456789ABCDEF".charAt((int)(Math.random() * 16))); break;
				case 'y': res.append("89AB".charAt((int)(Math.random() * 4))); break;
				default: res.append(GID_TEMPLATE.charAt(i)); break;
			}
		}

		return res.toString();
	}

	public String getBestTask(Context ctx, Date when) {
		List<Task> tasks = loadOpenTasks();
		Map<String, List<String>> taskUtilities = loadManyTaskUtilities("WHERE t.status < 100");
		Map<String, List<String>> taskLikelyhoodTime = loadManyTaskLikelyhoodTime("WHERE t.status < 100");

		// Log.i("Utilator", "Tasks: " + tasks);

		Task bestTask = null;
		float bestImportance = 0;

		for(Task task: tasks) {
			task.task_utility = TimeDistribution.compile(0, taskUtilities.get(task.gid));
			task.task_likelyhood_time = TimeDistribution.compile(990, taskLikelyhoodTime.get(task.gid));

			float importance = DistributionUtil.calculateImportance(ctx, this, when, task);
			// Log.i("Utilator", "Task: " + loadString(task, "title"));
			// Log.i("Utilator", "  importance: " + importance);

			if(importance > bestImportance) {
				bestTask = task;
				bestImportance = importance;
			}
		}

		return bestTask == null? null: bestTask.gid;
	}

	public String getTaskSeenLast() {
		Cursor res = db.rawQuery("SELECT gid FROM task_seen_last", new String[0]);
		String[] cols = lowerCaseArray(res.getColumnNames());

		while(res.moveToNext()) {
			Map<String, Object> row = loadCursorRow(cols, res);
			if(res.moveToNext()) throw new Error("multiple rows in task_seen_last");

			res.close();
			return (String)row.get("gid");
		}

		res.close();
		return null;
	}

	public void setTaskSeenLast(String gid) {
		db.beginTransaction();
		try {
			db.execSQL("UPDATE task_seen_last SET gid = ?", new String[] { gid });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
}

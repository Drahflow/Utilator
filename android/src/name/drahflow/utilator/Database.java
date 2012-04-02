package name.drahflow.utilator;

import android.app.*;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.util.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

public class Database {
	private SQLiteDatabase db;

	public Database(Context ctx) {
		db = new DatabaseOpenHelper(ctx).getWritableDatabase();
	}

	public Collection<Map<String, Object>> loadTasks() {
		List<Map<String, Object>> r = new ArrayList<Map<String, Object>>();

		Cursor res = db.rawQuery(
				"SELECT gid, title, description, seconds_estimate, seconds_taken, status FROM task WHERE status < 100",
				new String[] { });
		String[] cols = res.getColumnNames();

		// optimize to only store strings once
		while(res.moveToNext()) {
			r.add(loadCursorRow(cols, res));
		}

		return r;
	}

	private Map<String, Object> loadCursorRow(String[] cols, Cursor res) {
			Map<String, Object> row = new HashMap<String, Object>();

			for(int i = 0; i < cols.length; ++i) {
				row.put(cols[i].toLowerCase(), res.getString(i));
			}

			return row;
	}

	public Map<String, Object> loadTask(String gid) {
		Map<String, Object> r = new HashMap<String, Object>();

		Cursor res = db.rawQuery(
				"SELECT gid, title, description, seconds_estimate, seconds_taken, status FROM task WHERE gid = ?",
				new String[] { gid });
		String[] cols = res.getColumnNames();

		while(res.moveToNext()) {
			Map<String, Object> row = loadCursorRow(cols, res);
			if(res.moveToNext()) throw new Error("duplicate GID: " + gid);

			return row;
		}

		return null;
	}

	public List<Map<String, Object>> loadTaskUtilities(String gid) {
		List<Map<String, Object>> r = new ArrayList<Map<String, Object>>();

		Cursor res = db.rawQuery(
				"SELECT distribution FROM task_utility WHERE task = (SELECT id FROM task WHERE gid = ?)",
				new String[] { gid });
		String[] cols = res.getColumnNames();

		while(res.moveToNext()) {
			r.add(loadCursorRow(cols, res));
		}

		return r;
	}

	public List<Map<String, Object>> loadTaskLikelyhoodTime(String gid) {
		List<Map<String, Object>> r = new ArrayList<Map<String, Object>>();

		Cursor res = db.rawQuery(
				"SELECT id, distribution FROM task_likelyhood_time WHERE task = (SELECT id FROM task WHERE gid = ?)",
				new String[] { gid });
		String[] cols = res.getColumnNames();

		while(res.moveToNext()) {
			r.add(loadCursorRow(cols, res));
		}

		return r;
	}

	public String createTask(Map<String, Object> task) {
		if(task.get("gid") == null) task.put("gid", createGid());
		if(task.get("status") == null) task.put("status", 0);

		db.beginTransaction();
		try {
			db.execSQL(
					"INSERT INTO task (gid, title, description, seconds_estimate, seconds_taken, status) VALUES (?, ?, ?, ?, ?, ?)",
					mapArrayDeref(task, "gid", "title", "description", "seconds_estimate", "seconds_taken", "status"));
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		Log.i("Utilator", "Task newly created: " + task.get("gid"));

		return task.get("gid").toString();
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
			res[i] = map.get(keys[i]).toString();
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
}

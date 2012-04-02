package name.drahflow.utilator;

import android.app.*;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.util.*;
import android.database.sqlite.*;
import java.util.*;

class DatabaseOpenHelper extends SQLiteOpenHelper {
	public DatabaseOpenHelper(Context context) {
		super(context, "Utilator", null, 1);
	}

	@Override public void onCreate(SQLiteDatabase db) {
		db.execSQL("" +
				"CREATE TABLE task (" +
				"  id INTEGER PRIMARY KEY," +
				"  gid BLOB NOT NULL," +
				"  title TEXT," +
				"  description TEXT," +
				"  author BLOB," +
				"  seconds_estimate INTEGER," +
				"  seconds_taken INTEGER," +
				"  status INTEGER," +
				"  closed_at TEXT," +
				"  publication INTEGER" +
				");" +
				"");

		db.execSQL("" +
				"CREATE TABLE expectation (" +
				"  id INTEGER PRIMARY KEY," +
				"  gid BLOB NOT NULL," +
				"  title TEXT," +
				"  author BLOB," +
				"  publication INTEGER," +
				"  value INTEGER," +
				"  last_calculated TEXT" +
				");" +
				"");

		db.execSQL("" +
				"CREATE TABLE task_utility (" +
				"  id INTEGER PRIMARY KEY," +
				"  task INTEGER REFERENCES task (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  distribution TEXT NOT NULL" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX task_utility_idx ON task_utility (task);" +
				"");

		db.execSQL("" +
				"CREATE TABLE task_likelyhood_time (" +
				"  id INTEGER PRIMARY KEY," +
				"  task INTEGER REFERENCES task (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  distribution TEXT NOT NULL" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX task_likelyhood_time_idx ON task_likelyhood_time (task);" +
				"");

		db.execSQL("" +
				"CREATE TABLE task_likelyhood_space (" +
				"  id INTEGER PRIMARY KEY," +
				"  task INTEGER REFERENCES task (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  distribution TEXT NOT NULL" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX task_likelyhood_space_idx ON task_likelyhood_space (task);" +
				"");

		db.execSQL("" +
				"CREATE TABLE task_precondition (" +
				"  id INTEGER PRIMARY KEY," +
				"  task INTEGER REFERENCES task (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  precondition TEXT NOT NULL" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX task_precondition_idx ON task_precondition (task);" +
				"");

		db.execSQL("" +
				"CREATE TABLE task_effect (" +
				"  id INTEGER PRIMARY KEY," +
				"  task INTEGER REFERENCES task (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  expectation INTEGER REFERENCES expectation (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  effect TEXT NOT NULL" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX task_effect_idx1 ON task_effect (task);" +
				"");

		db.execSQL("" +
				"CREATE INDEX task_effect_idx2 ON task_effect (expectation);" +
				"");

		db.execSQL("" +
				"CREATE TABLE task_external (" +
				"  id INTEGER PRIMARY KEY," +
				"  task INTEGER REFERENCES task (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  external TEXT" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX task_external_idx ON task_external (task);" +
				"");

		db.execSQL("" +
				"CREATE TABLE expectation_utilities (" +
				"  id INTEGER PRIMARY KEY," +
				"  expectation INTEGER REFERENCES expectation (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  distribution TEXT NOT NULL" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX expectation_utilities_idx ON expectation_utilities (expectation);" +
				"");

		db.execSQL("" +
				"CREATE TABLE expectation_derivative (" +
				"  id INTEGER PRIMARY KEY," +
				"  expectation INTEGER REFERENCES expectation (id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL," +
				"  distribution TEXT NOT NULL" +
				");" +
				"");

		db.execSQL("" +
				"CREATE INDEX expectation_derivative_idx ON expectation_derivative (expectation);" +
				"");
	}

	@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		throw new Error("no database upgrading");
	}
}

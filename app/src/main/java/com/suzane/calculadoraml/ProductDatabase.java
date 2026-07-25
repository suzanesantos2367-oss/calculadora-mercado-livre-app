package com.suzane.calculadoraml;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class ProductDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "produtos.db";
    private static final int DB_VERSION = 1;

    public ProductDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "cost REAL NOT NULL," +
                "extra REAL NOT NULL," +
                "tax REAL NOT NULL," +
                "favorite INTEGER NOT NULL DEFAULT 0," +
                "updated_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS products");
        onCreate(db);
    }

    public long insert(Product p) {
        ContentValues v = values(p);
        return getWritableDatabase().insert("products", null, v);
    }

    public void update(Product p) {
        getWritableDatabase().update("products", values(p), "id=?", new String[]{String.valueOf(p.id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("products", "id=?", new String[]{String.valueOf(id)});
    }

    public Product get(long id) {
        Cursor c = getReadableDatabase().query("products", null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        try {
            return c.moveToFirst() ? fromCursor(c) : null;
        } finally {
            c.close();
        }
    }

    public List<Product> search(String query, boolean favoritesOnly) {
        List<Product> list = new ArrayList<>();
        String where = "name LIKE ?";
        List<String> args = new ArrayList<>();
        args.add("%" + query + "%");
        if (favoritesOnly) where += " AND favorite=1";
        Cursor c = getReadableDatabase().query("products", null, where,
                args.toArray(new String[0]), null, null, "favorite DESC, updated_at DESC");
        try {
            while (c.moveToNext()) list.add(fromCursor(c));
        } finally {
            c.close();
        }
        return list;
    }

    public int countAll() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM products", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; }
        finally { c.close(); }
    }

    public int countFavorites() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM products WHERE favorite=1", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; }
        finally { c.close(); }
    }

    private ContentValues values(Product p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name);
        v.put("cost", p.cost);
        v.put("extra", p.extra);
        v.put("tax", p.tax);
        v.put("favorite", p.favorite ? 1 : 0);
        v.put("updated_at", p.updatedAt);
        return v;
    }

    private Product fromCursor(Cursor c) {
        Product p = new Product();
        p.id = c.getLong(c.getColumnIndexOrThrow("id"));
        p.name = c.getString(c.getColumnIndexOrThrow("name"));
        p.cost = c.getDouble(c.getColumnIndexOrThrow("cost"));
        p.extra = c.getDouble(c.getColumnIndexOrThrow("extra"));
        p.tax = c.getDouble(c.getColumnIndexOrThrow("tax"));
        p.favorite = c.getInt(c.getColumnIndexOrThrow("favorite")) == 1;
        p.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return p;
    }
}

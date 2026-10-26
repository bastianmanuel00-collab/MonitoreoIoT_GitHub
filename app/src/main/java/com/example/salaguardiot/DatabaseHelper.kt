package com.example.salaguardiot

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "salaguard.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE usuarios(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL
            )
            """.trimIndent()
        )

        val passwordHash = hashPassword("SalaGuard2026")

        db.execSQL(
            "INSERT INTO usuarios(usuario, password_hash) VALUES(?, ?)",
            arrayOf("admin", passwordHash)
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    fun validarUsuario(usuario: String, password: String): Boolean {

        val db = readableDatabase
        val passwordHash = hashPassword(password)

        val cursor = db.rawQuery(
            """
            SELECT id FROM usuarios
            WHERE usuario = ? AND password_hash = ?
            """.trimIndent(),
            arrayOf(usuario, passwordHash)
        )

        val existe = cursor.moveToFirst()

        cursor.close()

        return existe
    }

    private fun hashPassword(password: String): String {

        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}
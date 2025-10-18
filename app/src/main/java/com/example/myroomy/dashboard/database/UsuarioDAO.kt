package com.example.myroomy.dashboard.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.example.myroomy.dashboard.models.TipoUsuario
import com.example.myroomy.dashboard.models.Usuario

class UsuarioDAO(context: Context) {
    private val dbHelper = AdminDatabaseHelper(context)

    fun insertar(usuario: Usuario): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("nombre", usuario.nombre)
            put("correo", usuario.correo)
            put("tipo", usuario.tipo.valor)
            put("clave", usuario.clave)
            put("estado", usuario.estado)
            put("urlFoto", usuario.urlFoto) // NUEVO: guardar la url
        }

        return try {
            db.insertOrThrow("usuarios", null, values)
        } catch (e: SQLiteConstraintException) {
            Log.e("UsuarioDAO", "Error al insertar usuario: correo duplicado", e)
            -1
        } finally {
            db.close()
        }
    }

    fun obtenerTodos(): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM usuarios", null)

        if (cursor.moveToFirst()) {
            do {
                val usuario = Usuario(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                    tipo = TipoUsuario.from(cursor.getString(cursor.getColumnIndexOrThrow("tipo"))),
                    clave = cursor.getString(cursor.getColumnIndexOrThrow("clave")),
                    estado = cursor.getInt(cursor.getColumnIndexOrThrow("estado")),
                    urlFoto = cursor.getString(cursor.getColumnIndexOrThrow("urlFoto")) // NUEVO: leer la url
                )
                lista.add(usuario)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return lista
    }

    fun obtenerUsuarioPorCredenciales(correo: String, clave: String): Usuario? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM usuarios WHERE correo = ? AND clave = ? AND estado = 1",
            arrayOf(correo, clave)
        )

        var usuario: Usuario? = null
        if (cursor.moveToFirst()) {
            usuario = Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                tipo = TipoUsuario.from(cursor.getString(cursor.getColumnIndexOrThrow("tipo"))),
                clave = cursor.getString(cursor.getColumnIndexOrThrow("clave")),
                estado = cursor.getInt(cursor.getColumnIndexOrThrow("estado")),
                urlFoto = cursor.getString(cursor.getColumnIndexOrThrow("urlFoto")) // NUEVO: leer la url
            )
        }

        cursor.close()
        db.close()
        return usuario
    }

    fun actualizar(usuario: Usuario): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("nombre", usuario.nombre)
            put("correo", usuario.correo)
            put("tipo", usuario.tipo.valor)
            put("clave", usuario.clave)
            put("estado", usuario.estado)
            put("urlFoto", usuario.urlFoto) // NUEVO: actualizar la url
        }

        val rows = db.update("usuarios", values, "id = ?", arrayOf(usuario.id.toString()))
        db.close()
        return rows
    }

    fun eliminar(id: Int): Int {
        val db = dbHelper.writableDatabase
        val rows = db.delete("usuarios", "id = ?", arrayOf(id.toString()))
        db.close()
        return rows
    }

    fun existeCorreo(correo: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id FROM usuarios WHERE correo = ?", arrayOf(correo))
        val existe = cursor.moveToFirst()
        cursor.close()
        db.close()
        return existe
    }

    fun obtenerPorCorreo(correo: String): Usuario? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM usuarios WHERE correo = ?", arrayOf(correo))
        var usuario: Usuario? = null
        if (cursor.moveToFirst()) {
            usuario = Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                tipo = TipoUsuario.from(cursor.getString(cursor.getColumnIndexOrThrow("tipo"))),
                clave = cursor.getString(cursor.getColumnIndexOrThrow("clave")),
                estado = cursor.getInt(cursor.getColumnIndexOrThrow("estado")),
                urlFoto = cursor.getString(cursor.getColumnIndexOrThrow("urlFoto"))
            )
        }
        cursor.close()
        db.close()
        return usuario
    }

}

package com.example.myroomy.dashboard.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.example.myroomy.dashboard.models.Reserva

class ReservaDAO(context: Context) {
    private val dbHelper = AdminDatabaseHelper(context)

    fun insertar(reserva: Reserva): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id_usuario", reserva.idUsuario)
            put("id_habitacion", reserva.idHabitacion)
            put("fecha_solicitud", reserva.fechaSolicitud)
            put("estado", reserva.estado)
            put("comentario", reserva.comentario)
            put("fecha_ingreso", reserva.fechaIngreso)
            put("fecha_salida", reserva.fechaSalida)
            put("total", reserva.total)
            put("metodo_pago", reserva.metodoPago)
            put("cantidad_personas", reserva.cantidadPersonas)
        }
        return db.insert("reservas", null, values)
    }
    fun obtenerPorUsuario(idUsuario: Int): List<Reserva> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Reserva>()

        val query = """
        SELECT r.*, 
               h.nombre AS nombre_habitacion, 
               h.precio AS precio_habitacion
        FROM reservas r
        INNER JOIN habitaciones h ON r.id_habitacion = h.id
        WHERE r.id_usuario = ?
        ORDER BY r.fecha_solicitud DESC
    """

        val cursor = db.rawQuery(query, arrayOf(idUsuario.toString()))

        while (cursor.moveToNext()) {
            lista.add(
                Reserva(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")),
                    idHabitacion = cursor.getInt(cursor.getColumnIndexOrThrow("id_habitacion")),
                    fechaSolicitud = cursor.getString(cursor.getColumnIndexOrThrow("fecha_solicitud")),
                    estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                    comentario = cursor.getString(cursor.getColumnIndexOrThrow("comentario")),
                    fechaIngreso = cursor.getString(cursor.getColumnIndexOrThrow("fecha_ingreso")),
                    fechaSalida = cursor.getString(cursor.getColumnIndexOrThrow("fecha_salida")),
                    total = cursor.getDouble(cursor.getColumnIndexOrThrow("total")),
                    metodoPago = cursor.getString(cursor.getColumnIndexOrThrow("metodo_pago")),
                    cantidadPersonas = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad_personas")),
                    nombreUsuario = "", // no lo necesitas aquí
                    urlFotoUsuario = "", // no lo necesitas aquí
                    nombreHabitacion = cursor.getString(cursor.getColumnIndexOrThrow("nombre_habitacion")),
                    precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_habitacion"))
                )
            )
        }

        cursor.close()
        return lista
    }
    fun listarTodas(): List<Reserva> {
        val query = """
            SELECT r.*, 
                   u.nombre AS nombre_usuario, 
                   u.urlFoto AS url_foto_usuario, 
                   h.nombre AS nombre_habitacion, 
                   h.precio AS precio_habitacion
            FROM reservas r
            INNER JOIN usuarios u ON r.id_usuario = u.id
            INNER JOIN habitaciones h ON r.id_habitacion = h.id
            ORDER BY r.fecha_solicitud DESC
        """
        return ejecutarConsulta(query, emptyArray())
    }
    fun listarPorEstado(estado: String): List<Reserva> {
        val query = """
        SELECT r.*, 
               u.nombre AS nombre_usuario, 
               u.urlFoto AS url_foto_usuario, 
               h.nombre AS nombre_habitacion, 
               h.precio AS precio_habitacion
        FROM reservas r
        INNER JOIN usuarios u ON r.id_usuario = u.id
        INNER JOIN habitaciones h ON r.id_habitacion = h.id
        WHERE r.estado = ?
        ORDER BY r.fecha_solicitud DESC
    """
        val reservas = ejecutarConsulta(query, arrayOf(estado))
        Log.d("Reservas", "Reservas de estado $estado: ${reservas.size}")
        return reservas
    }

    private fun ejecutarConsulta(query: String, args: Array<String>): List<Reserva> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Reserva>()
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(query, args)
            while (cursor.moveToNext()) {
                lista.add(
                    Reserva(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")),
                        idHabitacion = cursor.getInt(cursor.getColumnIndexOrThrow("id_habitacion")),
                        fechaSolicitud = cursor.getString(cursor.getColumnIndexOrThrow("fecha_solicitud")),
                        estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                        comentario = cursor.getString(cursor.getColumnIndexOrThrow("comentario")),
                        fechaIngreso = cursor.getString(cursor.getColumnIndexOrThrow("fecha_ingreso")),
                        fechaSalida = cursor.getString(cursor.getColumnIndexOrThrow("fecha_salida")),
                        total = cursor.getDouble(cursor.getColumnIndexOrThrow("total")),
                        metodoPago = cursor.getString(cursor.getColumnIndexOrThrow("metodo_pago")),
                        cantidadPersonas = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad_personas")),
                        nombreUsuario = cursor.getString(cursor.getColumnIndexOrThrow("nombre_usuario")),
                        urlFotoUsuario = cursor.getString(cursor.getColumnIndexOrThrow("url_foto_usuario")),
                        nombreHabitacion = cursor.getString(cursor.getColumnIndexOrThrow("nombre_habitacion")),
                        precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_habitacion"))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ReservaDAO", "Error al ejecutar consulta: ${e.message}")
        } finally {
            cursor?.let {
                if (!it.isClosed) it.close()
            }
        }

        return lista
    }


    fun actualizarEstado(idReserva: Int, nuevoEstado: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("estado", nuevoEstado)
        }
        return db.update("reservas", values, "id = ?", arrayOf(idReserva.toString()))
    }
}

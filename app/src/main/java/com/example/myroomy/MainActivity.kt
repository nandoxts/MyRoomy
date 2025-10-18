package com.example.myroomy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.myroomy.dashboard.activities.DashboardActivity
import com.example.myroomy.dashboard.database.UsuarioDAO
import com.example.myroomy.dashboard.models.TipoUsuario
import com.example.myroomy.dashboard.models.Usuario
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var googleLoginButton: LinearLayout

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("MyRoomyPrefs", MODE_PRIVATE)
        val tipoUsuario = prefs.getString("usuario_tipo", null)

        if (tipoUsuario != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        googleLoginButton = findViewById(R.id.googleLoginButton)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                validarUsuario(username, password)
            } else {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("838633769248-jrks5ijsbmkg1k7siil9gc3aaeq7u3vm.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleLoginButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }

    private fun validarUsuario(correo: String, clave: String) {
        val dao = UsuarioDAO(this)
        val usuario = dao.obtenerUsuarioPorCredenciales(correo, clave)

        if (usuario != null) {
            guardarEnPrefs(usuario)
            Toast.makeText(this, "Bienvenido, ${usuario.nombre}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Credenciales incorrectas o usuario inactivo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: ""
                val nombre = account?.displayName ?: ""
                val fotoUrlRemota = account?.photoUrl?.toString() ?: ""

                // Aquí iniciamos el proceso asíncrono completo
                GlobalScope.launch(Dispatchers.Main) {
                    val fotoPathLocal = if (fotoUrlRemota.isNotEmpty()) {
                        descargarYGuardarImagenGoogle(fotoUrlRemota)
                    } else {
                        ""
                    }

                    procesarUsuarioGoogle(email, nombre, fotoPathLocal)
                }

            } catch (e: ApiException) {
                Toast.makeText(this, "Error en login con Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                Log.e("GOOGLE_LOGIN", "Error code: ${e.statusCode}", e)
            }
        }
    }

    private suspend fun descargarYGuardarImagenGoogle(urlRemota: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val input = URL(urlRemota).openStream()
                val nombreArchivo = "user_google_${System.currentTimeMillis()}.jpg"
                val carpetaDestino = File(filesDir, "usuarios")
                if (!carpetaDestino.exists()) carpetaDestino.mkdirs()

                val archivoDestino = File(carpetaDestino, nombreArchivo)
                archivoDestino.outputStream().use { output ->
                    input.copyTo(output)
                }

                Log.i("GOOGLE_LOGIN", "Imagen descargada y guardada en: ${archivoDestino.absolutePath}")
                archivoDestino.absolutePath
            } catch (e: Exception) {
                Log.e("GOOGLE_LOGIN", "Error al descargar imagen de Google", e)
                ""
            }
        }
    }

    private fun procesarUsuarioGoogle(email: String, nombre: String, fotoPathLocal: String) {
        val dao = UsuarioDAO(this)

        if (!dao.existeCorreo(email)) {
            val nuevoUsuario = Usuario(
                id = 0,
                nombre = nombre,
                correo = email,
                tipo = TipoUsuario.CLIENTE,
                clave = "",
                estado = 1,
                urlFoto = fotoPathLocal
            )
            val idGenerado = dao.insertar(nuevoUsuario)

            if (idGenerado > 0) {
                Log.i("GOOGLE_LOGIN", "Usuario Google insertado en BD con id: $idGenerado")

                // Creamos un usuario con el ID real
                val usuarioConId = nuevoUsuario.copy(id = idGenerado.toInt())
                guardarEnPrefs(usuarioConId)

                Toast.makeText(this, "Bienvenido, $nombre", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Log.e("GOOGLE_LOGIN", "Error al insertar usuario Google en BD")
                Toast.makeText(this, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
            }

        } else {
            val usuarioExistente = dao.obtenerPorCorreo(email)

            if (usuarioExistente != null) {
                val usuarioActualizado = usuarioExistente.copy(
                    nombre = nombre,
                    urlFoto = fotoPathLocal
                )
                dao.actualizar(usuarioActualizado)

                Log.i("GOOGLE_LOGIN", "Usuario Google actualizado en BD")
                guardarEnPrefs(usuarioActualizado)

                Toast.makeText(this, "Bienvenido, $nombre", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Log.e("GOOGLE_LOGIN", "No se pudo obtener usuario existente tras comprobar existencia")
                Toast.makeText(this, "Error al acceder al usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun guardarEnPrefs(usuario: Usuario) {
        val prefs = getSharedPreferences("MyRoomyPrefs", MODE_PRIVATE)
        prefs.edit()
            .putInt("usuario_id", usuario.id)
            .putString("usuario_correo", usuario.correo)
            .putString("usuario_nombre", usuario.nombre)
            .putString("usuario_foto", usuario.urlFoto)
            .putString("usuario_tipo", usuario.tipo.name)
            .putString("usuario_estado", if (usuario.estado == 1) "Activo" else "Inactivo")
            .apply()
    }

}

package com.example.myroomy.dashboard.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.myroomy.MainActivity
import com.example.myroomy.R
import com.example.myroomy.dashboard.fragments.*
import com.example.myroomy.dashboard.models.TipoUsuario
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import java.io.File
import androidx.core.content.edit

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private var tipoUsuario: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val prefs = getSharedPreferences("MyRoomyPrefs", MODE_PRIVATE)
        tipoUsuario = prefs.getString("usuario_tipo", TipoUsuario.CLIENTE.name)

        navigationView.menu.clear()
        if (tipoUsuario == TipoUsuario.ADMIN.name) {
            navigationView.inflateMenu(R.menu.menu_drawer_admin)
        } else {
            navigationView.inflateMenu(R.menu.menu_drawer_cliente)
        }

        if (savedInstanceState == null) {
            val initialFragment = if (tipoUsuario == TipoUsuario.ADMIN.name) {
                AdminDashboardFragment()
            } else {
                CatalogoFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor_main, initialFragment)
                .commitNow()
        }

        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerClosed(drawerView: View) {
                    when (item.itemId) {
                        R.id.nav_dashboard -> replaceFragment(AdminDashboardFragment())
                        R.id.nav_gestion_habitaciones -> replaceFragment(HabitacionListFragment())
                        R.id.nav_gestion_reservas -> replaceFragment(ReservaListFragment())
                        R.id.nav_catalogo -> replaceFragment(CatalogoFragment())
                        R.id.nav_gestion_usuarios -> replaceFragment(UsuarioListFragment())
                        R.id.nav_reservas -> replaceFragment(MisReservasFragment())
                        R.id.nav_perfil -> replaceFragment(PerfilFragment())
                        R.id.nav_info_hotel -> replaceFragment(UbicacionFragment())
                        R.id.nav_cerrar_sesion -> cerrarSesion()
                        else -> Toast.makeText(this@DashboardActivity, "Opción no manejada", Toast.LENGTH_SHORT).show()
                    }
                    drawerLayout.removeDrawerListener(this)
                }
            })
            true
        }

        val headerView = navigationView.getHeaderView(0)
        val txtNombre = headerView.findViewById<TextView>(R.id.txtNombreUsuario)
        val txtCorreo = headerView.findViewById<TextView>(R.id.txtCorreoUsuario)
        val imgPerfil = headerView.findViewById<ImageView>(R.id.imgPerfil)

        val fotoPath = prefs.getString("usuario_foto", null)
        if (!fotoPath.isNullOrEmpty()) {
            val fotoFile = File(fotoPath)
            if (fotoFile.exists()) {
                Glide.with(this)
                    .load(fotoFile)
                    .circleCrop()
                    .placeholder(R.drawable.user) // por si no carga
                    .into(imgPerfil)
            } else {
                imgPerfil.setImageResource(R.drawable.user)
            }
        } else {
            imgPerfil.setImageResource(R.drawable.user)
        }
        txtNombre.text = prefs.getString("usuario_nombre", "Nombre")
        txtCorreo.text = prefs.getString("usuario_correo", "Correo")

        ViewCompat.setOnApplyWindowInsetsListener(headerView) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                topInset + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in,
                android.R.animator.fade_out,
                android.R.animator.fade_in,
                android.R.animator.fade_out
            )
            .replace(R.id.contenedor_main, fragment)
            .commit()
    }

    private fun cerrarSesion() {
        getSharedPreferences("MyRoomyPrefs", MODE_PRIVATE).edit { clear() }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

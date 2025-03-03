package com.chopcode.trasnportenataga_laplata.services;

import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Servicio encargado de registrar un usuario en Firebase.
 */
public class RegistroService {

    // Instancia de FirebaseAuth para la autenticación.
    private FirebaseAuth auth;
    // Referencia a la base de datos, nodo "usuarios".
    private DatabaseReference databaseReference;

    /**
     * Interfaz para comunicar el resultado del registro.
     */
    public interface RegistroCallback {
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Constructor que inicializa FirebaseAuth y la referencia a la base de datos.
     */
    public RegistroService() {
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");
    }

    /**
     * Registra un usuario nuevo utilizando correo y contraseña.
     *
     * @param nombre   Nombre completo del usuario.
     * @param correo   Correo electrónico.
     * @param telefono Teléfono (puede ser opcional).
     * @param password Contraseña.
     * @param callback Callback para notificar éxito o error.
     */
    public void registrarUsuario(String nombre, String correo, String telefono, String password, RegistroCallback callback) {
        // Crear usuario con Firebase Authentication.
        auth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Obtener el UID del usuario recién creado.
                        String uid = auth.getCurrentUser().getUid();

                        // Crear el objeto Usuario. Aquí, por defecto, lo definimos como "pasajero".
                        Pasajero pasajero = new Pasajero(uid, nombre, telefono, correo, "pasajero");

                        // Guardar los datos del usuario en la base de datos, bajo el nodo "usuarios".
                        databaseReference.child(uid).setValue(pasajero)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    } else {
                        // Error al crear el usuario en Firebase Auth.
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Error desconocido");
                    }
                });
    }
}

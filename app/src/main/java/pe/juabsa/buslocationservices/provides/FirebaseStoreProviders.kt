package pe.juabsa.buslocationservices.provides

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import pe.juabsa.buslocationservices.model.Empresa


class FirebaseStoreProviders {

    var db = FirebaseFirestore.getInstance()


    fun getAllEmpresas(): List<Empresa>? {
        db.collection("empresa")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        Log.d("FirebaseStoreProviders", document.id + " => " + document.data)
                    }
                } else {
                    Log.w("FirebaseStoreProviders", "Error getting documents.", task.exception)
                }
            }
        return null
    }


}
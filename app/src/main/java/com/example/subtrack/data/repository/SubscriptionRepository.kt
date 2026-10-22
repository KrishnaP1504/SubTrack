// data/repository/SubscriptionRepository.kt

package com.example.subtrack.data.repository

import com.example.subtrack.data.local.entity.Subscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SubscriptionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userDocRef
        get() = auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
        }

    private val collectionRef
        get() = userDocRef?.collection("subscriptions")

    // Expose Flow using Firestore realtime snapshots that dynamically responds to auth state changes
    val allActiveSubscriptions: Flow<List<Subscription>> = callbackFlow {
        var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            firestoreListener?.remove()
            firestoreListener = null

            if (user == null) {
                trySend(emptyList())
            } else {
                val ref = db.collection("users").document(user.uid).collection("subscriptions")
                firestoreListener = ref.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("SubscriptionRepo", "Snapshot error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val subs = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Subscription::class.java)
                        }
                        trySend(subs)
                    }
                }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListener?.remove()
        }
    }

    /**
     * Connects user profile to Firestore under users/{uid}
     */
    suspend fun saveUserProfile(phoneNumber: String) {
        val doc = userDocRef ?: return
        val userData = hashMapOf(
            "uid" to (auth.currentUser?.uid ?: ""),
            "phoneNumber" to phoneNumber,
            "lastLoginAt" to System.currentTimeMillis()
        )
        doc.set(userData, SetOptions.merge()).await()
    }

    /**
     * Queries Firestore to auto-detect any subscriptions previously saved for this user
     */
    suspend fun getExistingSubscriptions(): List<Subscription> {
        val ref = collectionRef ?: return emptyList()
        return try {
            val snapshot = ref.get().await()
            snapshot.documents.mapNotNull { it.toObject(Subscription::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSubscription(subscription: Subscription) {
        val ref = collectionRef ?: return
        ref.document(subscription.id).set(subscription).await()
    }

    suspend fun addMultipleSubscriptions(subscriptions: List<Subscription>) {
        val ref = collectionRef ?: return
        val batch = db.batch()
        for (sub in subscriptions) {
            val doc = ref.document(sub.id)
            batch.set(doc, sub)
        }
        batch.commit().await()
    }

    suspend fun updateSubscription(subscription: Subscription) {
        val ref = collectionRef ?: return
        ref.document(subscription.id).set(subscription).await()
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        val ref = collectionRef ?: return
        ref.document(subscription.id).delete().await()
    }
}
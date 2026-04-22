// data/repository/SubscriptionRepository.kt

package com.example.subtrack.data.repository

import com.example.subtrack.data.local.entity.Subscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SubscriptionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val collectionRef
        get() = auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("subscriptions")
        }

    // Expose Flow using Firestore realtime snapshots
    val allActiveSubscriptions: Flow<List<Subscription>> = callbackFlow {
        val ref = collectionRef
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = ref
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val subs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Subscription::class.java)
                    }
                    trySend(subs)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addSubscription(subscription: Subscription) {
        val ref = collectionRef ?: return
        ref.document(subscription.id).set(subscription).await()
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
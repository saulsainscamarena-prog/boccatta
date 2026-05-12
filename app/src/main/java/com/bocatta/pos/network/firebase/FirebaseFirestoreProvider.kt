package com.bocatta.pos.network.firebase

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Centralised provider for FirebaseFirestore instance.
 * Allows easy substitution/mocking and isolates Firestore usage from repositories.
 */
object FirebaseFirestoreProvider {
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
}


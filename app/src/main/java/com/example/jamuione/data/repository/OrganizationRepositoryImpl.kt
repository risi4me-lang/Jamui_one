package com.example.jamuione.data.repository

import android.net.Uri
import com.example.jamuione.domain.model.*
import com.example.jamuione.domain.repository.OrganizationRepository
import com.example.jamuione.util.Resource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject

class OrganizationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : OrganizationRepository {

    override fun createOrganization(organization: Organization, logoUri: Uri?): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val orgId = UUID.randomUUID().toString()
            var logoUrl: String? = null
            
            if (logoUri != null) {
                val ref = storage.reference.child("org_logos/$orgId")
                ref.putFile(logoUri).await()
                logoUrl = ref.downloadUrl.await().toString()
            }

            val finalOrg = organization.copy(
                organizationId = orgId,
                logoUrl = logoUrl,
                createdAt = System.currentTimeMillis(),
                state = organization.state.lowercase(),
                district = organization.district.lowercase()
            )

            val batch = firestore.batch()
            val orgRef = firestore.collection("organizations").document(orgId)
            val adminRef = orgRef.collection("admins").document(organization.createdBy)

            batch.set(orgRef, finalOrg)
            batch.set(adminRef, OrganizationAdmin(organization.createdBy, "OWNER", System.currentTimeMillis()))

            withTimeout(20000L) {
                batch.commit().await()
            }
            
            emit(Resource.Success(orgId))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create organization"))
        }
    }

    override fun getOrganization(orgId: String): Flow<Resource<Organization>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("organizations").document(orgId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to fetch organization"))
                    return@addSnapshotListener
                }
                snapshot?.toObject(Organization::class.java)?.let {
                    trySend(Resource.Success(it))
                } ?: trySend(Resource.Error("Organization not found"))
            }
        awaitClose { listener.remove() }
    }

    override fun getUserManagedOrganizations(userId: String): Flow<Resource<List<Organization>>> = flow {
        emit(Resource.Loading())
        try {
            // This requires a collectionGroup query or a field in organizations doc. 
            // For MVP, we'll search organizations where createdBy == userId.
            val snapshot = firestore.collection("organizations")
                .whereEqualTo("createdBy", userId)
                .get()
                .await()
            emit(Resource.Success(snapshot.toObjects(Organization::class.java)))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch managed organizations"))
        }
    }

    override fun getOrganizationsByLocation(
        state: String?,
        district: String?,
        type: OrganizationType?
    ): Flow<Resource<List<Organization>>> = callbackFlow {
        trySend(Resource.Loading())
        var query: Query = firestore.collection("organizations")
        
        if (state != null) query = query.whereEqualTo("state", state.lowercase())
        if (district != null) query = query.whereEqualTo("district", district.lowercase())
        if (type != null) query = query.whereEqualTo("type", type.name)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch organizations"))
                return@addSnapshotListener
            }
            snapshot?.let {
                trySend(Resource.Success(it.toObjects(Organization::class.java)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun followOrganization(orgId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val batch = firestore.batch()
            val orgRef = firestore.collection("organizations").document(orgId)
            val followerRef = orgRef.collection("followers").document(userId)

            batch.set(followerRef, mapOf("userId" to userId, "followedAt" to System.currentTimeMillis()))
            batch.update(orgRef, "followerCount", FieldValue.increment(1))

            batch.commit().await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to follow"))
        }
    }

    override fun unfollowOrganization(orgId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val batch = firestore.batch()
            val orgRef = firestore.collection("organizations").document(orgId)
            val followerRef = orgRef.collection("followers").document(userId)

            batch.delete(followerRef)
            batch.update(orgRef, "followerCount", FieldValue.increment(-1))

            batch.commit().await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to unfollow"))
        }
    }

    override fun observeIsFollowing(orgId: String, userId: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("organizations").document(orgId)
            .collection("followers").document(userId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    override fun createAnnouncement(announcement: OrganizationAnnouncement, imageUri: Uri?): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val annId = UUID.randomUUID().toString()
            var imageUrl: String? = null
            if (imageUri != null) {
                val ref = storage.reference.child("org_announcements/$annId")
                ref.putFile(imageUri).await()
                imageUrl = ref.downloadUrl.await().toString()
            }

            val finalAnnouncement = announcement.copy(id = annId, imageUrl = imageUrl, timestamp = System.currentTimeMillis())
            firestore.collection("organizations").document(announcement.organizationId)
                .collection("announcements").document(annId)
                .set(finalAnnouncement)
                .await()
            
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to post announcement"))
        }
    }

    override fun getAnnouncements(orgId: String): Flow<Resource<List<OrganizationAnnouncement>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("organizations").document(orgId)
            .collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to fetch announcements"))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(Resource.Success(it.toObjects(OrganizationAnnouncement::class.java)))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun addAdmin(orgId: String, userId: String, role: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("organizations").document(orgId)
                .collection("admins").document(userId)
                .set(OrganizationAdmin(userId, role, System.currentTimeMillis()))
                .await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add admin"))
        }
    }

    override fun getAdmins(orgId: String): Flow<Resource<List<OrganizationAdmin>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("organizations").document(orgId)
            .collection("admins")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to fetch admins"))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(Resource.Success(it.toObjects(OrganizationAdmin::class.java)))
                }
            }
        awaitClose { listener.remove() }
    }
}

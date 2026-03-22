package com.rebook.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.rebook.app.data.local.AppDatabase
import com.rebook.app.data.model.Book
import com.rebook.app.data.model.BookStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BookRepository(context: Context) {

    companion object {
        private const val SYNC_LIMIT = 50L
    }

    private val bookDao = AppDatabase.getInstance(context).bookDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val booksRef = firestore.collection("books")

    fun getAllBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { entities -> entities.map { Book.fromEntity(it) } }

    fun getBooksByOwner(ownerId: String): Flow<List<Book>> =
        bookDao.getBooksByOwner(ownerId).map { entities -> entities.map { Book.fromEntity(it) } }

    suspend fun getBookById(bookId: String): Book? =
        bookDao.getBookById(bookId)?.let { Book.fromEntity(it) }

    suspend fun syncBooksFromFirestore() {
        try {
            val snapshot = booksRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(SYNC_LIMIT)
                .get()
                .await()
            val entities = snapshot.documents.mapNotNull { doc ->
                val ownerId = doc.getString("ownerId").orEmpty()
                if (ownerId.isEmpty()) {
                    android.util.Log.w(
                        "BookRepository",
                        "Sync: book ${doc.id} has missing or blank ownerId"
                    )
                }
                com.rebook.app.data.local.entity.BookEntity(
                    id = doc.id,
                    title = doc.getString("title").orEmpty(),
                    author = doc.getString("author").orEmpty(),
                    description = doc.getString("description").orEmpty(),
                    imageUrl = doc.getString("imageUrl"),
                    ownerId = ownerId,
                    ownerName = doc.getString("ownerName").orEmpty(),
                    status = doc.getString("status") ?: "AVAILABLE",
                    requestedById = doc.getString("requestedById"),
                    createdAt = doc.readCreatedAtMillis()
                )
            }
            bookDao.insertBooks(entities)
        } catch (e: Exception) {
            android.util.Log.e("BookRepository", "Sync failed: ${e.message}", e)
        }
    }

    suspend fun addBook(book: Book): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not logged in"))
            val now = System.currentTimeMillis()
            val bookData = hashMapOf(
                "title" to book.title,
                "author" to book.author,
                "description" to book.description,
                "imageUrl" to book.imageUrl,
                "ownerId" to currentUser.uid,
                "ownerName" to (currentUser.displayName ?: ""),
                "status" to BookStatus.AVAILABLE.name,
                "requestedById" to null,
                "createdAt" to now
            )
            val docRef = booksRef.add(bookData).await()
            val entity = book.toEntity().copy(
                id = docRef.id,
                ownerId = currentUser.uid,
                ownerName = currentUser.displayName ?: "",
                status = BookStatus.AVAILABLE.name,
                createdAt = now
            )
            bookDao.insertBook(entity)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBook(book: Book): Result<Unit> {
        return try {
            val updates = mapOf(
                "title" to book.title,
                "author" to book.author,
                "description" to book.description,
                "imageUrl" to book.imageUrl,
                "status" to book.status.name,
                "requestedById" to book.requestedById
            )
            booksRef.document(book.id).update(updates).await()
            bookDao.updateBook(book.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBook(bookId: String): Result<Unit> {
        return try {
            booksRef.document(bookId).delete().await()
            bookDao.deleteBookById(bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestBook(bookId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not logged in"))
            val updates = mapOf(
                "status" to BookStatus.REQUESTED.name,
                "requestedById" to currentUser.uid
            )
            booksRef.document(bookId).update(updates).await()
            val cached = bookDao.getBookById(bookId)
            if (cached != null) {
                bookDao.updateBook(
                    cached.copy(
                        status = BookStatus.REQUESTED.name,
                        requestedById = currentUser.uid
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveRequest(bookId: String): Result<Unit> {
        return try {
            booksRef.document(bookId).update("status", BookStatus.LENT.name).await()
            val cached = bookDao.getBookById(bookId)
            if (cached != null) {
                bookDao.updateBook(cached.copy(status = BookStatus.LENT.name))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unrequestBook(bookId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to BookStatus.AVAILABLE.name,
                "requestedById" to null
            )
            booksRef.document(bookId).update(updates).await()
            val cached = bookDao.getBookById(bookId)
            if (cached != null) {
                bookDao.updateBook(
                    cached.copy(
                        status = BookStatus.AVAILABLE.name,
                        requestedById = null
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadBookImage(imageUri: Uri): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val fileName = "${uid}_${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("book_images/$fileName")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun DocumentSnapshot.readCreatedAtMillis(): Long {
    val v = get("createdAt") ?: return 0L
    return when (v) {
        is Long -> v
        is Number -> v.toLong()
        is Timestamp -> v.toDate().time
        else -> 0L
    }
}

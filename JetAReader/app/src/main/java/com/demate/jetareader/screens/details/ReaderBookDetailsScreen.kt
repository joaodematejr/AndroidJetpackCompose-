package com.demate.jetareader.screens.details

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

import com.demate.jetareader.components.ReaderAppBar
import com.demate.jetareader.components.RounderButton
import com.demate.jetareader.data.Resource
import com.demate.jetareader.model.Item
import com.demate.jetareader.model.MBook
import com.demate.jetareader.navigation.ReaderScreens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BookDetailsScreen(
    navController: NavController,
    bookId: String,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    Scaffold(topBar = {
        ReaderAppBar(
            title = "Book Details",
            icon = Icons.Default.ArrowBack,
            showProfile = false,
            navController = navController
        ) {
            navController.navigate(ReaderScreens.SearchScreen.name)
        }
    }
    ) {
        it.calculateTopPadding()
        Surface(
            modifier = Modifier
                .padding(3.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bookInfo = produceState<Resource<Item>>(initialValue = Resource.Loading()) {
                    value = viewModel.getBookDetails(bookId)
                }.value

                if (bookInfo.data == null) {
                    Row {
                        LinearProgressIndicator()
                        Text(text = "Loading Book Details")
                    }
                } else {
                    ShowBookDetails(bookInfo, navController)
                }
            }
        }
    }

}

@Composable
fun ShowBookDetails(bookInfo: Resource<Item>, navController: NavController) {
    val bookData = bookInfo.data?.volumeInfo
    val googleBookId = bookInfo.data?.id

    Card(
        modifier = Modifier.padding(34.dp),
        shape = CircleShape,
    ) {
        if (bookData != null) {
            Image(
                painter = rememberAsyncImagePainter(model = bookData.imageLinks.thumbnail),
                contentDescription = "Book Img",
                modifier = Modifier
                    .width(90.dp)
                    .height(90.dp)
                    .padding(1.dp)
            )
        }
    }
    Text(
        text = bookData?.title ?: "No Title",
        style = MaterialTheme.typography.headlineSmall,
        overflow = TextOverflow.Ellipsis,
        maxLines = 19,
    )
    Spacer(modifier = Modifier.height(5.dp))

    Text(
        text = bookData?.authors?.joinToString(", ") ?: "No Author",
        style = MaterialTheme.typography.headlineSmall,
        overflow = TextOverflow.Ellipsis,
        maxLines = 19,
    )

    Spacer(modifier = Modifier.height(5.dp))

    Text(
        text = bookData?.description ?: "No Description",
        style = MaterialTheme.typography.headlineSmall,
        overflow = TextOverflow.Ellipsis,
        maxLines = 19,
    )

    val clearDescription = HtmlCompat.fromHtml(
        bookInfo.data?.volumeInfo?.description ?: "",
        HtmlCompat.FROM_HTML_MODE_LEGACY
    )
    val localDims = LocalContext.current.resources.displayMetrics
    Surface(
        modifier = Modifier
            .height(localDims.heightPixels.dp.times(0.09f))
            .padding(3.dp),
        shape = RectangleShape,
        border = BorderStroke(1.dp, Color.DarkGray)
    ) {
        LazyColumn(modifier = Modifier.padding(3.dp)) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                if (bookData != null) {
                    Text(
                        text = clearDescription.toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    //btn
    Spacer(modifier = Modifier.height(5.dp))
    Row(
        modifier = Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        RounderButton(
            label = "Save",
        ) {
            val book = MBook(
                title = bookData?.title,
                authors = bookData?.authors.toString(),
                description = bookData?.description,
                categories = bookData?.categories.toString(),
                notes = "",
                photoUrl = bookData?.imageLinks?.thumbnail,
                publishedDate = bookData?.publishedDate,
                pageCount = bookData?.pageCount.toString(),
                rating = 0.0,
                googleBookId = googleBookId,
                userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
            )
            saveToFirebase(book, navController)
        }
        Spacer(modifier = Modifier.width(25.dp))
        RounderButton(
            label = "Cancel"
        ) {
            navController.popBackStack()
        }
    }
}

fun saveToFirebase(book: MBook, navController: NavController) {
    //save this book to the firestore database
    val db = FirebaseFirestore.getInstance()
    val dbCollection = db.collection("books")
    if (book.toString().isNotEmpty()) {
        dbCollection.add(book).addOnSuccessListener { documentRef ->
            val docId = documentRef.id
            dbCollection.document(docId).update(hashMapOf("id" to docId) as Map<String, Any>)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        navController.popBackStack()
                    }
                }.addOnFailureListener {
                    Log.w("TAG", "DocumentSnapshot successfully written!")
                }
        }
    }

}

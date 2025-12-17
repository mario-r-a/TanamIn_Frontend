package com.mario.tanamin.ui.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mario.tanamin.ui.viewmodel.QuizResult
import com.mario.tanamin.ui.viewmodel.StartQuizViewModel

// --- [PERUBAHAN 1] Tambahkan import untuk ExperimentalMaterial3Api ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartQuizView(
    navController: NavController,
    levelId: Int,
    levelName: String,
    viewModel: StartQuizViewModel = viewModel()
) {
    // State dari ViewModel (tidak berubah)
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val submitted by viewModel.submitted.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val questionIndex by viewModel.currentQuestionIndex.collectAsState()
    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val quizResult by viewModel.quizResult.collectAsState()
    val isSubmitting by viewModel.isSubmittingResult.collectAsState()
    val currentQuestion = quizQuestions.getOrNull(questionIndex)
    val options = currentQuestion?.let { listOf(it.option1, it.option2, it.option3, it.option4) } ?: emptyList()

    // --- [PERUBAHAN 2] State untuk mengontrol BottomSheet ---
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { false }
    )


    LaunchedEffect(levelId) {
        viewModel.loadQuestions(levelId)
    }

    if (quizResult != null) {
        QuizResultView(
            result = quizResult!!,
            onBackToCourse = {
                // Kembali ke CourseView dengan membersihkan riwayat kuis
                navController.popBackStack()
            }
        )
    } else {
        val topGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFfeac57), Color(0xFFffca91), Color.White),
            startY = 0f,
            endY = 400f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ... (Box untuk gradient tidak berubah)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(topGradient)
                    .align(Alignment.TopCenter)
            ) {}


            // --- [PERUBAHAN 3] Logika Utama Tampilan (Loading/Error/Success) ---
            when {
                isLoading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator()
                    }
                }
                !errorMessage.isNullOrBlank() -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Error: $errorMessage", color = Color.Red)
                    }
                }
                currentQuestion != null -> {
                    // UI Utama Kuis (Pertanyaan dan Opsi)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .padding(top = 40.dp)
                    ) {
                        // ... (Kolom Pertanyaan tidak berubah)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.70f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(text = "Level $levelId", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222B45), modifier = Modifier.padding(start = 44.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = levelName, fontSize = 14.sp, color = Color(0xFF222B45))
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(text = currentQuestion.question, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF222B45))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Select the correct answer", fontSize = 12.sp, color = Color(0xFF222B45))
                            Spacer(modifier = Modifier.height(80.dp))
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Investment", tint = Color(0xFFd3842f), modifier = Modifier.size(64.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // ... (Kolom Opsi dan Tombol Submit tidak berubah)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            options.forEachIndexed { idx, option ->
                                val isSelected = selectedIndex == idx
                                val bg = when {
                                    submitted && isCorrect == true && isSelected -> Color(0xFF8CD87D)
                                    submitted && isCorrect == false && isSelected -> Color(0xFFFFEBEE)
                                    isSelected -> Color(0xFFFFF0D9)
                                    else -> Color.White
                                }
                                val strokeColor = if (isSelected) Color(0xFFd3842f) else Color.LightGray.copy(alpha = 0.6f)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = bg,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(
                                            BorderStroke(1.dp, strokeColor),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !submitted) { viewModel.selectAnswer(idx) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = option, fontSize = 12.sp, color = Color(0xFF222B45))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(100.dp))

                            Button(
                                onClick = { viewModel.submitAnswer() },
                                enabled = selectedIndex != null && !submitted,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd3842f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(text = if (!submitted) "Submit" else "Submitted", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
                else -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("No questions found for this level.")
                    }
                }
            }

            // --- [PERUBAHAN 4] Implementasi ModalBottomSheet ---
            // Tampilkan BottomSheet jika jawaban sudah disubmit
            if (submitted) {
                ModalBottomSheet(
                    onDismissRequest = { /* Kita tidak ingin user bisa menutupnya dengan swipe */ },
                    sheetState = sheetState,
                    containerColor = if (isCorrect == true) Color(0xFFE8F5E9) else Color(0xFFFFEBEE) // Warna background sesuai hasil
                ) {
                    // Tampilkan isi BottomSheet sesuai jawaban benar atau salah
                    if (isCorrect == true) {
                        CorrectAnswerSheetContent(
                            onNextClick = { viewModel.nextQuestion() }
                        )
                    } else {
                        IncorrectAnswerSheetContent(
                            correctAnswer = currentQuestion?.answer ?: "N/A",
                            onNextClick = { viewModel.nextQuestion() }
                        )
                    }
                }
            }
        }
    }
}

// --- [PERUBAHAN 5] Composable baru untuk isi BottomSheet jika jawaban BENAR ---
@Composable
fun CorrectAnswerSheetContent(onNextClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Correct",
            tint = Color(0xFF388E3C),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "You are correct!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF388E3C)
        )
        Text(
            text = "Good job!",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onNextClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// --- [PERUBAHAN 6] Composable baru untuk isi BottomSheet jika jawaban SALAH ---
@Composable
fun IncorrectAnswerSheetContent(correctAnswer: String, onNextClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = "Incorrect",
            tint = Color(0xFFD32F2F),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "Incorrect!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F)
        )
        Text(
            text = "The correct answer was:",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        // Menampilkan jawaban yang benar
        Text(
            text = correctAnswer,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
        Button(
            onClick = onNextClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun QuizResultView(
    result: QuizResult, // Menerima data hasil dari ViewModel
    onBackToCourse: () -> Unit // Aksi untuk kembali ke CourseView
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quiz Completed!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222B45)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Persentase
        Text(
            text = "${result.percentage}%",
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (result.percentage >= 60) Color(0xFF388E3C) else Color(0xFFD32F2F)
        )
        Text(
            text = "You answered ${result.score} out of ${result.totalQuestions} questions correctly.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Koin yang didapat
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Paid,
                contentDescription = "Coins",
                tint = Color(0xFFFFB86C),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "+${result.coinsEarned} Gems",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pesan Streak
        Text(
            text = result.streakMessage,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Tombol Kembali ke Course
        Button(
            onClick = onBackToCourse,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd3842f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Back to Courses", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CorrectAnswerPreview() {
    CorrectAnswerSheetContent(onNextClick = {})
}

@Preview(showBackground = true)
@Composable
private fun IncorrectAnswerPreview() {
    IncorrectAnswerSheetContent(correctAnswer = "Corporate Bond", onNextClick = {})
}
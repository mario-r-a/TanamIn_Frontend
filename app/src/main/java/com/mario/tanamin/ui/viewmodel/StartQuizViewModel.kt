package com.mario.tanamin.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.DataQuestion
import com.mario.tanamin.data.repository.TanamInRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// data class khusus untuk Result (Tidak berubah)
data class QuizResult(
    val score: Int,
    val totalQuestions: Int,
    val percentage: Int,
    val coinsEarned: Int,
    val streakMessage: String
)

class StartQuizViewModel(
    private val repository: TanamInRepository = TanamInContainer().tanamInRepository
) : ViewModel() {

    // --- State untuk menyimpan Level ID ---
    private var currentLevelId: Int = 0

    private val _allQuestions = MutableStateFlow<List<DataQuestion>>(emptyList())
    private val _quizQuestions = MutableStateFlow<List<DataQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<DataQuestion>> = _quizQuestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _score = MutableStateFlow(0)

    private val _selectedIndex = MutableStateFlow<Int?>(null)
    val selectedIndex: StateFlow<Int?> = _selectedIndex.asStateFlow()

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect: StateFlow<Boolean?> = _isCorrect.asStateFlow()

    private val _quizResult = MutableStateFlow<QuizResult?>(null)
    val quizResult: StateFlow<QuizResult?> = _quizResult.asStateFlow()

    // --- [PERUBAHAN 1] State baru untuk menandakan proses PENGIRIMAN DATA ---
    private val _isSubmittingResult = MutableStateFlow(false)
    val isSubmittingResult: StateFlow<Boolean> = _isSubmittingResult.asStateFlow()


    fun loadQuestions(levelId: Int) {
        currentLevelId = levelId
        viewModelScope.launch {
            _isLoading.value = true
            reset()
            try {
                val result = repository.getQuestionsByLevel(levelId)
                result.fold(
                    onSuccess = { list ->
                        _allQuestions.value = list
                        if (list.isNotEmpty()) {
                            _quizQuestions.value = list.shuffled().take(5)
                        } else {
                            _errorMessage.value = "No questions available for this level."
                        }
                    },
                    onFailure = { t -> _errorMessage.value = t.message ?: "Failed to load questions" }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load questions"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAnswer(index: Int) {
        if (_submitted.value) return
        _selectedIndex.value = index
    }

    private fun optionTextOf(question: DataQuestion, index: Int): String {
        return when (index) {
            0 -> question.option1; 1 -> question.option2; 2 -> question.option3; 3 -> question.option4; else -> ""
        }
    }

    fun submitAnswer() {
        val sel = _selectedIndex.value ?: return
        val currentQuestion = _quizQuestions.value.getOrNull(_currentQuestionIndex.value) ?: return
        val selectedText = optionTextOf(currentQuestion, sel)
        val isAnswerCorrect = selectedText == currentQuestion.answer
        _isCorrect.value = isAnswerCorrect
        if (isAnswerCorrect) {
            _score.value++
        }
        _submitted.value = true
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _quizQuestions.value.size - 1) {
            _currentQuestionIndex.value++
            _selectedIndex.value = null
            _submitted.value = false
            _isCorrect.value = null
        } else {
            // Panggil fungsi baru untuk mengirim data
            submitResultToServer()
        }
    }

    // fungsi untuk mengirim data ke server
    private fun submitResultToServer() {
        viewModelScope.launch {
            _isSubmittingResult.value = true
            _errorMessage.value = null

            val total = _quizQuestions.value.size
            if (total == 0) {
                Log.e("StartQuizViewModel", "submitResultToServer: No questions, skipping")
                _isSubmittingResult.value = false
                return@launch
            }

            val finalScore = _score.value
            val percentage = (finalScore * 100) / total

            // Range check
            val coinsEarned = when {
                percentage == 100 -> 15
                percentage >= 80 -> 8
                percentage >= 60 -> 6
                percentage >= 40 -> 4
                percentage >= 20 -> 2
                else -> 0
            }
            val claimStreak = percentage >= 20

            Log.d("StartQuizViewModel", "Quiz finished: score=$finalScore/$total, percentage=$percentage%, coins=$coinsEarned, claimStreak=$claimStreak, levelId=$currentLevelId")

            var streakMessage = "Try to achieve 1 correct answer to Extend Your Streak! I believe in You"

            try {
                // 1. Update Profile (Koin & Streak)
                Log.d("StartQuizViewModel", "Calling handleCourseCompletion...")
                val profileResult = repository.handleCourseCompletion(
                    coinDelta = coinsEarned,
                    claimStreak = claimStreak
                )
                profileResult.onSuccess { updatedProfile ->
                    Log.d("StartQuizViewModel", "Profile updated successfully: coin=${updatedProfile.coin}, streak=${updatedProfile.streak}")
                    if (claimStreak) {
                        streakMessage = "You extend your streak to ${updatedProfile.streak ?: 0} days"
                    }
                }.onFailure {
                    Log.e("StartQuizViewModel", "Failed to update profile", it)
                    _errorMessage.value = "Failed to update profile: ${it.message}"
                }

                // 2. Update Level (jika perlu)
                if (percentage >= 60) {
                    Log.d("StartQuizViewModel", "Calling updateLevel for levelId=$currentLevelId...")
                    val levelResult = repository.updateLevel(levelId = currentLevelId, isCompleted = true)
                    levelResult.onSuccess {
                        Log.d("StartQuizViewModel", "Level updated successfully: $it")
                    }.onFailure {
                        Log.e("StartQuizViewModel", "Failed to update level", it)
                        _errorMessage.value = "Failed to update level: ${it.message}"
                    }
                }

            } catch (e: Exception) {
                Log.e("StartQuizViewModel", "Exception during submitResultToServer", e)
                _errorMessage.value = "An error occurred: ${e.message}"
            } finally {
                // Cek jika tidak ada error dari proses di atas, baru navigasi
                if (_errorMessage.value == null) {
                    Log.d("StartQuizViewModel", "Setting quiz result, will navigate to result screen")
                    _quizResult.value = QuizResult(
                        score = finalScore,
                        totalQuestions = total,
                        percentage = percentage,
                        coinsEarned = coinsEarned,
                        streakMessage = streakMessage
                    )
                } else {
                    Log.e("StartQuizViewModel", "Error occurred, not showing result: ${_errorMessage.value}")
                }
                _isSubmittingResult.value = false
            }
        }
    }

    fun reset() {
        _errorMessage.value = null
        _allQuestions.value = emptyList()
        _quizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _score.value = 0
        _selectedIndex.value = null
        _submitted.value = false
        _isCorrect.value = null
        _quizResult.value = null
        _isSubmittingResult.value = false // Pastikan ini juga direset
    }
}

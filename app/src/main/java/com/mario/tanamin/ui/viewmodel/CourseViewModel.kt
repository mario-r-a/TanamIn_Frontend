package com.mario.tanamin.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.repository.TanamInRepository

class CourseViewModel(
    private val repository: TanamInRepository = TanamInContainer().tanamInRepository
): ViewModel() {

}
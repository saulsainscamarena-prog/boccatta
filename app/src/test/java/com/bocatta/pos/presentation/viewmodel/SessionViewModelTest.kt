package com.bocatta.pos.presentation.viewmodel

import org.junit.Assert.assertTrue
import org.junit.Test

class SessionViewModelTest {

    @Test
    fun sessionViewModel_extiendeBaseAndroidViewModel() {
        assertTrue(BaseAndroidViewModel::class.java.isAssignableFrom(SessionViewModel::class.java))
    }
}

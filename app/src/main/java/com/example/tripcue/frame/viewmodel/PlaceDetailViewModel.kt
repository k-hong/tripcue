package com.example.tripcue.frame.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.tripcue.frame.uicomponents.home.PlaceInfo

@HiltViewModel
class PlaceDetailViewModel @Inject constructor() : ViewModel() {
    var selectedPlace: PlaceInfo? = null
}

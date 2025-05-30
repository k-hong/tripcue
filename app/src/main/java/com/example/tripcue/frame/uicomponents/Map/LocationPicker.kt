package com.example.tripcue.frame.uicomponents.Map

import android.content.Context
import android.location.Geocoder
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

@Composable
fun LocationPickerDialog(
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedAddress by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("거주 지역 선택") },
        text = {
            AndroidView(
                factory = { ctx ->
                    val mapView = MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            600
                        )
                        onCreate(null)
                        onResume()
                        getMapAsync { googleMap ->
                            val defaultLocation = LatLng(37.5665, 126.9780) // 서울 시청
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
                            googleMap.setOnMapClickListener { latLng ->
                                val address = getAddressFromLatLng(ctx, latLng)
                                selectedAddress = address
                                googleMap.clear()
                                googleMap.addMarker(MarkerOptions().position(latLng).title(address))
                            }
                        }
                    }
                    mapView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedAddress.isNotBlank()) {
                    onLocationSelected(selectedAddress)
                }
                onDismiss()
            }) {
                Text("선택")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        addresses?.firstOrNull()?.getAddressLine(0) ?: "위치 확인 불가"
    } catch (e: Exception) {
        "주소 변환 실패"
    }
}

@Composable
fun RegionField(region: String, onRegionChange: (String) -> Unit) {
    var showMap by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = region,
        onValueChange = {},
        label = { Text("거주 지역") },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMap = true }
    )

    if (showMap) {
        LocationPickerDialog(
            onLocationSelected = { onRegionChange(it) },
            onDismiss = { showMap = false }
        )
    }
}

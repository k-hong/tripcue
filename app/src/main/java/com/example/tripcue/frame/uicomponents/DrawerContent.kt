package com.example.tripcue.frame.uicomponents

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripcue.R

@Composable
fun ColumnScope.DrawerContent() {
    Text(text = "username",
        modifier = Modifier.padding(16.dp),
        fontSize = 20.sp
        )
    Image(
        painter = painterResource(id = R.drawable.baseline_person_24),
        contentDescription = "프로필 이미지",
        modifier = Modifier
            .padding(16.dp)
            .height(180.dp)
            .fillMaxWidth(),
        contentScale = ContentScale.Fit
    )

    Text(text = "이메일이메일",
        modifier = Modifier.padding(16.dp),
        fontSize = 20.sp
    )
    NavigationDrawerItem(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = "Drawer Item1") },
        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "")},
        onClick = {},
        selected = false

    )
    Spacer(modifier = Modifier.height(8.dp))
    NavigationDrawerItem(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = "Drawer Item2") },
        onClick = {},
        selected = false

    )
}
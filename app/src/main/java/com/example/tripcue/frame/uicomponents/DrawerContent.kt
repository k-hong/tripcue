// ✅ DrawerContent.kt (지역 드롭다운 → 텍스트 입력으로 되돌림)
package com.example.tripcue.frame.uicomponents

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.tripcue.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ColumnScope.DrawerContent(
    isEditMode: Boolean,
    onEditClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var nickname by remember { mutableStateOf("username") }
    var selectedTags by remember { mutableStateOf(listOf<String>()) }
    var region by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf(user?.photoUrl?.toString() ?: "") }

    var newNickname by remember { mutableStateOf("") }
    var newRegion by remember { mutableStateOf("") }
    var newSelectedTags by remember { mutableStateOf(listOf<String>()) }

    var isLoading by remember { mutableStateOf(true) }

    val interestOptions = listOf(
        "느긋한", "관광지", "식도락", "사진", "쇼핑", "휴양",
        "모험", "자연", "역사", "전통", "문화", "야경"
    )

    LaunchedEffect(Unit) {
        isLoading = true
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    nickname = doc.getString("nickname") ?: "username"
                    region = doc.getString("region") ?: ""
                    selectedTags = doc.get("interests") as? List<String> ?: emptyList()
                    newNickname = nickname
                    newRegion = region
                    newSelectedTags = selectedTags
                    isLoading = false
                }
        }
    }

    if (!isEditMode) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = nickname, fontSize = 20.sp)
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "수정")
                }
            }
            Image(
                painter = if (photoUrl.isNotEmpty()) rememberAsyncImagePainter(photoUrl)
                else rememberAsyncImagePainter(R.drawable.baseline_person_24),
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .padding(16.dp)
                    .height(180.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            Text(text = "지역: $region", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 16.sp)
            Text(text = "관심사: ${selectedTags.joinToString()}", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 16.sp)
        }
    } else {
        OutlinedTextField(
            value = newNickname,
            onValueChange = { newNickname = it },
            label = { Text("닉네임") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ✅ 지역 텍스트 입력 필드 복원
        OutlinedTextField(
            value = newRegion,
            onValueChange = { newRegion = it },
            label = { Text("지역") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("관심사 선택", modifier = Modifier.padding(start = 16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .height(200.dp)
                .padding(8.dp)
        ) {
            items(interestOptions) { tag ->
                val selected = tag in newSelectedTags
                FilterChip(
                    selected = selected,
                    onClick = {
                        newSelectedTags = if (selected) {
                            newSelectedTags - tag
                        } else {
                            newSelectedTags + tag
                        }
                    },
                    label = { Text(tag) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = onDoneClick, modifier = Modifier.weight(1f)) {
                Text("이전")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val nicknameToSave = newNickname.trim()
                    val regionToSave = newRegion.trim()
                    val interestsToSave = newSelectedTags.toList()

                    user?.uid?.let { uid ->
                        db.collection("users").document(uid)
                            .update(
                                mapOf(
                                    "nickname" to nicknameToSave,
                                    "region" to regionToSave,
                                    "interests" to interestsToSave
                                )
                            )
                            .addOnSuccessListener {
                                nickname = nicknameToSave
                                region = regionToSave
                                selectedTags = interestsToSave
                                onDoneClick()
                            }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("저장")
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    NavigationDrawerItem(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = "Drawer Item1") },
        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "") },
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

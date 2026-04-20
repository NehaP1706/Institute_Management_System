package com.ims.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.CourseType
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.components.HeaderTopBar
import com.ims.app.ui.components.SectionHeader
import com.ims.app.ui.theme.*

@Composable
fun CourseFilterScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = viewModel.getFilteredCourses().filter {
        searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.courseCode.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            HeaderTopBar(
                title      = "Courses",
                userName   = viewModel.getCurrentUserName(),
                userRole   = viewModel.getCurrentUserRole(),
                onNavigate = onNavigate
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                isAdmin      = viewModel.isAdmin(),
                onNavigate   = onNavigate
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search courses…", color = OnSurface.copy(0.5f)) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = Primary) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Primary,
                    unfocusedBorderColor = Divider,
                    focusedTextColor     = OnBackground,
                    unfocusedTextColor   = OnBackground,
                    cursorColor          = Primary
                )
            )

            Spacer(Modifier.height(12.dp))

            SectionHeader("Filter by Type")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = viewModel.selectedCourseFilter == null,
                        onClick  = { viewModel.selectedCourseFilter = null },
                        label    = { Text("All", fontSize = 12.sp) },
                        colors   = filterChipColors()
                    )
                }
                items(CourseType.values().toList()) { type ->
                    FilterChip(
                        selected = viewModel.selectedCourseFilter == type,
                        onClick  = {
                            viewModel.selectedCourseFilter =
                                if (viewModel.selectedCourseFilter == type) null else type
                        },
                        label    = { Text(type.name, fontSize = 12.sp) },
                        colors   = filterChipColors()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "${filtered.size} course${if (filtered.size != 1) "s" else ""} found",
                color    = OnSurface,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { course ->
                    CourseDetailCard(
                        courseCode = course.courseCode,
                        title      = course.title,
                        credits    = course.credits,
                        type       = course.type.name,
                        semester   = course.semester,
                        instructor = course.instructorName
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseDetailCard(
    courseCode: String,
    title: String,
    credits: Int,
    type: String,
    semester: String,
    instructor: String
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title,      color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(courseCode, color = Primary,      fontSize   = 12.sp)
                }
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .background(PrimaryDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(type, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip(icon = Icons.Default.CreditScore, label = "$credits Credits")
                InfoChip(icon = Icons.Default.School,      label = semester)
                InfoChip(icon = Icons.Default.Person,      label = instructor)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, color = OnSurface, fontSize = 11.sp)
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Primary,
    selectedLabelColor     = OnPrimary,
    containerColor         = SurfaceVar,
    labelColor             = OnSurface
)

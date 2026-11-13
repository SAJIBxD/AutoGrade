package com.autograde.ui.assessment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autograde.ui.components.FieldLabel
import com.autograde.ui.components.PrimaryButton
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AutoGradeTheme
import com.autograde.ui.theme.Ink
import com.autograde.ui.theme.Line
import com.autograde.ui.theme.Muted

private val subjects = listOf("Operating System", "DBMS", "Discrete Math", "Data Structures")
private val classes = listOf("BSc 1st Year", "BSc 2nd Year", "BSc 3rd Year", "BSc Final Year")

/** OMR step 1 — exam details. */
@Composable
fun CreateAssessmentScreen(
    state: AssessmentUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onTitle: (String) -> Unit,
    onSubject: (String) -> Unit,
    onClass: (String) -> Unit,
    onDate: (String) -> Unit,
    onMarks: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Leaving step 1 ends the flow and discards it, so confirm if there's work to lose.
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestBack = { if (state.hasProgress) confirmDiscard = true else onBack() }
    BackHandler(enabled = state.hasProgress) { confirmDiscard = true }

    var pickingDate by remember { mutableStateOf(false) }

    OmrFlowScaffold(
        title = "New OMR assessment",
        step = 1,
        onBack = requestBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                "Continue",
                Modifier.fillMaxWidth(),
                enabled = state.detailsValid,
                onClick = onNext,
            )
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Labeled("Assessment title") {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitle,
                    singleLine = true,
                    placeholder = { Text("e.g. Midterm — Operating System", color = Muted) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    shape = FieldShape,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Labeled("Subject") {
                DropdownField(state.subject, subjects, onSubject)
            }
            Labeled("Class / batch") {
                DropdownField(state.className, classes, onClass)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Labeled("Exam date", Modifier.weight(1f)) {
                    PickerField(state.examDate, onClick = { pickingDate = true })
                }
                Labeled("Total marks", Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.totalMarks,
                        onValueChange = onMarks,
                        singleLine = true,
                        isError = (state.totalMarks.toIntOrNull() ?: 0) <= 0,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        shape = FieldShape,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (pickingDate) {
        ExamDatePicker(
            onPicked = { onDate(utcDateLabel(it)) },
            onDismiss = { pickingDate = false },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this assessment?") },
            text = { Text("The details, uploaded sheets and answer key will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text("Discard", color = MaterialErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = Accent) }
            },
            containerColor = Color.White,
        )
    }
}

private val FieldShape = RoundedCornerShape(12.dp)
private val MaterialErrorRed = Color(0xFFB3261E)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White,
    cursorColor = Accent,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
)

@Composable
private fun Labeled(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        FieldLabel(label)
        content()
    }
}

/** Looks like a text field, behaves like a button. */
@Composable
private fun PickerField(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(FieldShape)
            .background(Color.White)
            .border(1.dp, Line, FieldShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            value,
            fontSize = 16.sp,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("▾", color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun DropdownField(
    value: String,
    options: List<String>,
    onPick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PickerField(value, onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == value) Accent else Ink,
                            fontWeight = if (option == value) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onPick(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDatePicker(onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let(onPicked)
                    onDismiss()
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(pickerState)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FB)
@Composable
private fun CreateAssessmentPreview() {
    AutoGradeTheme {
        CreateAssessmentScreen(
            state = AssessmentUiState(title = "Midterm — Operating System"),
            onBack = {}, onNext = {},
            onTitle = {}, onSubject = {}, onClass = {}, onDate = {}, onMarks = {},
        )
    }
}

package com.autograde.ui.assessment

/**
 * Sample data for the OMR assessment flow. Everything here is placeholder content
 * standing in for endpoints the backend does not expose yet (OMR grading, class reports).
 */

data class StudentSubmission(
    val name: String,
    val id: String,
    val score: Int?,
    val needsReview: Boolean = false,
)

val sampleSubmissions = listOf(
    StudentSubmission("Arafat Hossain", "200301", 92),
    StudentSubmission("Nusrat Jahan", "200302", 88),
    StudentSubmission("Rakibul Islam", "200303", 76),
    StudentSubmission("Farhana Akter", "200304", 78),
    StudentSubmission("Tanvir Ahmed", "200305", 81),
    StudentSubmission("Sadia Rahman", "200306", 95),
    StudentSubmission("Imran Hossain", "200307", 64),
    StudentSubmission("Mehjabin Chowdhury", "200308", 89),
)

/** Mock answer key for the OMR setup screen: question index -> option. */
val sampleAnswerKey: List<String> = listOf(
    "B", "A", "C", "D", "B", "C", "A", "D", "B", "C",
    "A", "C", "D", "B", "B", "A", "C", "D", "C", "A",
)

/** Score distribution buckets for the class report, label to count. */
val scoreDistribution = listOf(
    "0-39" to 3,
    "40-59" to 5,
    "60-69" to 9,
    "70-79" to 14,
    "80-89" to 21,
    "90-100" to 8,
)

/** Question-wise average score for the class report. */
val questionPerformance = listOf(
    "Q1" to 0.87f,
    "Q2" to 0.82f,
    "Q3" to 0.76f,
    "Q4" to 0.89f,
    "Q5" to 0.85f,
)

/** Recent assessments on the dashboard. */
data class RecentAssessment(
    val title: String,
    val meta: String,
    val status: String,
)

val sampleRecentAssessments = listOf(
    RecentAssessment("CSE 310 — Mid Term", "BSc (Final Year) · 120 submissions", "Completed"),
    RecentAssessment("DBMS — Quiz 2", "BSc (3rd Year) · 67 submissions", "Processing"),
    RecentAssessment("Discrete Math — Final", "BSc (2nd Year) · 102 submissions", "Completed"),
)

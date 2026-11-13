package com.autograde.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.autograde.AutoGradeApp
import com.autograde.data.model.Profile
import com.autograde.ui.assessment.AnswerKeyScreen
import com.autograde.ui.assessment.AssessmentViewModel
import com.autograde.ui.assessment.CollectSubmissionsScreen
import com.autograde.ui.assessment.CreateAssessmentScreen
import com.autograde.ui.assessment.ProcessingScreen
import com.autograde.ui.assessment.SubmissionsListScreen
import com.autograde.ui.dashboard.BottomNavBar
import com.autograde.ui.dashboard.DashboardScreen
import com.autograde.ui.dashboard.ExamsScreen
import com.autograde.ui.dashboard.ReportsScreen
import com.autograde.ui.dashboard.navigateToTab
import com.autograde.ui.login.LoginScreen
import com.autograde.ui.profile.ProfileScreen
import com.autograde.ui.result.ResultScreen
import com.autograde.ui.theme.Bg

/** Every route in the app, so hosts and screens can't drift apart on names. */
object Routes {
    const val LOGIN = "login"

    // Bottom-tab destinations
    const val DASHBOARD = "dashboard"
    const val EXAMS = "exams"
    const val REPORTS = "reports"
    const val PROFILE = "profile"

    const val RESULT = "result"

    // Create-assessment OMR flow. A nested graph, so its ViewModel lives exactly
    // as long as the flow and a new assessment always starts from a clean slate.
    const val OMR_FLOW = "omr"
    const val CREATE = "omr/create"
    const val COLLECT = "omr/collect"
    const val ANSWER_KEY = "omr/answer_key"
    const val PROCESSING = "omr/processing"
    const val SUBMISSIONS = "omr/submissions"

    val tabs = setOf(DASHBOARD, EXAMS, REPORTS, PROFILE)
}

/** One- or two-letter badge for the dashboard / profile avatar. */
fun Profile.avatarInitials(): String {
    val named = listOfNotNull(firstName, lastName)
        .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
        .joinToString("")
    if (named.isNotBlank()) return named
    return email?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}

/**
 * The single-activity navigation graph, hosted by [com.autograde.MainActivity].
 *
 * One scaffold owns the bottom bar, which is shown only on tab destinations.
 * The OMR flow is a nested graph:
 * details → answer sheets (→ scan) → answer key → processing → submissions → report.
 */
@Composable
fun AutoGradeAppNav(
    startDestination: String = Routes.LOGIN,
    authUiState: AuthUiState = AuthUiState(),
    profileUiState: ProfileUiState = ProfileUiState(),
    statusText: String? = null,
    onGoogleClick: () -> Unit = {},
    onRequestOtp: (email: String) -> Unit = {},
    onVerifyOtp: (email: String, otp: String) -> Unit = { _, _ -> },
    onEditEmail: () -> Unit = {},
    onAuthSuccess: () -> Unit = {},
    onSaveProfile: (firstName: String, lastName: String, phone: String) -> Unit = { _, _, _ -> },
    onUploadPhoto: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val nav: NavHostController = rememberNavController()

    LaunchedEffect(authUiState.auth) {
        if (authUiState.auth != null) {
            onAuthSuccess()
            nav.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(profileUiState.loggedOut) {
        if (profileUiState.loggedOut) {
            nav.navigate(Routes.LOGIN) {
                popUpTo(nav.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val profile = profileUiState.profile

    Scaffold(
        containerColor = Bg,
        // Each screen handles the status bar itself; the bottom bar handles the nav bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute in Routes.tabs,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
            ) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { nav.navigateToTab(it.route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { enter(forward = true) },
            exitTransition = { exit(forward = true) },
            popEnterTransition = { enter(forward = false) },
            popExitTransition = { exit(forward = false) },
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    uiState = authUiState,
                    onRequestOtp = onRequestOtp,
                    onVerifyOtp = onVerifyOtp,
                    onEditEmail = onEditEmail,
                    onGoogleClick = onGoogleClick,
                )
            }

            // --- tabs ---
            composable(Routes.DASHBOARD) { entry ->
                DashboardScreen(
                    firstName = profile?.firstName,
                    avatarInitials = profile?.avatarInitials() ?: "?",
                    avatarUrl = profile?.imageUrl,
                    onStartMarking = { nav.navigateFrom(entry, Routes.OMR_FLOW) },
                    onOpenAssessment = { nav.navigateFrom(entry, Routes.RESULT) },
                    onSeeAll = { nav.navigateToTab(Routes.EXAMS) },
                    onOpenProfile = { nav.navigateToTab(Routes.PROFILE) },
                )
            }
            composable(Routes.EXAMS) { entry ->
                ExamsScreen(
                    onOpenAssessment = { nav.navigateFrom(entry, Routes.RESULT) },
                    onCreate = { nav.navigateFrom(entry, Routes.OMR_FLOW) },
                )
            }
            composable(Routes.REPORTS) { entry ->
                ReportsScreen(onOpenReport = { nav.navigateFrom(entry, Routes.RESULT) })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    uiState = profileUiState,
                    statusText = statusText,
                    onSave = onSaveProfile,
                    onUploadPhoto = onUploadPhoto,
                    onLogout = onLogout,
                )
            }

            composable(Routes.RESULT) { entry ->
                ResultScreen(onBack = { nav.popFrom(entry) })
            }

            // --- create-assessment OMR flow ---
            navigation(startDestination = Routes.CREATE, route = Routes.OMR_FLOW) {
                composable(Routes.CREATE) { entry ->
                    val vm = entry.omrViewModel(nav)
                    val state by vm.state.collectAsState()
                    CreateAssessmentScreen(
                        state = state,
                        onBack = { nav.popFrom(entry) },
                        onNext = { nav.navigateFrom(entry, Routes.COLLECT) },
                        onTitle = vm::onTitle,
                        onSubject = vm::onSubject,
                        onClass = vm::onClass,
                        onDate = vm::onDate,
                        onMarks = vm::onMarks,
                    )
                }
                composable(Routes.COLLECT) { entry ->
                    val vm = entry.omrViewModel(nav)
                    val state by vm.state.collectAsState()
                    CollectSubmissionsScreen(
                        state = state,
                        onBack = { nav.popFrom(entry) },
                        onUpload = vm::uploadSubmissions,
                        onRemove = vm::removeSubmission,
                        onContinue = { nav.navigateFrom(entry, Routes.ANSWER_KEY) },
                    )
                }
                composable(Routes.ANSWER_KEY) { entry ->
                    val vm = entry.omrViewModel(nav)
                    val state by vm.state.collectAsState()
                    AnswerKeyScreen(
                        state = state,
                        onBack = { nav.popFrom(entry) },
                        onQuestionCount = vm::setQuestionCount,
                        onAnswer = vm::setAnswer,
                        onEvaluate = { nav.navigateFrom(entry, Routes.PROCESSING) },
                    )
                }
                composable(Routes.PROCESSING) { entry ->
                    val vm = entry.omrViewModel(nav)
                    val state by vm.state.collectAsState()
                    ProcessingScreen(
                        sheetCount = state.sheetCount,
                        onDone = {
                            // Drop the setup screens: Back from the results goes home.
                            nav.navigateFrom(entry, Routes.SUBMISSIONS) {
                                popUpTo(Routes.CREATE) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.SUBMISSIONS) { entry ->
                    val vm = entry.omrViewModel(nav)
                    val state by vm.state.collectAsState()
                    SubmissionsListScreen(
                        assessmentTitle = state.title,
                        onBack = { nav.popFrom(entry) },
                        onViewReport = { nav.navigateFrom(entry, Routes.RESULT) },
                    )
                }
            }
        }
    }
}

// --- helpers ---

/** The shared OMR-flow ViewModel, scoped to the nested graph's back stack entry. */
@Composable
private fun NavBackStackEntry.omrViewModel(nav: NavHostController): AssessmentViewModel {
    val parent = remember(this) { nav.getBackStackEntry(Routes.OMR_FLOW) }
    val app = LocalContext.current.applicationContext as AutoGradeApp
    return viewModel(parent, factory = AppViewModelFactory(app))
}

/**
 * Navigate only if [entry] is the visible, settled screen. Stops double taps
 * (or taps during a transition) from pushing the same screen twice.
 */
private fun NavHostController.navigateFrom(
    entry: NavBackStackEntry,
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (entry.isResumed()) navigate(route, builder)
}

/** Pop only from the visible screen, so a double back-tap can't pop two screens. */
private fun NavHostController.popFrom(entry: NavBackStackEntry) {
    if (entry.isResumed()) popBackStack()
}

private fun NavBackStackEntry.isResumed() =
    lifecycle.currentState == Lifecycle.State.RESUMED

private fun NavBackStackEntry.isTab() = destination.route in Routes.tabs

private const val NAV_MS = 280

/** Tab ↔ tab cross-fades; everything else slides like a pushed screen. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(forward: Boolean): EnterTransition =
    if (initialState.isTab() && targetState.isTab()) {
        fadeIn(tween(200))
    } else {
        slideIntoContainer(
            if (forward) AnimatedContentTransitionScope.SlideDirection.Start
            else AnimatedContentTransitionScope.SlideDirection.End,
            tween(NAV_MS),
            initialOffset = { if (forward) it else it / 4 },
        ) + fadeIn(tween(NAV_MS))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(forward: Boolean): ExitTransition =
    if (initialState.isTab() && targetState.isTab()) {
        fadeOut(tween(200))
    } else {
        slideOutOfContainer(
            if (forward) AnimatedContentTransitionScope.SlideDirection.Start
            else AnimatedContentTransitionScope.SlideDirection.End,
            tween(NAV_MS),
            targetOffset = { if (forward) it / 4 else it },
        ) + fadeOut(tween(NAV_MS))
    }

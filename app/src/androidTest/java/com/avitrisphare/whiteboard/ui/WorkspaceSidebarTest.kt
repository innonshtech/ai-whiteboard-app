package com.avitrisphare.whiteboard.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertHasClickAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avitrisphare.whiteboard.presentation.dashboard.components.WorkspaceSidebar
import com.avitrisphare.whiteboard.domain.model.Folder
import com.avitrisphare.whiteboard.core.theme.AvitriSphareTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceSidebarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sidebarDisplaysFoldersAndTags() {
        val folders = listOf(Folder(id = "1", name = "Work", createdAt = 0L, updatedAt = 0L))
        val tags = listOf("Urgent", "Personal")

        composeTestRule.setContent {
            AvitriSphareTheme {
                WorkspaceSidebar(
                    folders = folders,
                    tags = tags,
                    onFolderClick = {},
                    onTagClick = {}
                )
            }
        }

        // Verify Workspace title
        composeTestRule.onNodeWithText("Workspace").assertIsDisplayed()
        
        // Verify All Notes option exists
        composeTestRule.onNodeWithText("All Notes").assertIsDisplayed().assertHasClickAction()

        // Verify custom folder exists
        composeTestRule.onNodeWithText("Work").assertIsDisplayed().assertHasClickAction()

        // Verify tags exist
        composeTestRule.onNodeWithText("Urgent").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("Personal").assertIsDisplayed().assertHasClickAction()
    }
}

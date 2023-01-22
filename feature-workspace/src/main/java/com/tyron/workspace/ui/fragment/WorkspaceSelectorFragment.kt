package com.tyron.workspace.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.blacksquircle.ui.core.ui.delegate.viewBinding
import com.blacksquircle.ui.core.ui.extensions.applySystemWindowInsets
import com.blacksquircle.ui.core.ui.extensions.navigate
import com.blacksquircle.ui.core.ui.navigation.Screen
import com.tyron.ui.feature.workspace.R
import com.tyron.ui.feature.workspace.databinding.FragmentWorkspaceSelectorBinding
import com.tyron.workspace.ui.viewmodel.WorkspaceSelectorViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class WorkspaceSelectorFragment : Fragment(R.layout.fragment_workspace_selector) {

    private val viewModel by activityViewModels<WorkspaceSelectorViewModel>()
    private val binding by viewBinding(FragmentWorkspaceSelectorBinding::bind)
    private val navController by lazy { findNavController() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeViewModel()

        view.applySystemWindowInsets(true) { _, top, _, _ ->
            binding.toolbar.updatePadding(top = top)
        }

        binding.toolbar.setOnClickListener {
            navController.navigate(Screen.Editor(File("/sdcard/AndroidIDEProjects/MyApplication/")))
        }
    }

    private fun observeViewModel() {

    }
}
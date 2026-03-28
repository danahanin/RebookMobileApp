package com.rebook.app.util

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.rebook.app.viewmodel.BookViewModel

fun Fragment.observeBookOperations(
    bookViewModel: BookViewModel,
    showProgress: (Boolean) -> Unit,
    onSuccess: () -> Unit = {}
) {
    bookViewModel.operationState.observe(viewLifecycleOwner) { state ->
        when (state) {
            is BookOperationState.Loading -> showProgress(true)
            is BookOperationState.Success -> {
                showProgress(false)
                onSuccess()
                bookViewModel.resetOperationState()
            }
            is BookOperationState.Error -> {
                showProgress(false)
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                bookViewModel.resetOperationState()
            }
            is BookOperationState.Idle -> showProgress(false)
        }
    }
}

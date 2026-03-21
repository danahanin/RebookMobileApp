package com.rebook.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.rebook.app.R
import com.rebook.app.databinding.FragmentEditProfileBinding
import com.rebook.app.util.UserOperationState
import com.rebook.app.viewmodel.UserViewModel
import com.squareup.picasso.Picasso

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var isSaving = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImageSelected(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentUser()
        setupClickListeners()
        observeOperationState()
        observeUploadedImageUrl()
    }

    private fun loadCurrentUser() {
        userViewModel.currentUser.value?.let { user ->
            binding.etDisplayName.setText(user.displayName)
            uploadedImageUrl = user.profileImageUrl
            loadProfileImage(user.profileImageUrl)
        }
    }

    private fun loadProfileImage(url: String?) {
        if (!url.isNullOrEmpty()) {
            Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivProfileImage)
        }
    }

    private fun setupClickListeners() {
        binding.layoutProfileImage.setOnClickListener { pickImage() }
        binding.tvChangePhoto.setOnClickListener { pickImage() }
        binding.btnSave.setOnClickListener { saveProfile() }
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleImageSelected(uri: Uri) {
        selectedImageUri = uri
        Picasso.get().load(uri).into(binding.ivProfileImage)
        uploadImage(uri)
    }

    private fun uploadImage(uri: Uri) {
        userViewModel.uploadProfileImage(uri)
    }

    private fun observeUploadedImageUrl() {
        userViewModel.uploadedImageUrl.observe(viewLifecycleOwner) { url ->
            if (url != null) {
                uploadedImageUrl = url
                userViewModel.clearUploadedImageUrl()
            }
        }
    }

    private fun saveProfile() {
        if (isSaving) return

        val displayName = binding.etDisplayName.text?.toString()?.trim() ?: ""

        if (!validateInput(displayName)) return

        isSaving = true
        userViewModel.updateProfile(displayName, uploadedImageUrl)
    }

    private fun validateInput(displayName: String): Boolean {
        if (displayName.isEmpty()) {
            binding.tilDisplayName.error = getString(R.string.error_empty_name)
            return false
        }
        binding.tilDisplayName.error = null
        return true
    }

    private fun observeOperationState() {
        userViewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UserOperationState.Loading -> showLoading(true)
                is UserOperationState.Success -> handleSuccess()
                is UserOperationState.Error -> handleError(state.message)
                is UserOperationState.Idle -> showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }

    private fun handleSuccess() {
        if (isSaving) {
            isSaving = false
            showLoading(false)
            userViewModel.resetOperationState()
            findNavController().popBackStack()
        }
    }

    private fun handleError(message: String) {
        isSaving = false
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        userViewModel.resetOperationState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

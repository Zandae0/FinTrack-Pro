package signin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.aicomsapp.R
import com.example.aicomsapp.databinding.FragmentLoginBinding
import com.example.aicomsapp.di.Injection
import com.example.aicomsapp.preference.UserPreference
import com.example.aicomsapp.preference.dataStore
import com.example.aicomsapp.viewmodels.ViewModelFactory
import com.example.aicomsapp.viewmodels.dashboard.DashboardCombinedFragment
import com.example.aicomsapp.viewmodels.shared.SharedUserViewModel
import com.example.aicomsapp.viewmodels.signin.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import resetpass.ForgotPasswordFragment

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LoginViewModel
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupWindowInsets()
        setupLoginButton()
        setupForgotPasswordButton()
        handleBackButton()
        setupGoogleSignIn()
        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
        }

        // Check if user is already logged in and navigate to dashboard if true
        if (isUserLoggedIn()) {
            val userStatus = getUserStatus()
            navigateToDashboard(userStatus)
        }
    }

    private fun setupViewModel() {
        val retrofitRepository = Injection.provideRetrofitRepository(requireContext())
        val userPreference = UserPreference.getInstance(requireContext().dataStore)
        val factory = ViewModelFactory(requireContext(), retrofitRepository, userPreference)
        viewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }
    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val identifier = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (identifier.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Please enter both username and password", Toast.LENGTH_SHORT).show()
            } else {
                performLogin(identifier, password)
            }
        }
    }

    private fun performLogin(identifier: String, password: String) {
        Log.d("LoginFragment", "Attempting to login with identifier: $identifier")

        viewModel.login(
            identifier,
            password,
            onSuccess = { response ->
                Log.d("LoginFragment", "Login successful. Response: ${Gson().toJson(response)}")

                val userId = response.data?.userId
                val token = response.data?.token
                val userStatus = response.data?.userStatus
                val name = response.data?.name
                val role = response.data?.role
                val email = response.data?.email

                if (!userId.isNullOrBlank() && !token.isNullOrBlank() && userStatus != null) {
                    Log.d("LoginFragment", "User ID retrieved: $userId")

                    // Save login session and user status
                    saveLoginSession(userId,  token, userStatus, name, role, email)

                    val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedUserViewModel::class.java)
                    sharedViewModel.setUserStatus(userStatus)
                    sharedViewModel.setUID(userId)

                    navigateToDashboard(userStatus)
                } else {
                    Log.e("LoginFragment", "User ID or token is null")
                    Toast.makeText(requireContext(), "Login failed: Missing required data", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Log.e("LoginFragment", "Login failed with error: $error")
                Toast.makeText(requireContext(), "Login failed: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveLoginSession(userId: String, token: String, userStatus: Int, name: String?, role: String?, email: String?) {
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        Log.d("LoginFragment", "Saving session: userId=$userId, role=$role, email=$email") // Log untuk cek data yang disimpan
        with(sharedPref.edit()) {
            putString("TOKEN", token) // Save the token to keep the session
            putInt("USER_STATUS", userStatus) // Save user status
            putString("USER_NAME", name)
            putString("USER_ID", userId)
            putString("USER_ROLE", role)
            putString("USER_EMAIL", email)
            apply() // Apply changes asynchronously
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("TOKEN", null) != null // Check if token exists in SharedPreferences
    }

    private fun getUserStatus(): Int {
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getInt("USER_STATUS", -1) // Get the saved user status, default is -1 if not found
    }

    private fun navigateToDashboard(userStatus: Int) {
        // Navigate to Dashboard fragment
        val dashboardFragment = DashboardCombinedFragment().apply {
            arguments = Bundle().apply {
                putInt("USER_STATUS", userStatus)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, dashboardFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupForgotPasswordButton() {
        binding.textView2.setOnClickListener {
            // Navigate to ForgotPassword fragment
            val forgotPasswordFragment = ForgotPasswordFragment()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, forgotPasswordFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun logout() {
        // Clear SharedPreferences on logout
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear() // Clear all saved session data
            apply()
        }

        // Navigate back to login screen
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Exit the app
                requireActivity().finish()
            }
        })
    }
    companion object {
        private const val GOOGLE_SIGN_IN_REQUEST_CODE = 9001
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                performGoogleSignIn(idToken)
            }
        } catch (e: ApiException) {
            Log.e("LoginFragment", "Google sign-in failed: ${e.statusCode}")
        }
    }
    private fun performGoogleSignIn(idToken: String) {
        viewModel.googleSignIn(idToken,
            onSuccess = { response ->
                val userId = response.data?.userId
                val token = response.data?.token
                val userStatus = response.data?.userStatus
                val name = response.data?.name
                val role = response.data?.role
                val email = response.data?.email

                if (!userId.isNullOrBlank() && !token.isNullOrBlank() && userStatus != null) {
                    saveLoginSession(userId, token, userStatus, name, role, email)
                    navigateToDashboard(userStatus)
                } else {
                    Toast.makeText(requireContext(), "Login failed: Missing required data", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Log.e("LoginFragment", "Google sign-in failed with error: $error")
                Toast.makeText(requireContext(), "Google sign-in failed: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

}

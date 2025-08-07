package com.example.aicomsapp.viewmodels.dashboard

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.MediaStore
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.aicomsapp.R
import com.example.aicomsapp.adapters.ImprestFundAdapter
import com.example.aicomsapp.databinding.FragmentDashboardCombinedBinding
import com.example.aicomsapp.di.Injection
import com.example.aicomsapp.viewmodels.detailpageimprest.DetailImprestFragment
import com.example.aicomsapp.viewmodels.inputimprest.InputImprestFragment
import com.example.aicomsapp.viewmodels.labcash.LabCashFragment
import com.example.aicomsapp.viewmodels.note.NoteFragment
import com.example.aicomsapp.viewmodels.profile.UserProfileFragment
import com.example.aicomsapp.viewmodels.usercontrol.UserControlFragment
import com.example.aicomsapp.viewmodels.response.ImprestFund
import com.example.aicomsapp.viewmodels.shared.SharedUserViewModel
import com.example.aicomsapp.viewmodels.views.BottomNavBar
import com.example.aicomsapp.viewmodels.views.BottomNavListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.datepicker.MaterialDatePicker
import signin.LoginFragment
import java.io.File
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardCombinedFragment : Fragment(), BottomNavListener {

    private var _binding: FragmentDashboardCombinedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardCombinedViewModel
    private var selectedImageFile: File? = null
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private var balance: Double = 0.0
    private var userStatus: Int = 0
    private var beforeDate: String? = null
    private var afterDate: String? = null


    private var backPressedTime: Long = 0
    private lateinit var toast: Toast

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardCombinedBinding.inflate(inflater, container, false)
        return binding.root
    }
    private var dialogView: View? = null
    private var dialog: AlertDialog? = null
    private var selectedImprestFund: ImprestFund? = null
    private var fullImprestFundlist: List<ImprestFund> = emptyList()
    private lateinit var adapter: ImprestFundAdapterDashboard
    private fun getUserStatusFromPreferences(): Int {
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getInt("USER_STATUS", 0) // 0 sebagai default jika tidak ada
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userStatus = getUserStatusFromPreferences()
        Log.d("DashboardCombinFragment", "User status from preferences: $userStatus")
        val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedUserViewModel::class.java)
        // Ambil user status dari arguments
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val userName = sharedPref.getString("USER_NAME", "Guest")
        val userId = sharedPref.getString("USER_ID", "No ID")
        val email = sharedPref.getString("USER_EMAIL", "")
        Log.d("Dashboard", "Saved email: $email, userStatus: $userStatus")
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri = data?.data
                if (selectedImageUri != null) {
                    selectedImageFile = File(getRealPathFromURI(selectedImageUri))
                    Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
                    dialogView?.let { view ->
                        val selectedImageView = view.findViewById<ImageView>(R.id.selectedImageView)
                        val imagePickerButton = view.findViewById<Button>(R.id.buttonChooseFile)

                        selectedImageView.setImageURI(selectedImageUri)
                        selectedImageView.visibility = View.VISIBLE
                        imagePickerButton.visibility = View.GONE
                    }// Reopen dialog to display selected image
                } else {
                    Toast.makeText(requireContext(), "Image selection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        adapter = ImprestFundAdapterDashboard(
            listOf(),
            onItemClick = { imprestFund -> navigateToDetail(imprestFund) },
            userStatus = userStatus,
            onPictureClick = { imprestFund -> showUploadDialog(imprestFund) },
            onPhotoSudahClick = { imprestFund -> showEnlargedSudahImageDialog(imprestFund) },
            onTransactionTypeChange = { imprestFund, newTransactionType ->
                updateTransactionType(imprestFund, newTransactionType)
            },
            onStatusChangeClick = { imprestFund -> showStatusChangeDialog(imprestFund) }
        )
        binding.imprestFundsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.imprestFundsRecyclerView.adapter = adapter
        // Set user name from shared preferences
        val roleAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.Filter, // your array defined in strings.xml
            R.layout.filter_selected_item // custom layout for selected item (main view)
        )
        roleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.filterSpinner.adapter = roleAdapter
        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                sortImprestFundsList(selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        binding.tvFromDate.setOnClickListener {
            showDatePickerDialog(binding.tvFromDate) { selectedDate ->
                beforeDate = selectedDate // Store in the format 'yyyy-MM-dd' for filtering
                filterImprestFund(binding.searchView.query.toString()) // Trigger filter
            }
        }

        binding.tvToDate.setOnClickListener {
            showDatePickerDialog(binding.tvToDate) { selectedDate ->
                afterDate = selectedDate // Store in the format 'yyyy-MM-dd' for filtering
                filterImprestFund(binding.searchView.query.toString()) // Trigger filter
            }
        }

            sortImprestFundsList("Nama") // Sort by "Nama" initially
        // Set nama pengguna ke TextView
        binding.userName.text = userName
        // Setup ViewModel
        setupViewModel()

        // Setup Bottom Navigation Bar menggunakan BottomNavBar
        setupBottomNavigationBar()

        // Setup UI berdasarkan status user
        updateUIBasedOnUserStatus()

        setupProfileMenu()

        // Fetch Imprest Funds untuk ditampilkan
        fetchImprestFunds ()
        setupSearchView()


    }
    override fun onResume() {
        super.onResume()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun handleBackPressed() {
        if (backPressedTime + 5000 > System.currentTimeMillis()) {
            toast.cancel()
            requireActivity().finish() // Keluar dari aplikasi
        } else {
            toast = Toast.makeText(requireContext(), "Press back again to exit the application", Toast.LENGTH_SHORT)
            toast.show()
        }
        backPressedTime = System.currentTimeMillis() // Set waktu back ditekan pertama kali
    }

    private fun setupProfileMenu() {
        val profileImageView = binding.profilePicture

        // Set onClickListener for the ImageView
        profileImageView.setOnClickListener {
            // Create PopupMenu
            val popupMenu = PopupMenu(ContextThemeWrapper(requireContext(), R.style.PopupMenuStyle), profileImageView)
            popupMenu.inflate(R.menu.popup_menu_profile) // Inflate the menu resource

            // Set a click listener for menu item clicks
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_user_profile -> {
                        // Navigate to UserProfileFragment (if exists)
                        val userProfileFragment = UserProfileFragment()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, userProfileFragment)
                            .addToBackStack(null)
                            .commit()
                        true
                    }

                    R.id.menu_logout -> {
                        // Handle logout logic
                        performLogout()
                        true
                    }

                    else -> false
                }
            }

            // Show the popup menu
            popupMenu.show()
        }
    }
    private fun setupViewModel() {
        val retrofitRepository = Injection.provideRetrofitRepository(requireContext())
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(DashboardCombinedViewModel::class.java) -> {
                        DashboardCombinedViewModel(retrofitRepository) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
        viewModel = ViewModelProvider(this, factory).get(DashboardCombinedViewModel::class.java)
    }
    private fun setupBottomNavigationBar() {
        // Gunakan BottomNavBar untuk mengatur navigasi
        val bottomNavBar = BottomNavBar(binding.bottomBar.root, this) // Menggunakan 'this' sebagai listener
    }

    private fun updateUIBasedOnUserStatus() {
        Log.d("DashboardCombinFragment", "Updating UI, userStatus: $userStatus")
        if (userStatus == 1) {
            // Admin
            binding.saldoContainer.visibility = View.VISIBLE
            binding.bottomBar.userControl.visibility = View.VISIBLE
            binding.bottomBar.kasAicomsButton.visibility = View.VISIBLE
        } else {
            // General User
            binding.saldoContainer.visibility = View.GONE
            binding.bottomBar.userControl.visibility = View.GONE
            binding.bottomBar.kasAicomsButton.visibility = View.GONE
        }
    }
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0 // Menghilangkan desimal
        return format.format(amount).replace("Rp", "Rp.")
    }
    //perhitungan saldo berdasarkan transactionType
    private fun updateTransactionType(imprestFunds: ImprestFund, newTransactionType: String) {
        // Check if photoSudah exists to determine initial balance adjustment
        balance -= if (imprestFunds.status == "undone") {
            when (imprestFunds.transactionType) {
                "in" -> imprestFunds.amount  // Add to balance if type is "in" and undone
                "out" -> -imprestFunds.amount // Subtract from balance if type is "out" and undone
                else -> 0.0
            }
        } else {
            0.0 // Neutralize balance if status to done
        }

        // Update transactionType and neutralize effect if photo is uploaded
        balance += if (imprestFunds.status == "undone") {
            when (newTransactionType) {
                "in" -> imprestFunds.amount  // Add to balance if type is "in" with undone
                "out" -> -imprestFunds.amount // Subtract if type is "out" with undone
                else -> 0.0
            }
        } else {
            0.0 // Neutralize balance if status to done
        }

        // Update transaction type and balance display
        imprestFunds.transactionType = newTransactionType
        binding.tvSaldoAmount.text = formatCurrency(balance)
        binding.imprestFundsRecyclerView.adapter?.notifyDataSetChanged()
    }
    //untuk ngesorting
    private fun filterAndSortImprestFund(
        query: String? = null,
        criteria: String? = null
    ) {
        // Step 1: Apply date range filter
        val dateFilteredList = filterByDateRange()

        // Step 2: Apply search filter (name search)
        val nameFilteredList = if (query.isNullOrEmpty()) {
            dateFilteredList
        } else {
            dateFilteredList.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Step 3: Apply sorting based on criteria
        val sortedList = when (criteria) {
            "Nama" -> nameFilteredList.sortedBy { it.name }
            "Tanggal" -> nameFilteredList.sortedByDescending { it.inputDate }
            "Uang" -> nameFilteredList.sortedBy { it.amount }
            "Status" -> nameFilteredList.sortedWith(compareBy {
                when (it.status) {
                    "undone" -> 0
                    "done" -> 1
                    else -> 2
                }
            })
            else -> nameFilteredList // Default to no sorting if criteria is not specified
        }

        // Update the adapter with the final list
        adapter.updateData(sortedList)
    }

    //buat searching
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search query submission
                filterAndSortImprestFund(query = query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle text changes for real-time filtering
                filterAndSortImprestFund(query = newText)
                return true
            }
        })// Change the color of the text in the SearchView to black
        binding.searchView.apply {
            val searchEditText = findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            searchEditText?.setTextColor(Color.BLACK)
            searchEditText?.setHintTextColor(Color.GRAY)
        }

    }
    private fun filterByDateRange(): List<ImprestFund> {
        // Date format for parsing Firestore dates
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Parse beforeDate and afterDate if they're set, for accurate comparison
        val beforeDateParsed = beforeDate?.let { dateFormat.parse(it) }
        val afterDateParsed = afterDate?.let { dateFormat.parse(it) }

        return fullImprestFundlist.filter { item ->
            val itemDate = dateFormat.parse(item.inputDate)

            // Check if itemDate is within the date range, inclusive
            (beforeDateParsed == null || itemDate == beforeDateParsed || itemDate.after(beforeDateParsed)) &&
                    (afterDateParsed == null || itemDate == afterDateParsed || itemDate.before(afterDateParsed))
        }
    }

    private fun sortImprestFundsList(newCriteria: String) {
        filterAndSortImprestFund(criteria = newCriteria)
    }
    // Function to filter imprestfund items based on the query
    private fun filterImprestFund(query: String?) {
        val dateFilteredList = filterByDateRange()
        val filteredList = if (query.isNullOrEmpty()) {
            dateFilteredList
        } else {
            dateFilteredList.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateData(filteredList)
    }

    private fun onSortCriteriaChanged(criteria: String) {
        val dateFilteredList = filterByDateRange()
        val sortedList = when (criteria) {
            "Nama" -> fullImprestFundlist.sortedBy { it.name }
            "Tanggal" -> fullImprestFundlist.sortedByDescending { it.inputDate }
            "Uang" -> fullImprestFundlist.sortedBy { it.amount }
            "Status" -> fullImprestFundlist.sortedWith(compareBy {
                when (it.status) {
                    "undone" -> 0
                    "done" -> 1
                    else -> 2
                }
            })
            else -> fullImprestFundlist
        }
        adapter.updateData(sortedList)
    }
    private fun fetchImprestFunds() {
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("USER_EMAIL", "")
        viewModel.getImprestFunds(
            onSuccess = { ImprestFundsList ->
                fullImprestFundlist = if (userStatus == 1) {
                    ImprestFundsList
                } else {
                    ImprestFundsList.filter { it.email == userEmail }
                }
                balance = 0.0

                // Hitung saldo berdasarkan transactionType
                fullImprestFundlist.forEach { imprestFund ->
                    balance += if (imprestFund.status == "undone") {
                        when (imprestFund.transactionType) {
                            "in" -> imprestFund.amount  // Add if type is "in" and undone
                            "out" -> -imprestFund.amount // Subtract if type is "out" and undone
                            else -> 0.0
                        }
                    } else {
                        0.0 // Neutralize if status to done
                    }
                }

                // Set saldo ke TextView
                binding.tvSaldoAmount.text = formatCurrency(balance)
                adapter.updateData(fullImprestFundlist)
                sortImprestFundsList("Nama")
            },
            onError = {
                Toast.makeText(requireContext(), "Failed to fetch imprest funds", Toast.LENGTH_SHORT).show()
            }
        )
    }
    private fun navigateToDetail(imprestFunds: ImprestFund) {
        val detailFragment = DetailImprestFragment()
        val bundle = Bundle().apply {
            putString("EXTRA_ID", imprestFunds.id)
            putString("EXTRA_NAME", imprestFunds.name)
            putDouble("EXTRA_AMOUNT", imprestFunds.amount)
            putString("EXTRA_INPUT_DATE", imprestFunds.inputDate)
            putString("EXTRA_SOURCE", imprestFunds.source)
            putString("EXTRA_PIC", imprestFunds.pic)
            putString("EXTRA_TRANSACTION_DATE", imprestFunds.transactionDate)
            putString("EXTRA_PURPOSE", imprestFunds.purpose)
            putString("EXTRA_IMAGE_URI", imprestFunds.photoURL)
            putString("EXTRA_TRANSACTION_TYPE", imprestFunds.transactionType)
            putString("EXTRA_EMAIL", imprestFunds.email)
            putString("EXTRA_ANOA", imprestFunds.AnoA)
        }
        Log.d("DashboardFragment", "Photo URI: ${imprestFunds.photoURL}")
        detailFragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateUIWithImprestFunds(imprestFunds: List<ImprestFund>) {
        binding.imprestFundsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ImprestFundAdapter(imprestFunds)
        binding.imprestFundsRecyclerView.adapter = adapter
    }

    private fun showStatusChangeDialog(imprestFunds: ImprestFund) {
        selectedImprestFund = imprestFunds

        // Define the updated status based on the current status
        val newStatus = if (imprestFunds.status == "undone") "done" else "undone"

        // Create a simple confirmation dialog
        dialog = AlertDialog.Builder(requireContext())
            .setMessage("Apakah Anda ingin mengubah statusnya?")
            .setNegativeButton("Tidak") { dialogInterface, _ ->
                // Dismiss the dialog without making any changes
                dialogInterface.dismiss()
            }
            .setPositiveButton("Ya") { _, _ ->
                // Update status only, without requiring a photo upload
                updateImprestFund(
                    id = imprestFunds.id,
                    name = imprestFunds.name,
                    inputDate = imprestFunds.inputDate,
                    purpose = imprestFunds.purpose,
                    transactionDate = imprestFunds.transactionDate,
                    amount = imprestFunds.amount,
                    source = imprestFunds.source,
                    pic = imprestFunds.pic,
                    transactionType = imprestFunds.transactionType,
                    status = newStatus,
                    photo = null, // No photo upload required
                    photoSudah = null, // Only updating status
                    email = null,
                    AnoA = null
                )

                // Refresh data after updating status
                viewModel.uploadStatus.observe(viewLifecycleOwner) { statusMessage ->
                    Toast.makeText(requireContext(), statusMessage, Toast.LENGTH_SHORT).show()
                    fetchImprestFunds()
                    dialog?.dismiss()
                }
            }
            .create()

        dialog?.show()
    }

    private fun showUploadDialog(imprestFunds: ImprestFund) {
        selectedImprestFund = imprestFunds
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_upload_image, null)
        dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Upload Image")
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                // Clear selected image when canceled and dismiss dialog
                selectedImageFile = null
                dialogInterface.dismiss()
            }

            .setPositiveButton("Upload") { _, _ ->
                selectedImageFile?.let {
                    val updatedTransactionType = if (imprestFunds.transactionType == "in") "out" else "in"
                    val updatedStatus = "done"
                    updateImprestFund(
                        id = imprestFunds.id,
                        name = imprestFunds.name,
                        inputDate = imprestFunds.inputDate,
                        purpose = imprestFunds.purpose,
                        transactionDate = imprestFunds.transactionDate,
                        amount = imprestFunds.amount,
                        source = imprestFunds.source,
                        pic = imprestFunds.pic,
                        transactionType = updatedTransactionType,
                        status = updatedStatus,
                        photo = null,
                        photoSudah = it,
                        email = imprestFunds.email,
                        AnoA = imprestFunds.AnoA// Only photoSudah is required
                    )
                }
            }
            .create()

        val imagePickerButton = dialogView?.findViewById<Button>(R.id.buttonChooseFile)
        val selectedImageView = dialogView?.findViewById<ImageView>(R.id.selectedImageView)


        // Set up image picker button
        selectedImageView?.visibility = if (selectedImageFile != null) View.VISIBLE else View.GONE
        imagePickerButton?.visibility = if (selectedImageFile != null) View.GONE else View.VISIBLE

        imagePickerButton?.setOnClickListener {
            selectImage()
        }
        selectedImageView?.setOnClickListener {
            showEnlargedImageDialog()
        }

        dialog?.setOnDismissListener {
            selectedImageFile = null
            dialogView = null
            dialog = null
        }

        dialog?.show()
        viewModel.uploadStatus.observe(viewLifecycleOwner) { statusMessage ->
            Toast.makeText(requireContext(), statusMessage, Toast.LENGTH_SHORT).show()
            fetchImprestFunds()
            dialog?.dismiss()
        }

    }
    //membesarkan foto yang dipilih
    private fun showEnlargedImageDialog() {
        val enlargedImageDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enlarged_image, null)
        val enlargedImageView = enlargedImageDialogView.findViewById<ImageView>(R.id.enlargedImageView)

        // Display selected image in enlarged view
        selectedImageFile?.let { file ->
            enlargedImageView.setImageURI(Uri.fromFile(file))
        }

        AlertDialog.Builder(requireContext())
            .setView(enlargedImageDialogView)
            .setPositiveButton("Close", null)
            .create()
            .show()
    }
    //membesarkan foto yang sudah diupload
    private fun showEnlargedSudahImageDialog(imprestFunds: ImprestFund) {
        val photoSudahUri = imprestFunds.photoSUDAH
        if (!photoSudahUri.isNullOrEmpty()) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enlarge_detail, null)
            val enlargedImageView = dialogView.findViewById<ImageView>(R.id.enlargedImageView)
            Log.d("Fragment", "photoSUDAH URI: $photoSudahUri")

            // Load the image using Glide
            Glide.with(this)
                .load(photoSudahUri)
                .error(R.drawable.error_404)
                .into(enlargedImageView)

            // Create and show the dialog
            AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()
                .show()
        } else {
            Log.e("Fragment", "Error: photoSUDAH URI is empty or null")
            Toast.makeText(requireContext(), "No image available", Toast.LENGTH_SHORT).show()
        }
    }


    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    // Fungsi tambahan untuk mendapatkan path sebenarnya dari URI
    private fun getRealPathFromURI(uri: Uri): String {
        var filePath = ""
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            if (idx != -1) {
                filePath = cursor.getString(idx)
            }
            cursor.close()
        }
        return filePath
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun updateImprestFund(
        id: String,
        name: String = "",
        inputDate: String = "",
        purpose: String = "",
        transactionDate: String = "",
        amount: Double = .0,
        source: String = "",
        pic: String = "",
        transactionType: String = "",
        status: String = "",
        photo: File? = null,
        photoSudah: File? = null,
        email: String? = null,
        AnoA: String? = null
    ) {
        viewModel.updateImprestFund(id, name, inputDate, purpose, transactionDate, amount, source, pic, transactionType,status, photo, photoSudah, email, AnoA)
    }

    private fun showDatePickerDialog(textView: TextView, onDateSelected: (String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setTheme(R.style.CustomDatePickerTheme)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selectedDate
            }

            // Format date for internal use (yyyy-MM-dd)
            val internalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedInternalDate = internalFormat.format(calendar.time)

            // Format date for display (dd/MM/yyyy)
            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDisplayDate = displayFormat.format(calendar.time)

            // Set the display format in the TextView
            textView.text = formattedDisplayDate

            // Pass the internal format to the filtering function
            onDateSelected(formattedInternalDate)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }


    // Implementasi listener untuk navigasi dari BottomNavBar

    override fun onHomeClicked() {
        // Navigasi ke DashboardCombinedFragment lagi
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DashboardCombinedFragment())
            .commit()
    }

    override fun onUserControlClicked() {
        // Navigasi ke UserControlActivity
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, UserControlFragment())
            .commit()
    }

    override fun onFabPlusClicked() {
        // Arahkan ke InputImprestFragment saat FAB diklik
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, InputImprestFragment()) // Ganti fragment saat ini
            .addToBackStack(null) // Tambahkan ke backstack untuk navigasi kembali
            .commit()
    }

    override fun onLabCashClicked() {
        // Navigasi ke UserControlActivity
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LabCashFragment())
            .commit()
    }

    override fun onNoteClicked() {
        // Retrieve the UID from SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", null)

        if (userId != null) {
            // Create a new instance of NoteFragment
            val noteFragment = NoteFragment()

            // Pass the UID to NoteFragment using Bundle
            val bundle = Bundle().apply {
                putString("USER_ID", userId)
                putInt("USER_STATUS", userStatus)
            }
            noteFragment.arguments = bundle

            // Navigate to NoteFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, noteFragment)
                .addToBackStack(null)
                .commit()
        } else {
            // Handle the case where UID is not found
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        // Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Sign out from Google
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Clear user data from SharedPreferences
                val sharedPref = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    remove("TOKEN")
                    remove("USER_STATUS")
                    apply()
                }

                // Navigate back to LoginFragment
                val loginFragment = LoginFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, loginFragment)
                    .commit()

                // Show a toast message
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to log out from Google", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Jika ada tombol lain seperti FAB Plus, Kas AICOMS, Note, bisa ditambahkan di sini
}

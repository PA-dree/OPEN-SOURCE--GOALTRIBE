package com.functions.goaltribe

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var localDb: GoalDatabaseHelper

    private var userListener: ListenerRegistration? = null
    private var goalsListener: ListenerRegistration? = null

    private lateinit var tvWelcomeBack: TextView
    private lateinit var tvXp: TextView
    private lateinit var tvCoins: TextView
    private lateinit var rvMyGoals: RecyclerView
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var avatarImageView: ImageView

    private lateinit var rvTribes: RecyclerView
    private lateinit var tribeAdapter: TribeAdapter
    private val tribeList = mutableListOf<Tribe>()

    private lateinit var recyclerViewChallenges: RecyclerView
    private lateinit var challengeAdapter: ChallengeAdapter
    private val challenges = mutableListOf<Challenge>()

    private lateinit var goalAdapter: GoalAdapter
    private var selectedAvatarBase64: String? = null
    private val PICK_IMAGE_REQUEST = 2001

    private var translator: Translator? = null
    private var currentLanguage = "en"

    private val user get() = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("GoalTribePrefs", MODE_PRIVATE)
        currentLanguage = prefs.getString("language", "en") ?: "en"

        FirebaseApp.initializeApp(this)
        localDb = GoalDatabaseHelper(this)
        // Firebase setup
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Tribe RecyclerView
        rvTribes = findViewById(R.id.rv_tribe_avatars)
        tribeAdapter = TribeAdapter(tribeList) { tribe ->
            Toast.makeText(this, "Selected tribe: ${tribe.name}", Toast.LENGTH_SHORT).show()
        }
        rvTribes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvTribes.adapter = tribeAdapter

        // UI Initialization
        tvWelcomeBack = findViewById(R.id.tv_welcome_back)
        tvXp = findViewById(R.id.tv_xp)
        tvCoins = findViewById(R.id.tv_coins)
        rvMyGoals = findViewById(R.id.rv_my_goals)
        fabAddGoal = findViewById(R.id.fab_add_goal)
        avatarImageView = findViewById(R.id.iv_user_avatar_header)

        avatarImageView.setOnClickListener { showSettingsDialog() }

        // Goals RecyclerView
        goalAdapter = GoalAdapter(mutableListOf())
        rvMyGoals.layoutManager = LinearLayoutManager(this)
        rvMyGoals.adapter = goalAdapter

        // Load authenticated data
        checkAuthenticationAndLoadData()

        fabAddGoal.setOnClickListener { showAddMenuDialog() }
    }

    // ---------------- ADD MENU ----------------
    private fun showAddMenuDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_options, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        dialog.setContentView(dialogView)
        translateDialogViews(dialogView, mapOf(
            R.id.btnAddGoal to "Add Goal",
            R.id.btnAddChallenge to "Add Challenge",
            R.id.btnAddTribe to "Add Tribe"
        ))

        val btnAddGoal = dialogView.findViewById<LinearLayout>(R.id.btnAddGoal)
        val btnAddChallenge = dialogView.findViewById<LinearLayout>(R.id.btnAddChallenge)
        val btnAddTribe = dialogView.findViewById<LinearLayout>(R.id.btnAddTribe)

        btnAddGoal.setOnClickListener {
            dialog.dismiss()
            showAddGoalDialog()
        }

        btnAddChallenge.setOnClickListener {
            dialog.dismiss()
            showAddChallengeDialog()
        }

        btnAddTribe.setOnClickListener {
            dialog.dismiss()
            showCreateTribeDialog()
        }

        dialog.show()
    }

    private fun showCreateTribeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tribe, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        translateDialogViews(dialogView, mapOf(
            R.id.etTribeName to "Tribe Name",
            R.id.etTribeDescription to "Tribe Description",
            R.id.spTribeCategory to "Tribe Category",
            R.id.imgTribeAvatar to "Tribe Avatar",
            R.id.btnSelectTribeAvatar to "Select Avatar",
            R.id.btnCancelTribe to "Cancel",
            R.id.btnCreateTribe to "Create Tribe"
        ))

        val etTribeName = dialogView.findViewById<EditText>(R.id.etTribeName)
        val etTribeDescription = dialogView.findViewById<EditText>(R.id.etTribeDescription)
        val spTribeCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spTribeCategory)
        val imgTribeAvatar = dialogView.findViewById<ImageView>(R.id.imgTribeAvatar)
        val btnSelectAvatar = dialogView.findViewById<Button>(R.id.btnSelectTribeAvatar)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelTribe)
        val btnCreate = dialogView.findViewById<Button>(R.id.btnCreateTribe)

        val categories = listOf("Fitness", "Study", "Lifestyle", "Career", "Personal Growth")
        spTribeCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories))

        btnSelectAvatar.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_GET_CONTENT)
            pickIntent.type = "image/*"
            startActivityForResult(pickIntent, PICK_IMAGE_REQUEST)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnCreate.setOnClickListener {
            val name = etTribeName.text.toString().trim()
            val desc = etTribeDescription.text.toString().trim()
            val category = spTribeCategory.text.toString().trim()

            if (name.isEmpty() || desc.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser ?: return@setOnClickListener
            val tribeRef = db.collection("tribes").document()
            val newTribe = Tribe(
                id = tribeRef.id,
                name = name,
                description = desc,
                category = category,
                createdBy = currentUser.uid,
                avatarBase64 = selectedAvatarBase64
            )

            tribeRef.set(newTribe)
                .addOnSuccessListener {
                    Toast.makeText(this, "Tribe created!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun loadTribesFromFirestore() {
        db.collection("tribes")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val tribes = snapshot?.toObjects(Tribe::class.java) ?: emptyList()
                tribeAdapter.updateList(tribes)
            }
    }

    // ---------------- CHALLENGES ----------------
    private fun showAddChallengeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_challenge, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        translateDialogViews(dialogView, mapOf(
            R.id.etChallengeTitle to "Challenge Title",
            R.id.etChallengeDescription to "Challenge Description",
            R.id.btnSaveChallenge to "Save",
            R.id.btnCancelChallenge to "Cancel"
        ))

        val etTitle = dialogView.findViewById<EditText>(R.id.etChallengeTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etChallengeDescription)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveChallenge)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelChallenge)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDescription.text.toString().trim()
            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveChallengeToFirestore(title, desc)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun saveChallengeToFirestore(title: String, desc: String) {
        val currentUser = auth.currentUser ?: return
        val challengeRef = db.collection("challenges").document()
        val challenge = Challenge(challengeRef.id, title, desc, currentUser.uid)

        challengeRef.set(challenge)
            .addOnSuccessListener { Toast.makeText(this, "Challenge posted!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun setupChallengeRecyclerView() {
        recyclerViewChallenges = findViewById(R.id.rv_activity_feed)
        challengeAdapter = ChallengeAdapter(challenges)
        recyclerViewChallenges.layoutManager = LinearLayoutManager(this)
        recyclerViewChallenges.adapter = challengeAdapter
        loadChallenges()
    }

    private fun loadChallenges() {
        db.collection("challenges")
            .whereEqualTo("createdBy", user?.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.w(TAG, "Listen failed.", e); return@addSnapshotListener }
                val list = mutableListOf<Challenge>()
                snapshot?.forEach { doc -> list.add(doc.toObject(Challenge::class.java).copy(id = doc.id)) }
                challenges.clear()
                challenges.addAll(list)
                challengeAdapter.updateList(challenges)
            }
    }

    // ---------------- AUTH & DATA ----------------
    private fun checkAuthenticationAndLoadData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        } else {
            ensureUserDocument()
            listenToUserData(currentUser.uid)
            listenToGoals(currentUser.uid)
            listenToUserProfile(currentUser.uid)
            loadTribesFromFirestore()
            setupChallengeRecyclerView()
        }
    }

    private fun ensureUserDocument() {
        val user = auth.currentUser ?: return
        val userRef = db.collection("users").document(user.uid)
        val defaultData = mapOf("name" to (user.displayName ?: user.email?.substringBefore('@') ?: "User"), "xp" to 0, "coins" to 0)
        userRef.get().addOnSuccessListener { if (!it.exists()) userRef.set(defaultData) }
    }

    private fun listenToUserData(uid: String) {
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e(TAG, "Error listening to user data: $error"); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    val xp = snapshot.getLong("xp") ?: 0
                    val coins = snapshot.getLong("coins") ?: 0
                    val name = snapshot.getString("name") ?: "User"
                    tvWelcomeBack.text = "Welcome back, $name!"
                    tvXp.text = xp.toString()
                    tvCoins.text = coins.toString()
                }
            }
    }

    private fun listenToGoals(uid: String) {
        goalsListener?.remove()
        goalsListener = db.collection("users").document(uid).collection("goals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to goals: $error");
                    goalAdapter.updateGoals(localDb.getAllGoals())
                    Toast.makeText(this,"Showing offline Goals", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val goals = snapshot?.documents?.mapNotNull { it.toObject<Goal>()?.copy(id = it.id) } ?: emptyList()
                goalAdapter.updateGoals(goals)
            }

    }

    private fun listenToUserProfile(uid: String) {
        val userRef = db.collection("users").document(uid)
        userListener?.remove()
        userListener = userRef.addSnapshotListener { snapshot, e ->
            if (e != null) { Log.e(TAG, "Listen failed: ", e); return@addSnapshotListener }
            if (snapshot != null && snapshot.exists()) {
                val base64Avatar = snapshot.getString("avatarBase64")
                val name = snapshot.getString("name") ?: "User"
                findViewById<TextView>(R.id.tvUserName)?.text = name
                if (!base64Avatar.isNullOrEmpty()) {
                    val decodedBytes = Base64.decode(base64Avatar, Base64.DEFAULT)
                    avatarImageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size))
                } else avatarImageView.setImageResource(R.drawable.ic_avatar)
            }
        }
    }

    // ---------------- SETTINGS ----------------
    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        translateDialogViews(dialogView, mapOf(
            R.id.tvUserName to "User Name",
            R.id.btnEditProfile to "Edit Profile",
            R.id.btnChangePassword to "Change Password",
            R.id.rbEnglish to "English",
            R.id.rbZulu to "Zulu",
            R.id.btnLogout to "Logout"
        ))

        val tvUserName = dialogView.findViewById<TextView>(R.id.tvUserName)
        val btnEditProfile = dialogView.findViewById<LinearLayout>(R.id.btnEditProfile)
        val btnChangePassword = dialogView.findViewById<LinearLayout>(R.id.btnChangePassword)
        val radioEnglish = dialogView.findViewById<RadioButton>(R.id.rbEnglish)
        val radioZulu = dialogView.findViewById<RadioButton>(R.id.rbZulu)
        val radioAfrikaans = dialogView.findViewById<RadioButton>(R.id.rbAfrikaans)
        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)

        tvUserName.text = user?.displayName ?: "Anonymous"

        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnChangePassword.setOnClickListener {
            FirebaseAuth.getInstance().sendPasswordResetEmail(user?.email ?: "")
                .addOnSuccessListener { Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { Toast.makeText(this, "Failed to send email: ${it.message}", Toast.LENGTH_SHORT).show() }
        }

        radioEnglish.setOnClickListener { currentLanguage = "en"; setLocale("en"); Toast.makeText(this, "English selected", Toast.LENGTH_SHORT).show() }
        radioZulu.setOnClickListener { currentLanguage = "zu"; setLocale("zu"); Toast.makeText(this, "Zulu selected", Toast.LENGTH_SHORT).show() }
        radioAfrikaans.setOnClickListener { currentLanguage = "af"; setLocale("af"); Toast.makeText(this, "Afrikaans selected", Toast.LENGTH_SHORT).show() }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            dialog.dismiss()
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        translateDialogViews(dialogView, mapOf(
            R.id.etDisplayName to "Display Name",
            R.id.imgAvatarEdit to "Avatar",
            R.id.btnChangeAvatar to "Change Avatar",
            R.id.btnSaveProfile to "Save"
        ))

        val imgAvatar = dialogView.findViewById<ImageView>(R.id.imgAvatarEdit)
        val btnChangeAvatar = dialogView.findViewById<Button>(R.id.btnChangeAvatar)
        val etDisplayName = dialogView.findViewById<EditText>(R.id.etDisplayName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProfile)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
            etDisplayName.setText(doc.getString("name") ?: "")
            val avatarBase64 = doc.getString("avatarBase64")
            if (!avatarBase64.isNullOrEmpty()) {
                val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgAvatar.setImageBitmap(bitmap)
            }
        }

        btnChangeAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSave.setOnClickListener {
            val newName = etDisplayName.text.toString().trim()
            if (newName.isEmpty()) {
                etDisplayName.error = "Please enter a name"
                return@setOnClickListener
            }

            val updates = mutableMapOf<String, Any>("name" to newName)
            selectedAvatarBase64?.let { base64 -> updates["avatarBase64"] = base64 }

            db.collection("users").document(currentUser.uid).update(updates)
                .addOnSuccessListener { Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show(); dialog.dismiss() }
                .addOnFailureListener { Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Restart the activity to apply the language change immediately
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // ---------------- ADD GOAL ----------------
    private fun showAddGoalDialog() {
        val user = auth.currentUser ?: run { Toast.makeText(this, "Please sign in to add a goal.", Toast.LENGTH_SHORT).show(); checkAuthenticationAndLoadData(); return }
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_goal)
        translateDialogViews(dialog.window?.decorView ?: return, mapOf(
            R.id.etGoalName to "Goal Name",
            R.id.cbRun to "Run Habit",
            R.id.swStretch to "Stretch Habit",
            R.id.tvTargetDate to "Target Date",
            R.id.etMotivation to "Motivation",
            R.id.btnSaveGoal to "Save"
        ))

        val etGoalName = dialog.findViewById<EditText>(R.id.etGoalName)
        val cbRun = dialog.findViewById<CheckBox>(R.id.cbRun)
        val swStretch = dialog.findViewById<Switch>(R.id.swStretch)
        val tvTargetDate = dialog.findViewById<TextView>(R.id.tvTargetDate)
        val etMotivation = dialog.findViewById<EditText>(R.id.etMotivation)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveGoal)

        var selectedDate = ""
        tvTargetDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> selectedDate = "$d/${m + 1}/$y"; tvTargetDate.text = selectedDate }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            val goalName = etGoalName.text.toString().trim()
            if (goalName.isEmpty()) { etGoalName.error = "Please enter a goal name"; return@setOnClickListener }
            val newGoal = Goal(goalName = goalName, motivation = etMotivation.text.toString().trim(), runHabit = cbRun.isChecked, stretchHabit = swStretch.isChecked, targetDate = selectedDate, progress = 0)
            db.collection("users").document(user.uid).collection("goals").add(newGoal)
                .addOnSuccessListener { Toast.makeText(this, "Goal saved!", Toast.LENGTH_SHORT).show(); dialog.dismiss() }
                .addOnFailureListener {
                    e -> Log.e(TAG, "Error saving goal: ${e.message}");
                    Toast.makeText(this, "Error saving goal: ${e.message}", Toast.LENGTH_LONG).show()

                    val goalSaved = localDb.createGoal(newGoal)
                    if (goalSaved) Toast.makeText(this, "Goal saved locally!", Toast.LENGTH_SHORT).show()
                }



        }

        dialog.show()
    }

    // ---------------- TRANSLATION ----------------
    private fun setupTranslator(toLanguage: String) {
        val targetLang = when (toLanguage.lowercase()) {
            "fr" -> TranslateLanguage.FRENCH
            else -> TranslateLanguage.ENGLISH
        }

        translator?.close()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLang)
            .build()

        translator = Translation.getClient(options)
        translator?.downloadModelIfNeeded()
            ?.addOnSuccessListener { Log.d(TAG, "Translation model ready for $targetLang") }
            ?.addOnFailureListener { e -> Log.e(TAG, "Failed to download translation model", e) }
    }

    private fun translateUI(language: String) {
        setupTranslator(language)
        val translations = mapOf(
            R.id.tv_welcome_back to "Welcome back",
            R.id.tv_xp to "XP",
            R.id.tv_coins to "Coins",
            R.id.tv_my_goals to "My Goals",
            R.id.tv_my_tribe to "My Tribe",
            R.id.tv_shared_challenges to "Shared Challenges",
        )

        lifecycleScope.launch(Dispatchers.IO) {
            translations.forEach { (id, text) ->
                val view = findViewById<View>(id)
                if (view is TextView) {
                    translator?.translate(text)
                        ?.addOnSuccessListener { translated -> runOnUiThread { view.text = translated } }
                        ?.addOnFailureListener { runOnUiThread { view.text = text } }
                }
            }
        }
    }

    // ---------------- ACTIVITY RESULTS ----------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data
            val inputStream = contentResolver.openInputStream(uri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, baos)
            selectedAvatarBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        }
    }

    private fun translateDialogViews(rootView: View, translations: Map<Int, String>) {
        if (currentLanguage == "fr") setupTranslator("fr")

        translations.forEach { (id, defaultText) ->
            val view = rootView.findViewById<View>(id)
            if (view is TextView) {
                if (currentLanguage == "fr") {
                    translator?.translate(defaultText)?.addOnSuccessListener { translated -> view.text = translated }?.addOnFailureListener { view.text = defaultText }
                } else view.text = defaultText
            } else if (view is Button) {
                if (currentLanguage == "fr") {
                    translator?.translate(defaultText)?.addOnSuccessListener { translated -> view.text = translated }?.addOnFailureListener { view.text = defaultText }
                } else view.text = defaultText
            } else if (view is EditText) {
                if (currentLanguage == "fr") {
                    translator?.translate(defaultText)?.addOnSuccessListener { translated -> view.hint = translated }?.addOnFailureListener { view.hint = defaultText }
                } else view.hint = defaultText
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); goalsListener?.remove(); userListener?.remove() }
    override fun onResume() { super.onResume(); checkAuthenticationAndLoadData() }
}

package com.muhammetkdr.instagramclone.view

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.muhammetkdr.instagramclone.databinding.ActivityUploadBinding
import java.util.UUID

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedPicture: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        registerLauncher()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    fun upload(view: View) {

        // universal uniqe id
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"

        val reference = storage.reference      // ' bir adet / ' koymak bir tane daha child açıp alta geçirir
        val imageReference = reference.child("images").child(imageName)

        if (selectedPicture != null) {
            imageReference.putFile(selectedPicture!!).addOnSuccessListener {
                //download url -> firestore
                val uploadPictureReference = storage.reference.child("images").child(imageName)
                uploadPictureReference.downloadUrl.addOnSuccessListener {
                    val downloadUrl = it.toString()
                    if (auth.currentUser != null) {
                        val postMap = hashMapOf<String, Any>()
                        postMap.put("downloadUrl", downloadUrl)
                        postMap.put("userEmail", auth.currentUser!!.email!!)
                        postMap.put("comment",binding.commentText.text.toString() )
                        postMap.put("date",com.google.firebase.Timestamp.now())
                        firestore.collection("Posts").add(postMap).addOnSuccessListener {
                            val intent = Intent(this@UploadActivity,FeedActivity::class.java)
                            startActivity(intent)
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this@UploadActivity,it.localizedMessage,Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
            }

        }
    }

    fun selectImage(view: View) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(view, "Permission Needed for Gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission!") {
                        //request permission
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }.show()
            } else {
                //request permission
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            val intentToGallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //start activity for result
            activityResultLauncher.launch(intentToGallery)
        }
    }

    private fun registerLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        selectedPicture = intentFromResult.data
                        selectedPicture?.let {
                            binding.imageView.setImageURI(it)
                        }
                    }
                }
            }
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    // permission granted
                    val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                } else {
                    // permission denied
                    Toast.makeText(this@UploadActivity, "Permission Denied!", Toast.LENGTH_LONG).show()
                }
            }
    }
}

package com.krshubham.mealsonwheelsbusiness

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_restaurant.*
import java.util.*

class RestaurantActivity : AppCompatActivity() {

    private lateinit var catFilePath: Uri
    private lateinit var foodFilePath: Uri
    private lateinit var storageReference: StorageReference
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var resId: String
    private lateinit var categoryReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        storageReference = FirebaseStorage.getInstance().reference
        firebaseDatabase = FirebaseDatabase.getInstance()

        categoryReference = firebaseDatabase.getReference("category")

        resId = intent.getStringExtra("id")!!
        food_image_select.setOnClickListener {
            pickImageFromGallery(FOOD_IMAGE_PICK)
        }

        category_image_select.setOnClickListener {
            pickImageFromGallery(CAT_IMAGE_PICK)
        }

        save_food.setOnClickListener {

            addRestaurantFood()
        }

    }

    private fun addRestaurantFood() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Uploading...")
        progressDialog.show()
        val catName = category.text.toString()
        val foodName = food.text.toString()
        val ref: StorageReference = storageReference.child("food_images/${foodName}${resId}.jpg")
        ref.putFile(foodFilePath)
            .addOnSuccessListener {
                progressDialog.dismiss()


                val price = price.text.toString()
                val rating = rating.text.toString()
                ref.downloadUrl.addOnSuccessListener {

                    val image = it.toString()
                    val food = Food(foodName, price, rating, image)

                    val foodReference = firebaseDatabase.getReference("food")
                    val foodKey = foodName.toLowerCase(Locale.getDefault()).replace(" ", "") + resId

                    foodReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {

                        }

                        override fun onDataChange(p0: DataSnapshot) {

                            if (p0.hasChild(foodKey)) {

                                val map = mapOf(
                                    "name" to foodName,
                                    "price" to price,
                                    "rating" to rating,
                                    "image" to image
                                )
                                foodReference.child(foodKey).updateChildren(map)
                            } else {

                                foodReference.child(foodKey).setValue(food).addOnSuccessListener {

                                    val catRef =
                                        storageReference.child("category_images/${catName}${resId}.jpg")
                                    catRef.putFile(catFilePath)
                                        .addOnSuccessListener {

                                            catRef.downloadUrl.addOnSuccessListener { cat ->

                                                val catKey =
                                                    catName.toLowerCase(Locale.getDefault()).replace(
                                                        " ",
                                                        ""
                                                    ) + resId
                                                categoryReference.child(catKey).child("name")
                                                    .setValue(catName)
                                                categoryReference.child(catKey).child("image")
                                                    .setValue(cat.toString())
                                                categoryReference.child(catKey).child(foodKey)
                                                    .setValue(true)
                                                    .addOnSuccessListener {

                                                            firebaseDatabase.getReference("restaurantCategories")
                                                                .child(intent.getStringExtra("id")!!).child(catKey).setValue(true)

                                                    }
                                            }


                                        }


                                }
                            }
                        }

                    })


                }



                Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed " + e.message, Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnProgressListener { taskSnapshot ->
                val progress =
                    100.0 * taskSnapshot.bytesTransferred / taskSnapshot
                        .totalByteCount
                progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
            }
    }


    private fun pickImageFromGallery(code: Int) {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        try {
            startActivityForResult(intent, code)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }


    }

    companion object {
        //image pick code
        private const val CAT_IMAGE_PICK = 1000
        private const val FOOD_IMAGE_PICK = 1002
        //Permission code
        private const val PERMISSION_CODE = 1001
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery(requestCode)
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == FOOD_IMAGE_PICK) {
            foodFilePath = data?.data!!
            imageView2.setImageURI(foodFilePath)
        } else if (resultCode == Activity.RESULT_OK && requestCode == CAT_IMAGE_PICK) {
            catFilePath = data?.data!!
            category_image.setImageURI(catFilePath)
        }
    }

}

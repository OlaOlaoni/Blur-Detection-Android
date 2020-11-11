package com.example.blurdetectionrealtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.blurdetectionrealtime.ml.BlurModelQuant
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.support.image.TensorImage
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object{
        private val IMAGE_CHOOSE = 1000;
        private val PERMISSION_CODE = 1001;
        private lateinit var bitmap: Bitmap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Permission to read external storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        take_iamge.setOnClickListener {
            result_txt.text = ""

            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            filePhoto = getPhotoFile(FILE_NAME)

            val providerFile = FileProvider.getUriForFile(this,"com.example.androidcamera.fileprovider", filePhoto)
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
            if (takePhotoIntent.resolveActivity(this.packageManager) != null){
                startActivityForResult(takePhotoIntent, REQUEST_CODE)
            }else {
                Toast.makeText(this,"Camera could not open", Toast.LENGTH_SHORT).show()
            }
        }

        load_image.setOnClickListener {

            try {

                // Read the image as Bitmap
                bitmap = (viewimage.getDrawable() as BitmapDrawable).bitmap

                // We reshape the image into 400*400
                bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

                // Load the model file
                val model = BlurModelQuant.newInstance(baseContext)

                // Creates inputs for reference.
                val image = TensorImage.fromBitmap(bitmap)

                // Runs model inference and gets result.
                val outputs = model.process(image)
                    .probabilityAsCategoryList.apply {
                        sortByDescending { it.score }
                    }.take(2)

                // Create an empty string
                var outputString = ""

                //Take the highest result
                val result = outputs[0]
                outputString = result.label

                runOnUiThread {
                    result_txt.text = translate(outputString)
                }

                // Releases model resources if no longer used.
                model.close()

            } catch (e: IOException) {
                finish()
            }
        }
    }

    private fun detectImage(){
        try {

            // Read the image as Bitmap
            bitmap = (viewimage.getDrawable() as BitmapDrawable).bitmap

            // We reshape the image into 400*400
            bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

            // Load the model file
            val model = BlurModelQuant.newInstance(baseContext)

            // Creates inputs for reference.
            val image = TensorImage.fromBitmap(bitmap)

            // Runs model inference and gets result.
            val outputs = model.process(image)
                .probabilityAsCategoryList.apply {
                    sortByDescending { it.score }
                }.take(2)

            // Create an empty string
            var outputString = ""

            //Take the highest result
            val result = outputs[0]
            outputString = result.label

            runOnUiThread {
                result_txt.text = translate(outputString)
            }

            // Releases model resources if no longer used.
            model.close()

        } catch (e: IOException) {
            finish()
        }
    }

    private fun translate(value: String): String? {
        if (value == "sharp") return "Good"
        if (value == "defocused_blurred") return "Warning: The Image is not Clear"
        return if (value == "motion_blurred") "Warning: The Image is not Clear" else ""
    }

    private fun chooseImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_CHOOSE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    chooseImageGallery()
                }else{
                    Toast.makeText(this,"Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getPhotoFile(fileName: String): File {
        val directoryStorage = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directoryStorage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val takenPhoto = BitmapFactory.decodeFile(filePhoto.absolutePath)
            viewimage.setImageBitmap(takenPhoto)
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            viewimage.setImageURI(data?.data)
        }

    }

   /* @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // This functions return the selected image from gallery
        if (requestCode == IMAGE_CHOOSE && resultCode == Activity.RESULT_OK && null != data) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            if (BuildConfig.DEBUG && selectedImage == null) {
                error("Assertion failed")
            }
            val cursor = contentResolver.query(
                selectedImage!!,
                filePathColumn, null, null, null
            )!!
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            viewimage.setImageBitmap(BitmapFactory.decodeFile(picturePath))

            //Setting the URI so we can read the Bitmap from the image
            viewimage.setImageURI(null)
            viewimage.setImageURI(selectedImage)
        }
    }*/
}

private const val REQUEST_CODE = 13
private lateinit var filePhoto: File
private const val FILE_NAME = "photo.jpg"
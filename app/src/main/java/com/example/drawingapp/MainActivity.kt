package com.example.drawingapp

import android.graphics.Bitmap
import android.app.Dialog
import android.content.ContentValues
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import yuku.ambilwarna.AmbilWarnaDialog

class MainActivity : AppCompatActivity(){
    private lateinit var myBrushSizeButton: ImageButton
    private lateinit var myDrawingView: DrawingView
    private lateinit var myUndoButton: ImageButton
    private lateinit var myColorWheel: ImageButton
    private lateinit var mySaveButton: ImageButton
    private lateinit var purpleColorButton: ImageButton
    private lateinit var redColorButton: ImageButton
    private lateinit var blueColorButton: ImageButton
    private lateinit var greenColorButton: ImageButton
    private lateinit var yellowColorButton: ImageButton
    private lateinit var galleryButton: ImageButton
    private lateinit var imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        purpleColorButton = findViewById(R.id.imageButtonPurple)
        redColorButton = findViewById(R.id.imageButtonRed)
        blueColorButton = findViewById(R.id.imageButtonBlue)
        greenColorButton = findViewById(R.id.imageButtonGreen)
        yellowColorButton = findViewById(R.id.imageButtonYellow)

        val listOfColors = listOf(purpleColorButton, redColorButton, blueColorButton, greenColorButton, yellowColorButton)

        myDrawingView = findViewById(R.id.myDrawingView)
        myBrushSizeButton = findViewById(R.id.imageButtonBrush)
        myUndoButton = findViewById(R.id.imageButtonBack)

        myUndoButton.setOnClickListener {
            myDrawingView.undoPath()
        }

        myColorWheel = findViewById(R.id.imageButtonColor)

        myColorWheel.setOnClickListener {
            generateColorWheelDialog()
        }

        mySaveButton = findViewById(R.id.imageButtonFileSave)

        mySaveButton.setOnClickListener {
            val(bitMap, paths) = myDrawingView.getBitMap()
            if (paths.isNotEmpty()) {
                saveCanvasToGallery(bitMap)
            }else{
                Toast.makeText(this, "Please draw something first", Toast.LENGTH_SHORT).show()
            }
        }

        myBrushSizeButton.setOnClickListener {
            showBrushDialog()
        }

        for(colorButton in listOfColors) {
            colorButton.setOnClickListener {

                for(btn in listOfColors) {
                    btn.isSelected = false
                }

                colorButton.isSelected = true
                val newColor = colorButton.tag.toString()
                myDrawingView.changeBrushColor(newColor)

            }
        }

        galleryButton = findViewById(R.id.imageButtonFileUpload)

        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    myDrawingView.setCanvasBackground(uri)
                } else {
                    Toast.makeText(this, "No image was selected", Toast.LENGTH_SHORT).show()
                }
            }
        galleryButton.setOnClickListener {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

    }


    private fun generateColorWheelDialog() {
        val dialog = AmbilWarnaDialog(this, Color.WHITE, object: AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                Toast.makeText(this@MainActivity, "Default color was set to black", Toast.LENGTH_SHORT).show()
                val defaultColor = Color.BLACK
                myDrawingView.changeBrushColor(defaultColor)
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                myDrawingView.changeBrushColor(color)
            }

        })
        dialog.show()
    }

    private fun saveCanvasToGallery(bitMap: Bitmap) {

        val imageTypeDialog = Dialog(this)
        imageTypeDialog.setContentView(R.layout.png_or_jpeg_file_selector)
        val pngButton = imageTypeDialog.findViewById<Button>(R.id.buttonPNG)
        val jpegButton = imageTypeDialog.findViewById<Button>(R.id.buttonJPEG)

        pngButton.setOnClickListener {
            val fileName = "Drawing${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyDrawings")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val contentResolver = applicationContext.contentResolver

            val imageUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            imageUri?.let { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    outputStream?.use {
                        bitMap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
                Toast.makeText(this, "Image was successfully stored in gallery as a png", Toast.LENGTH_SHORT).show()
            }
            imageTypeDialog.dismiss()
        }

        jpegButton.setOnClickListener {
            val fileName = "Drawing${System.currentTimeMillis()}.jpeg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyDrawings")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val contentResolver = applicationContext.contentResolver

            val imageUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            imageUri?.let { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    outputStream?.use {
                        bitMap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
                Toast.makeText(this, "Image was successfully stored in gallery as a jpeg", Toast.LENGTH_SHORT).show()
            }
            imageTypeDialog.dismiss()
        }

        imageTypeDialog.show()
    }

    private fun showBrushDialog() {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.brush_size_dialog)

        val progressSeekbar = dialog.findViewById<SeekBar>(R.id.seekBarBrushWidth)
        val progressText = dialog.findViewById<TextView>(R.id.textViewBrushSize)

        progressSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                myDrawingView.changeBrushSize(p0?.progress?.toFloat() ?: 0.toFloat() )
                progressText.text = "Stroke Width: ${p0?.progress.toString()}"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
        dialog.show()
    }

}
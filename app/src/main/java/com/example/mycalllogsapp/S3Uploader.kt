package com.example.mycalllogsapp

import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File

class S3Uploader(private val context: Context) {

    fun upload(file: File) {
        try {
            val credentials = BasicAWSCredentials("", "")
            // Using the recommended constructor
            val s3Client = AmazonS3Client(credentials, Region.getRegion(Regions.AP_SOUTH_1))

            val transferUtility = TransferUtility.builder()
                .context(context)
                .s3Client(s3Client)
                .build()

            val uploadObserver = transferUtility.upload("", file.name, file)
            uploadObserver.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    if (state == TransferState.COMPLETED) {
                        Log.d("S3Uploader", "Upload complete")
                        file.delete()
                    } else if (state == TransferState.FAILED) {
                        Log.e("S3Uploader", "Upload failed")
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    val percentDone = (bytesCurrent.toFloat() / bytesTotal.toFloat()) * 100
                    Log.d("S3Uploader", "Progress: $percentDone%")
                }

                override fun onError(id: Int, ex: Exception?) {
                    Log.e("S3Uploader", "Error during upload", ex)
                }
            })
        } catch (e: Exception) {
            Log.e("S3Uploader", "Failed to upload file", e)
        }
    }
}

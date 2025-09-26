package br.tmvdl.android.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import br.tmvdl.android.camera.databinding.FragmentSecondBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.webrtc.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var webRTCService: WebRTCService? = null
    private var isStreaming = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && audioGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera and audio permissions are required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        binding.buttonStartStream.setOnClickListener {
            startWebRTCStream()
        }

        binding.buttonStopStream.setOnClickListener {
            stopWebRTCStream()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        webRTCService = WebRTCService(requireContext())
        webRTCService?.initialize()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.qrCameraPreview.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrResult ->
                        requireActivity().runOnUiThread {
                            binding.qrResultText.text = "QR Code: $qrResult"
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startWebRTCStream() {
        if (!isStreaming) {
            webRTCService?.createPeerConnection()

            val videoSink = object : VideoSink {
                override fun onFrame(frame: VideoFrame?) {
                    // Handle video frame for streaming
                }
            }

            webRTCService?.startVideoCapture(videoSink)

            webRTCService?.createOffer { sessionDescription ->
                Log.d(TAG, "Created offer: ${sessionDescription.description}")
                // In a real implementation, you would send this offer to a signaling server
                // and exchange it with a remote peer
            }

            isStreaming = true
            binding.buttonStartStream.isEnabled = false
            binding.buttonStopStream.isEnabled = true

            Toast.makeText(requireContext(), "WebRTC stream started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopWebRTCStream() {
        if (isStreaming) {
            webRTCService?.stopVideoCapture()
            isStreaming = false
            binding.buttonStartStream.isEnabled = true
            binding.buttonStopStream.isEnabled = false

            Toast.makeText(requireContext(), "WebRTC stream stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopWebRTCStream()
        webRTCService?.dispose()
        cameraExecutor.shutdown()
        _binding = null
    }

    private inner class QRCodeAnalyzer(private val listener: (String) -> Unit) : ImageAnalysis.Analyzer {

        private val scanner = BarcodeScanning.getClient()

        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            when (barcode.valueType) {
                                Barcode.TYPE_TEXT -> {
                                    listener(barcode.displayValue ?: "Unknown QR Code")
                                }
                                Barcode.TYPE_URL -> {
                                    listener("URL: ${barcode.url?.url}")
                                }
                                else -> {
                                    listener(barcode.displayValue ?: "QR Code detected")
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "QR Code scanning failed", it)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    companion object {
        private const val TAG = "SecondFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
package br.tmvdl.android.camera

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import br.tmvdl.android.camera.databinding.DialogQrCodeBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.webrtc.VideoSink
import java.util.*

class QRCodeDialogFragment : DialogFragment() {

    private var _binding: DialogQrCodeBinding? = null
    private val binding get() = _binding!!

    private var connectionUrl: String = ""
    private var connectionId: String = ""
    private var webRTCService: WebRTCService? = null
    private var isStreamStarted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogQrCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeWebRTC()
        generateConnectionData()
        generateQRCode()
        startCameraStream()

        binding.buttonClose.setOnClickListener {
            stopCameraStream()
            dismiss()
        }

        binding.buttonShare.setOnClickListener {
            shareConnectionInfo()
        }
    }

    private fun initializeWebRTC() {
        webRTCService = WebRTCService(requireContext())
        webRTCService?.initialize()
    }

    private fun startCameraStream() {
        if (!isStreamStarted) {
            webRTCService?.createPeerConnection()

            val videoSink = object : VideoSink {
                override fun onFrame(frame: org.webrtc.VideoFrame?) {
                    // Video frames ready for streaming
                }
            }

            webRTCService?.startVideoCapture(videoSink)

            webRTCService?.createOffer { sessionDescription ->
                // This offer would be used by connecting devices
                requireActivity().runOnUiThread {
                    binding.connectionInfo.text = "Stream Active - ID: $connectionId"
                }
            }

            isStreamStarted = true
        }
    }

    private fun stopCameraStream() {
        if (isStreamStarted) {
            webRTCService?.stopVideoCapture()
            isStreamStarted = false
        }
    }

    private fun generateConnectionData() {
        // Generate a unique connection ID for this session
        connectionId = "cam_${UUID.randomUUID().toString().take(8)}"

        // In a real implementation, this would be your signaling server URL
        // For demo purposes, we'll create a connection URL with the ID
        connectionUrl = "https://your-webrtc-app.com/connect?id=$connectionId"

        binding.connectionInfo.text = "Connection ID: $connectionId"
    }

    private fun generateQRCode() {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(
                connectionUrl,
                BarcodeFormat.QR_CODE,
                256,
                256
            )
            binding.qrCodeImage.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Toast.makeText(requireContext(), "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareConnectionInfo() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join my camera stream: $connectionUrl")
            putExtra(Intent.EXTRA_SUBJECT, "Camera Stream Connection")
        }

        startActivity(Intent.createChooser(shareIntent, "Share connection"))
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCameraStream()
        webRTCService?.dispose()
        _binding = null
    }

    companion object {
        const val TAG = "QRCodeDialogFragment"

        fun newInstance(): QRCodeDialogFragment {
            return QRCodeDialogFragment()
        }
    }
}
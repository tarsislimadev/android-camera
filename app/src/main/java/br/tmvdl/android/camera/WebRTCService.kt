package br.tmvdl.android.camera

import android.content.Context
import android.util.Log
import org.webrtc.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WebRTCService(private val context: Context) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var localVideoSource: VideoSource? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Log.d(TAG, "onSignalingChange: $p0")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Log.d(TAG, "onIceConnectionChange: $p0")
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            Log.d(TAG, "onIceConnectionReceivingChange: $p0")
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            Log.d(TAG, "onIceGatheringChange: $p0")
        }

        override fun onIceCandidate(p0: IceCandidate?) {
            Log.d(TAG, "onIceCandidate: $p0")
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
            Log.d(TAG, "onIceCandidatesRemoved")
        }

        override fun onAddStream(p0: MediaStream?) {
            Log.d(TAG, "onAddStream: $p0")
        }

        override fun onRemoveStream(p0: MediaStream?) {
            Log.d(TAG, "onRemoveStream: $p0")
        }

        override fun onDataChannel(p0: DataChannel?) {
            Log.d(TAG, "onDataChannel: $p0")
        }

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded")
        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
            Log.d(TAG, "onAddTrack")
        }
    }

    fun initialize() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, peerConnectionObserver)
        return peerConnection
    }

    fun startVideoCapture(videoSink: VideoSink) {
        try {
            videoCapturer = Camera2Enumerator(context).run {
                deviceNames.find { isFrontFacing(it) }?.let { createCapturer(it, null) }
                    ?: deviceNames.find { !isFrontFacing(it) }?.let { createCapturer(it, null) }
            }

            localVideoSource = peerConnectionFactory?.createVideoSource(false)
            videoCapturer?.initialize(
                SurfaceTextureHelper.create("CaptureThread", EglBase.create().eglBaseContext),
                context,
                localVideoSource?.capturerObserver
            )

            localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video", localVideoSource)
            localVideoTrack?.addSink(videoSink)

            videoCapturer?.startCapture(640, 480, 30)

            val stream = peerConnectionFactory?.createLocalMediaStream("local_stream")
            stream?.addTrack(localVideoTrack)
            peerConnection?.addStream(stream)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting video capture", e)
        }
    }

    fun stopVideoCapture() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            localVideoTrack?.dispose()
            localVideoSource?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video capture", e)
        }
    }

    fun createOffer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            callback(it)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Create offer failed: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Set local description failed: $error")
            }
        }, constraints)
    }

    fun dispose() {
        stopVideoCapture()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        executor.shutdown()
    }

    companion object {
        private const val TAG = "WebRTCService"
    }
}
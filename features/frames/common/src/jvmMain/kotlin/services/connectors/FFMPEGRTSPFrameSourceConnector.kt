package space.kscience.frameswork.features.frames.common.services.connectors

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.meta.buildMetaContainer
import korlibs.time.DateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import space.kscience.frameswork.features.frames.common.models.*
import space.kscience.frameswork.features.frames.common.models.javacv.BufferedImageFrameData
import space.kscience.frameswork.features.frames.common.services.FrameSourceConnector
import kotlin.time.Duration.Companion.milliseconds


/**
 * JVM frame-source connector that reads an RTSP stream through JavaCV's FFmpeg grabber.
 *
 * Each allocated flow owns a grabber configured for RTSP over TCP. Frames are converted to
 * [BufferedImageFrameData] and annotated with source, dimensions, source timestamp, and receive time.
 * Failed start attempts are retried after [reconnectDelayMillis]. Cancelling collection stops and
 * releases the grabber.
 *
 * @param id identifier attached to emitted frame metadata.
 * @param rtspUrl RTSP URL passed to the FFmpeg grabber.
 * @param reconnectDelayMillis delay before retrying a failed grabber start, in milliseconds.
 */
class FFMPEGRTSPFrameSourceConnector(
    override val id: FramesSourceId,
    private val rtspUrl: String,
    private val reconnectDelayMillis: Long = 1000L
) : FrameSourceConnector {
    private val toMatConverter: ToMat = ToMat()
    private val converter = Java2DFrameConverter()

    /**
     * Allocates a cold flow backed by a dedicated FFmpeg grabber.
     *
     * @return a flow of JPEG-capable buffered-image frames from the RTSP stream.
     */
    override fun allocateFlow(): Flow<FrameData> {
        val grabber = FFmpegFrameGrabber(rtspUrl).apply {
            timeout = 5000000
            format = "rtsp"
            setOption("rtsp_transport", "tcp")
//        setOption("stimeout", "5000000")  // 5s socket timeout (microseconds)
            setOption("buffer_size", "1024000")
        }
        return flow<FrameData> {
            while (true) {
                runCatching {
                    grabber.start()
                }.onFailure {
                    delay(reconnectDelayMillis.milliseconds)
                    continue
                }
                runCatchingLogging {
                    while (true) {
                        val frame: Frame? = grabber.grabImage()
                        if (frame == null) break
                        // Converting to buffered image
                        emit(
                            BufferedImageFrameData(
                                converter.convert(frame),
                                buildMetaContainer {
                                    put(FramesSourceIdMeta, id)
//                                    put(FrameSourceMat, toMatConverter.convert(frame))
                                    put(FrameSourceWidth, frame.imageWidth)
                                    put(FrameSourceHeight, frame.imageHeight)
                                    put(FrameSourceTimestampMicroseconds, frame.timestamp)
                                    put(FrameReceiveTimestamp, DateTime.now())
                                }
                            )
                        )
                    }
                }
                grabber.stop()
                grabber.release()
            }
        }.onCompletion {
            grabber.stop()
            grabber.release()
        }
    }

    /** Returns the serializable RTSP configuration represented by this connector. */
    override fun createConfig(): FrameSourceConnectorConfig = FFMPEGRTSPConfig(id.string, rtspUrl)
}

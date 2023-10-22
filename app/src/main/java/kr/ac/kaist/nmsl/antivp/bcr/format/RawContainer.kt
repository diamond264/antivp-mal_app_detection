package kr.ac.kaist.nmsl.antivp.bcr.format

import android.media.MediaCodec
import android.media.MediaFormat
import android.system.Os
import android.system.OsConstants
import java.io.FileDescriptor
import java.nio.ByteBuffer

class RawContainer(private val fd: FileDescriptor) : Container {
    private var isStarted = false
    private var track = -1
    private var frameSize = 0
    private var channelCount = 0
    private var sampleRate = 0

    override fun start() {
        if (isStarted) {
            throw IllegalStateException("Container already started")
        }

        Os.ftruncate(fd, 0)

        Os.lseek(fd, 0, OsConstants.SEEK_SET)

        isStarted = true
    }

    override fun stop() {
        if (!isStarted) {
            throw IllegalStateException("Container not started")
        }

        isStarted = false
    }

    override fun release() {
        if (isStarted) {
            stop()
        }
    }

    override fun addTrack(mediaFormat: MediaFormat): Int {
        if (isStarted) {
            throw IllegalStateException("Container already started")
        } else if (track >= 0) {
            throw IllegalStateException("Track already added")
        }

        track = 0
        frameSize = mediaFormat.getInteger(Format.KEY_X_FRAME_SIZE_IN_BYTES)
        channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        return track
    }

    override fun writeSamples(trackIndex: Int, byteBuffer: ByteBuffer,
                              bufferInfo: MediaCodec.BufferInfo) {
        if (!isStarted) {
            throw IllegalStateException("Container not started")
        } else if (track < 0) {
            throw IllegalStateException("No track has been added")
        } else if (track != trackIndex) {
            throw IllegalStateException("Invalid track: $trackIndex")
        }

        Os.write(fd, byteBuffer)
    }
}
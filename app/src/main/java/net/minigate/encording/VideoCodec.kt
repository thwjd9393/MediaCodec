package net.minigate.encording

import android.content.Context
import android.media.*
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.InvalidParameterException


class VideoCodec(context: Context) {

    private val VIDEO_MIME_TYPE = "video/avc" //libx264

    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var mediaCodec: MediaCodec
    private lateinit var outputFile: File

    init {
        try {
            // 출력 파일 및 믹서 설정
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)

            val timeStamp = (System.currentTimeMillis() / 1000).toString()
            val outputFileName = "encoded_${timeStamp}_mystory.mp4" // 인코딩된 동영상 파일 이름

            Log.d("TAG", " outputFileName = ${outputDir}")

            outputFile = File(outputDir, outputFileName)
            mediaMuxer = MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // assets 폴더에서 동영상 파일을 가져옵니다.
            val assetVideoFileName = "1690856714260_myStory_1.mp4" // assets 폴더에 있는 동영상 파일 이름
            val inputStream = context.assets.open(assetVideoFileName)
            Log.d("TAG", " assetVideoFileName = ${assetVideoFileName}")

            // 인코딩을 시작합니다.
            encodeVideo(inputStream)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun encodeVideo(inputStream: InputStream) {

        try {
            val width = 1920 // 동영상 가로 해상도
            val height = 1080 // 동영상 세로 해상도
            val bitRate = 6000000 // 비트레이트 (bps)
            val frameRate = 30 // 프레임 레이트 (fps)

            val encoderFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height)
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5) // 프레임 간격 설정

            mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            mediaCodec.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex: Int

            val bufferSize = width * height * 3 / 2 // YUV420 데이터 크기

            val buffer = ByteArray(bufferSize)
            val inputBuffer = ByteBuffer.allocate(bufferSize)

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead < 0) {
                    break
                }

                inputBuffer.clear()
                inputBuffer.put(buffer, 0, bytesRead)
                inputBuffer.flip()

                val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)

                Log.d("TAG", "inputBufferIndex = ${inputBufferIndex}")

                if (inputBufferIndex >= 0) {
                    val inputBufferArray = mediaCodec.getInputBuffer(inputBufferIndex)
                    inputBufferArray?.put(inputBuffer)

                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytesRead, 0, 0)
                }

                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, -1)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                    outputBuffer?.position(bufferInfo.offset)
                    outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)

                    // 비디오 데이터를 믹서에 쓰기
                    if (outputBuffer != null) {
                        mediaMuxer.writeSampleData(0, outputBuffer, bufferInfo)
                    }

                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }

                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            // 인코딩 및 믹서 작업 완료 후 정리
            mediaCodec.stop()
            mediaCodec.release()
            mediaMuxer.stop()
            mediaMuxer.release()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun release() {

        try {
            mediaCodec.stop()
            mediaCodec.release()
            mediaMuxer.stop()
            mediaMuxer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
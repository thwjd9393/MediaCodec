package net.minigate.encording

import android.content.Context
import android.media.*
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

class VideoEncoder(context: Context) {

    private val VIDEO_MIME_TYPE = "video/avc" //libx264
//    private val outputFileName = "encoded_video4.mp4"

    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var mediaCodec: MediaCodec
    private lateinit var outputFile: File

    init {
        try {
            // 출력 파일 및 믹서 설정
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)

            val timeStamp = (System.currentTimeMillis() / 1000).toString()
            var outputFileName = "before_${timeStamp}_mystory.mp4"
            outputFile = File(outputDir, outputFileName)

            //비디오 및 오디오 트랙을 결합하여 최종 비디오 파일을 생성하는 데 사용
            Log.d("TAG", " outputDir = ${outputDir}")

            // assets 파일에서 파일 열기
            val assetFileDescriptor = context.assets.openFd("1690856714260_myStory_1.mp4") ///30초
//            val assetFileDescriptor = context.assets.openFd("1694423932472_myStory_1.mp4") // 5초

            mediaMuxer = MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4) //초기화

            val extractor = MediaExtractor() //비디오 파일로부터 인코드된 비디오 프레임을 얻는 역할, 비디오 추출기
            extractor.setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
            Log.d("TAG", "fileDescriptor = ${assetFileDescriptor.fileDescriptor}")
            Log.d("TAG", "startOffset = ${assetFileDescriptor.startOffset}")
            Log.d("TAG", "length = ${assetFileDescriptor.length}")
            Log.d("TAG", "extractor = ${extractor}")

            val trackIndex = selectTrack(extractor)
            Log.d("TAG", "trackIndex = ${trackIndex}")

            // 비디오 포맷 설정
            val format = extractor.getTrackFormat(trackIndex)
            val width = format.getInteger(MediaFormat.KEY_WIDTH)
            val height = format.getInteger(MediaFormat.KEY_HEIGHT)
            val bitRate = 13500000 // 비트레이트 (bps)
            val frameRate = 30 // 프레임 레이트 (fps)

            val encoderFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height)
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5) //프레임 간격 설정

            mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE) //초기화
            mediaCodec.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            // Surface의 경우는 디코딩시에만 적용,인코딩시에는 MediaCodec.CONFIGURE_FLAG_ENCODE을 지정하고, 디코딩 시에는 0을 설정
            mediaCodec.start()

            encodeVideo(extractor, trackIndex)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // 비디오 프레임 추출 - 비디오 트랙을 선택하고 해당 트랙의 인덱스를 반환하는 역할
    // 여러 트랙 중에서 비디오 트랙을 찾아내고 선택하는 데 사용
    private fun selectTrack(extractor: MediaExtractor): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                extractor.selectTrack(i)
                Log.d("TAG", "mime = ${mime}")
                Log.d("TAG", "i = ${i}")
                return i
            }
        }
        return -1
    }


    private fun encodeVideo(extractor: MediaExtractor, trackIndex: Int) {
        val info = MediaCodec.BufferInfo()
        Log.d("TAG","info = ${info.size}")
        var outputBufferIndex: Int

        while (true) {
            Log.d("TAG","aaaaaaaaaaaaaaaaaSS")
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, -1)
            Log.d("TAG","obi = ${outputBufferIndex}")
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                continue
            }
            if (outputBufferIndex >= 0) {
                Log.d("TAG","bbbbbbbbbbbbbbbbbbbbbbbbbbb")
                val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                val data = ByteArray(info.size)

                outputBuffer?.get(data)
                outputBuffer?.clear()

                // 비디오 데이터를 믹서에 쓰기
                mediaMuxer.writeSampleData(trackIndex, ByteBuffer.wrap(data), info)
                mediaMuxer.start()
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

                // 모든 프레임을 인코딩한 경우 종료
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("TAG", "outputBufferIndex = ${outputBufferIndex}")
                    break
                }
            }
        }

        // 인코딩된 비디오를 저장
//        val timeStamp = (System.currentTimeMillis() / 1000).toString()
//        var outputFileName = "${timeStamp}_mystory.mp4"
//        val encodedVideoFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), outputFileName)
        val encodedVideoFile = File(outputFile.path) // 수정
        Log.d("TAG", " encodedVideoFile ${encodedVideoFile}")

        outputFile.copyTo(encodedVideoFile, true)

        mediaCodec.stop()
        mediaCodec.release()
        mediaMuxer.stop()
        mediaMuxer.release()

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
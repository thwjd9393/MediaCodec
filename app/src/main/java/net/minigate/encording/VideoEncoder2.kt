package net.minigate.encording

import android.content.Context
import android.content.res.AssetManager
import android.media.*
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class VideoEncoder2(context: Context) {

    private val VIDEO_MIME_TYPE = "video/avc" //libx264
//    private val outputFileName = "encoded_video4.mp4"

    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var mediaCodec: MediaCodec

    init {
        try {
            // 출력 파일 및 믹서 설정
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)

            val timeStamp = (System.currentTimeMillis() / 1000).toString()
//            var outputFileName = "before_${timeStamp}_mystory.mp4"

            val outputFilePath: String = context.filesDir.toString() + File.separator + "before_" + timeStamp + ".mp4"
            val videoFile: String = context.filesDir.toString() + File.separator + "after_" + timeStamp + ".mp4"

            val inputAssetFileName = "1690856714260_myStory_1.mp4" //30초짜리 영상

            val assetVideoFilePath = copyAssetVideoToInternalStorage(inputAssetFileName, context)
            Log.d("TAG","assetVideoFilePath = ${assetVideoFilePath}") //복사한 파일 경로

            if (assetVideoFilePath != null) {
                mediaMuxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4) // 초기화

//                Log.d("TAG","mediaMuxer : outputFile = ${outputFilePath}")

                val extractor = MediaExtractor() // 비디오 파일로부터 인코드된 비디오 프레임을 얻는 역할, 비디오 추출기
                extractor.setDataSource(assetVideoFilePath) // 앱 내부 저장소에 복사한 파일 경로 전달

                val trackIndex = selectTrack(extractor)
                Log.d("TAG", "trackIndex = $trackIndex")

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
                encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5) // 프레임 간격 설정

                mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
                mediaCodec.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                mediaCodec.start()

                encodeVideo(extractor, trackIndex, context)
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun copyAssetVideoToInternalStorage(
        assetFileName: String,
        context: Context
    ): String? {
        return try {
            val assetManager: AssetManager = context.assets
            val inputStream = assetManager.open(assetFileName)
            val outputPath = context.filesDir.toString() + File.separator + assetFileName
            val outputStream = FileOutputStream(outputPath)
            Log.d("TAG assetFileName" +
                    " = ", assetFileName)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
            outputPath // 복사된 파일의 경로 반환
        } catch (e: IOException) {
            Log.e("TAG", "Asset 파일을 앱 내부 저장소로 복사 중 오류 발생: " + e.message)
            null // 복사 실패 시 null 반환
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


    private fun encodeVideo(extractor: MediaExtractor, trackIndex: Int, context: Context) {
        val info = MediaCodec.BufferInfo()
        Log.d("TAG","info = ${info}")
        var outputBufferIndex: Int
        var muxerStarted = false // 믹서 시작 여부를 나타내는 플래그

        while (true) {
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, -1)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                continue
            }
            if (outputBufferIndex >= 0) {
                val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                val data = ByteArray(info.size)
                outputBuffer?.get(data)
                outputBuffer?.clear()

                // 믹서를 처음으로 시작하도록 처리
                if (!muxerStarted) {
                    mediaMuxer.start()
                    muxerStarted = true
                }

                // 비디오 데이터를 믹서에 쓰기
                mediaMuxer.writeSampleData(trackIndex, ByteBuffer.wrap(data), info)
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

                // 모든 프레임을 인코딩한 경우 종료
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("TAG", "outputBufferIndex = $outputBufferIndex")
                    break
                }
            }
        }

        // 인코딩된 비디오를 저장
//        val timeStamp = (System.currentTimeMillis() / 1000).toString()
//        var outputFileName = "${timeStamp}_mystory.mp4"
//        val encodedVideoFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), outputFileName)
////        val encodedVideoFile = File(outputFile.path) // 수정
//        Log.d("TAG", " encodedVideoFile ${encodedVideoFile}")
//
//        encodedVideoFile.copyTo(encodedVideoFile, true)
//
//        mediaCodec.stop()
//        mediaCodec.release()
//        mediaMuxer.stop()
//        mediaMuxer.release()

        // 인코딩된 비디오를 저장
        val videoFile: String = context.filesDir.toString() + File.separator
        Log.d("TAG", " videoFile  = $videoFile")
        val encodedVideoFile = File(videoFile) // 수정
        Log.d("TAG", " encodedVideoFile = $encodedVideoFile")

        encodedVideoFile.copyTo(encodedVideoFile, true)

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
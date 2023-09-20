package net.minigate.encording

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var videoEncoder: VideoEncoder
    private lateinit var videoEncoder2: VideoEncoder2
    private lateinit var videoCodec: VideoCodec

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btn : Button = findViewById(R.id.btn)

        btn.setOnClickListener {
//            videoEncoder = VideoEncoder(this)
//            videoEncoder.release()

            videoCodec = VideoCodec(this)
            videoCodec.release()

//            videoEncoder2 = VideoEncoder2(this)
//            videoEncoder2.release()

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        videoEncoder.release()
    }
}
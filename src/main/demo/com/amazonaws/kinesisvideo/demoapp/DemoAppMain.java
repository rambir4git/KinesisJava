package com.amazonaws.kinesisvideo.demoapp;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.demoapp.contants.DemoTrackInfos;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSource;
import com.amazonaws.kinesisvideo.java.client.KinesisVideoJavaClientFactory;
import com.amazonaws.kinesisvideo.java.mediasource.file.AudioVideoFileMediaSource;
import com.amazonaws.kinesisvideo.java.mediasource.file.AudioVideoFileMediaSourceConfiguration;
import com.amazonaws.kinesisvideo.java.mediasource.file.ImageFileMediaSource;
import com.amazonaws.kinesisvideo.java.mediasource.file.ImageFileMediaSourceConfiguration;
import com.amazonaws.kinesisvideo.producer.Tag;
import com.amazonaws.regions.Regions;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static com.amazonaws.kinesisvideo.util.StreamInfoConstants.ABSOLUTE_TIMECODES;

/**
 * Demo Java Producer.
 */
public final class DemoAppMain {
    // Use a different stream name when testing audio/video sample
    private static final String STREAM_NAME = "vis_test_rtsp";
    private static final int FPS_25 = 25;
    private static final int RETENTION_ONE_HOUR = 1;
    private static final String IMAGE_DIR = "src/main/resources/data/h264/";
    private static final String FRAME_DIR = "src/main/resources/data/audio-video-frames";
    // CHECKSTYLE:SUPPRESS:LineLength
    // Need to get key frame configured properly so the output can be decoded. h264 files can be decoded using gstreamer plugin
    // gst-launch-1.0 rtspsrc location="YourRtspUri" short-header=TRUE protocols=tcp ! rtph264depay ! decodebin ! videorate ! videoscale ! vtenc_h264_hw allow-frame-reordering=FALSE max-keyframe-interval=25 bitrate=1024 realtime=TRUE ! video/x-h264,stream-format=avc,alignment=au,profile=baseline,width=640,height=480,framerate=1/25 ! multifilesink location=./frame-%03d.h264 index=1
    private static final String IMAGE_FILENAME_FORMAT = "frame-%03d.h264";
    private static final int START_FILE_INDEX = 1;
    private static final int END_FILE_INDEX = 375;

    private DemoAppMain() {
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) {
        try {
            //System.loadLibrary("KinesisVideoProducerJNI");
            // create Kinesis Video high level client
            final AWSCredentialsProvider awsCredentialsProvider = new AWSCredentialsProvider() {
                @Override
                public AWSCredentials getCredentials() {
                    AWSCredentials awsCredentials = new AWSCredentials() {
                        @Override
                        public String getAWSAccessKeyId() {
                            return "AKIAJ3UNGJMU66ZK3ZXQ";
                        }

                        @Override
                        public String getAWSSecretKey() {
                            return "06PgV5LiGrozM8Nf3vUFQVcmXJ8jicp2pArP2q3m";
                        }
                    };
                    return awsCredentials;
                }

                @Override
                public void refresh() {

                }
            };
            final KinesisVideoClient kinesisVideoClient = KinesisVideoJavaClientFactory
                    .createKinesisVideoClient(
                            Regions.AP_SOUTH_1,
                            awsCredentialsProvider);

            // create a media source. this class produces the data and pushes it into
            // Kinesis Video Producer lower level components
            //final MediaSource mediaSource = createImageFileMediaSource();

            // Audio/Video sample is available for playback on HLS (Http Live Streaming)
            //final MediaSource mediaSource = createFileMediaSource();

            //Create GStreamer Video Source for RTSP stream
            final MediaSource mediaSource = createRtspMediaSource();

            // register media source with Kinesis Video Client
            kinesisVideoClient.registerMediaSource(mediaSource);

            // start streaming
            mediaSource.start();
        } catch (final KinesisVideoException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static MediaSource createRtspMediaSource() throws URISyntaxException {

        return new GStreamerRtspMediaSource(
                STREAM_NAME,
                new URI("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_175k.mov"),
                Duration.ofHours(RETENTION_ONE_HOUR),
                new Tag[] { new Tag("Produced-By", "Happy") });
    }

    /**
     * Create a MediaSource based on local sample H.264 frames.
     *
     * @return a MediaSource backed by local H264 frame files
     */
    private static MediaSource createImageFileMediaSource() {
        final ImageFileMediaSourceConfiguration configuration =
                new ImageFileMediaSourceConfiguration.Builder()
                        .fps(FPS_25)
                        .dir(IMAGE_DIR)
                        .filenameFormat(IMAGE_FILENAME_FORMAT)
                        .startFileIndex(START_FILE_INDEX)
                        .endFileIndex(END_FILE_INDEX)
                        //.contentType("video/hevc") // for h265
                        .build();
        final ImageFileMediaSource mediaSource = new ImageFileMediaSource(STREAM_NAME);
        mediaSource.configure(configuration);

        return mediaSource;
    }

    /**
     * Create a MediaSource based on local sample H.264 frames and AAC frames.
     *
     * @return a MediaSource backed by local H264 and AAC frame files
     */
    private static MediaSource createFileMediaSource() {
        final AudioVideoFileMediaSourceConfiguration configuration =
                new AudioVideoFileMediaSourceConfiguration.AudioVideoBuilder()
                        .withDir(FRAME_DIR)
                        .withRetentionPeriodInHours(RETENTION_ONE_HOUR)
                        .withAbsoluteTimecode(ABSOLUTE_TIMECODES)
                        .withTrackInfoList(DemoTrackInfos.createTrackInfoList())
                        .build();
        final AudioVideoFileMediaSource mediaSource = new AudioVideoFileMediaSource(STREAM_NAME);
        mediaSource.configure(configuration);

        return mediaSource;
    }

}

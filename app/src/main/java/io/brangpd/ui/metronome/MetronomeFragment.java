package io.brangpd.ui.metronome;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import io.brangpd.R;
import io.brangpd.databinding.MetronomeFragmentBinding;

public class MetronomeFragment extends Fragment {
    private static final String TAG = MetronomeFragment.class.getName();

    private MetronomeViewModel mViewModel;
    private static final int kMinBpm = 20;
    private static final long kMaxRecordStopMs = bpmToMspb(kMinBpm);
    private static final int kMaxBpm = 300;
    private static final int kInitBpm = 60;
    private Timer mPlayingTimer = null;
    private long mLastRecordedTimeMs;
    private long mLastRecordedStopMs;
    private long mRecordCount;

    public static MetronomeFragment newInstance() {
        return new MetronomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        MetronomeFragmentBinding binding = MetronomeFragmentBinding.inflate(inflater, container, false);

        EditText editTextNumberBpm = binding.editTextNumberBpm;
        SeekBar seekBarBpm = binding.seekBarBpm;
        Button buttonPlay = binding.buttonPlay;
        Button buttonRecord = binding.buttonRecord;
        Button buttonResetRecord = binding.buttonResetRecord;

        mViewModel = new ViewModelProvider(this).get(MetronomeViewModel.class);
        mViewModel.getBpm().observe(getViewLifecycleOwner(), integer -> {
            String bpmStr = integer.toString();
            editTextNumberBpm.setText(bpmStr);
            if (mPlayingTimer != null) {
                mPlayingTimer.cancel();
                mPlayingTimer = new Timer();
            }
            schedulePlayTimer(getContext(), mPlayingTimer, integer);
        });
        mViewModel.isPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            boolean recordOptionEnabled = !isPlaying;
            buttonRecord.setEnabled(recordOptionEnabled);
            buttonResetRecord.setEnabled(recordOptionEnabled);
        });
        editTextNumberBpm.setText(String.valueOf(kInitBpm));

        seekBarBpm.setProgress(kInitBpm);
        seekBarBpm.setMax(bpmToSeekBarProgress(kMaxBpm));
        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int bpm = seekBarProgressToBpm(i);
                editTextNumberBpm.setText(String.valueOf(bpm));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int i = seekBar.getProgress();
                int bpm = seekBarProgressToBpm(i);
                mViewModel.setBpm(bpm);
            }
        });

        buttonPlay.setOnClickListener(view -> {
            Boolean isPlaying = mViewModel.isPlaying().getValue();
            if (isPlaying == null || !isPlaying) {
                buttonPlay.setText(R.string.metronome_stop);
                mViewModel.setPlaying(true);
                if (mPlayingTimer != null) {
                    mPlayingTimer.cancel();
                    mPlayingTimer = null;
                }
                Integer bpm = mViewModel.getBpm().getValue();
                if (bpm != null && bpm != 0) {
                    mPlayingTimer = new Timer();
                    schedulePlayTimer(getContext(), mPlayingTimer, bpm);
                }
            } else {
                buttonPlay.setText(R.string.metronome_play);
                mViewModel.setPlaying(false);
                if (mPlayingTimer != null) {
                    mPlayingTimer.cancel();
                    mPlayingTimer = null;
                }
            }
        });

        buttonRecord.setOnClickListener(view -> {
            playSound(getContext());
            long nowMs = Calendar.getInstance().getTime().getTime();
            long stopMs = nowMs - mLastRecordedTimeMs;
            mLastRecordedTimeMs = nowMs;
            if (stopMs > kMaxRecordStopMs) {
                mRecordCount = 0;
                mLastRecordedStopMs = 0;
                return;
            }
            ++mRecordCount;
            mLastRecordedStopMs = mLastRecordedStopMs + (stopMs - mLastRecordedStopMs) / mRecordCount;
            int bpm = mspbToBpm(mLastRecordedStopMs);
            mViewModel.setBpm(bpm);
        });

        buttonResetRecord.setOnClickListener(view -> {
            mRecordCount = 0;
            mLastRecordedStopMs = 0;
            mLastRecordedTimeMs = 0;
        });

        return binding.getRoot();
    }

    private static int seekBarProgressToBpm(int i) {
        return i + kMinBpm;
    }

    private static int bpmToSeekBarProgress(int bpm) {
        return bpm - kMinBpm;
    }

    private static long bpmToMspb(int v) {
        if (v == 0) {
            return 0;
        }
        float bpm = (float) v;
        float bpms = bpm / 1000 / 60;
        return (long) (1 / bpms);
    }

    private static int mspbToBpm(long v) {
        if (v == 0) {
            return 0;
        }
        return (int) (60 * 1000 / (float) v);
    }

    private static void schedulePlayTimer(Context context, Timer timer, int bpm) {
        if (timer == null) {
            return;
        }
        if (bpm == 0) {
            return;
        }
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                String time = Calendar.getInstance().getTime().toString();
                Log.d(TAG, String.format("run: Tick %s", time));
                playSound(context);
            }
        };
        long mspb = bpmToMspb(bpm);
        Log.d(TAG, String.format("onCreateView: Start Playing at interval %d ms", mspb));
        timer.schedule(timerTask, 0, mspb);
    }

    private static void playSound(Context context) {
        MediaPlayer player = MediaPlayer.create(context, R.raw.metronome_high);
        player.start();
        player.setOnCompletionListener(MediaPlayer::release);
    }
}
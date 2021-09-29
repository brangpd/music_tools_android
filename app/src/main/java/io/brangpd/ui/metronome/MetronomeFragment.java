package io.brangpd.ui.metronome;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import io.brangpd.R;
import io.brangpd.databinding.MetronomeFragmentBinding;

public class MetronomeFragment extends Fragment {
    private static final String TAG = MetronomeFragment.class.getName();

    private MetronomeViewModel mViewModel;
    private static final int kMinBpm = 20;
    private static final int kMaxBpm = 300;
    private static final int kInitBpm = 60;
    private MediaPlayer mHighSoundPlayer;
    private MediaPlayer mLowSoundPlayer;
    private boolean mIsPlaying = false;
    private Timer mPlayingTimer = null;

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

        mHighSoundPlayer = MediaPlayer.create(getActivity(), R.raw.metronome_high);
        mLowSoundPlayer = MediaPlayer.create(getActivity(), R.raw.metronome_low);
        mViewModel = new ViewModelProvider(this).get(MetronomeViewModel.class);
        mViewModel.getBpm().observe(getViewLifecycleOwner(), integer -> {
            String bpmStr = integer.toString();
            editTextNumberBpm.setText(bpmStr);
        });
        editTextNumberBpm.setText(String.valueOf(kInitBpm));

        seekBarBpm.setProgress(kInitBpm);
        seekBarBpm.setMax(bpmToSeekBarProgress(kMaxBpm));
        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int bpm = seekBarProgressToBpm(i);
                mViewModel.setBpm(bpm);
                if (mPlayingTimer != null) {
                    mPlayingTimer.cancel();
                    mPlayingTimer = new Timer();
                }
                schedulePlayTimer(mPlayingTimer, bpm);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        buttonPlay.setOnClickListener(view -> {
            if (!mIsPlaying) {
                buttonPlay.setText(R.string.metronome_stop);
                mIsPlaying = true;
                if (mPlayingTimer != null) {
                    mPlayingTimer.cancel();
                    mPlayingTimer = null;
                }
                int bpm = mViewModel.getBpm().getValue();
                if (bpm != 0) {
                    mPlayingTimer = new Timer();
                    schedulePlayTimer(mPlayingTimer, bpm);
                }
            } else {
                buttonPlay.setText(R.string.metronome_play);
                mIsPlaying = false;
                if (mPlayingTimer != null) {
                    mPlayingTimer.cancel();
                    mPlayingTimer = null;
                }
            }
        });

        buttonRecord.setOnClickListener(view -> {

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

    private void schedulePlayTimer(Timer timer, int bpm) {
        if (timer == null) {
            return;
        }
        if (bpm == 0) {
            return;
        }
        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                Log.d(TAG, "run: Tick");
                playSound();
            }
        };
        long mspb = bpmToMspb(bpm);
        Log.d(TAG, String.format("onCreateView: Start Playing at interval %d ms", mspb));
        timer.scheduleAtFixedRate(timerTask, 0, mspb);
    }

    private void playSound() {
        mHighSoundPlayer.start();
    }
}
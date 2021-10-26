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
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import io.brangpd.R;
import io.brangpd.databinding.MetronomeFragmentBinding;

public class MetronomeFragment extends Fragment {
    private static final String TAG = MetronomeFragment.class.getName();

    private MetronomeViewModel mViewModel;
    public static final int kMinBpm = 20;
    private static final long kMaxRecordStopMs = bpmToMspb(kMinBpm);
    public static final int kMaxBpm = 300;
    public static final int kInitBpm = 60;
    private Timer mPlayingTimer = null;
    private long mLastRecordedTimeMs;
    private long mLastRecordedStopMs;
    private long mRecordCount;
    private boolean mIsPlaying = false;
    private Map<Integer, Integer> mSpeedToBpm;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        MetronomeFragmentBinding binding = MetronomeFragmentBinding.inflate(inflater, container, false);

        EditText editTextNumberBpm = binding.editTextNumberBpm;
        SeekBar seekBarBpm = binding.seekBarBpm;
        Button buttonPlay = binding.buttonPlay;
        Button buttonRecord = binding.buttonRecord;
        Button buttonResetRecord = binding.buttonResetRecord;
        Button buttonChoosePreset = binding.buttonChoosePreset;

        mViewModel = new ViewModelProvider(this).get(MetronomeViewModel.class);
        mViewModel.getBpm().observe(getViewLifecycleOwner(), integer -> {
            String bpmStr = integer.toString();
            editTextNumberBpm.setText(bpmStr);
            seekBarBpm.setProgress(bpmToSeekBarProgress(integer));
            if (mIsPlaying) {
                startPlayingTimer();
            }
        });

        // BPM 编辑区域
        editTextNumberBpm.setText(String.valueOf(kInitBpm));
        editTextNumberBpm.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                mViewModel.setBpm(Integer.parseInt(textView.getText().toString()));
                return true;
            }
            return false;
        });

        seekBarBpm.setProgress(kInitBpm);
        seekBarBpm.setMax(bpmToSeekBarProgress(kMaxBpm));
        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    // 拉动实时显示当前BPM，但是不马上修改BPM
                    editTextNumberBpm.setText(String.valueOf(seekBarProgressToBpm(i)));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 在拉动抬起时真正修改BPM
                int bpm = seekBarProgressToBpm(seekBar.getProgress());
                mViewModel.setBpm(bpm);
            }
        });

        buttonPlay.setOnClickListener(view -> {
            if (!mIsPlaying) {
                buttonPlay.setText(R.string.metronome_stop);
                buttonRecord.setEnabled(false);
                buttonResetRecord.setEnabled(false);
                mIsPlaying = true;
                if (mPlayingTimer != null) {
                    mPlayingTimer.cancel();
                    mPlayingTimer = null;
                }
                Integer bpm = mViewModel.getBpm().getValue();
                if (bpm != null && bpm != 0) {
                    startPlayingTimer();
                }
            } else {
                buttonPlay.setText(R.string.metronome_play);
                buttonRecord.setEnabled(true);
                buttonResetRecord.setEnabled(true);
                mIsPlaying = false;
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

        // 速度预设值
        HashMap<Integer, Integer> speedToBpm = new HashMap<>();
        mSpeedToBpm = speedToBpm;
        speedToBpm.put(R.id.metronome_preset_larghissimo, 20);
        speedToBpm.put(R.id.metronome_preset_adagissimo, 30);
        speedToBpm.put(R.id.metronome_preset_grave, 35);
        speedToBpm.put(R.id.metronome_preset_largo, 50);
        speedToBpm.put(R.id.metronome_preset_lento, 55);
        speedToBpm.put(R.id.metronome_preset_larghetto, 60);
        speedToBpm.put(R.id.metronome_preset_adagio, 70);
        speedToBpm.put(R.id.metronome_preset_adagietto, 75);
        speedToBpm.put(R.id.metronome_preset_andante, 90);
        speedToBpm.put(R.id.metronome_preset_andantino, 95);
        speedToBpm.put(R.id.metronome_preset_moderato, 100);
        speedToBpm.put(R.id.metronome_preset_allegretto, 110);
        speedToBpm.put(R.id.metronome_preset_allegro, 140);
        speedToBpm.put(R.id.metronome_preset_vivace, 160);
        speedToBpm.put(R.id.metronome_preset_vivacissimo, 175);
        speedToBpm.put(R.id.metronome_preset_allegrissimo, 175);
        speedToBpm.put(R.id.metronome_preset_presto, 185);
        speedToBpm.put(R.id.metronome_preset_prestissimo, 205);
        buttonChoosePreset.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(getContext(), view);
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                Integer bpm = mSpeedToBpm.get(menuItem.getItemId());
                if (bpm != null) {
                    Toast.makeText(getContext(), menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                    mViewModel.setBpm(bpm);
                    return true;
                }
                return false;
            });
            popupMenu.inflate(R.menu.metronome_preset);
            popupMenu.show();
        });

        return binding.getRoot();
    }

    private void startPlayingTimer() {
        if (mPlayingTimer != null) {
            mPlayingTimer.cancel();
        }
        mPlayingTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                String time = Calendar.getInstance().getTime().toString();
                Log.d(TAG, String.format("run: Tick %s", time));
                playSound(getContext());
            }
        };
        int bpm = Objects.requireNonNull(mViewModel.getBpm().getValue());
        long mspb = bpmToMspb(bpm);
        Log.d(TAG, String.format("onCreateView: Start Playing at interval %d ms", mspb));
        mPlayingTimer.schedule(timerTask, 0, mspb);
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

    private static void playSound(Context context) {
        MediaPlayer player = MediaPlayer.create(context, R.raw.metronome_high);
        player.start();
        player.setOnCompletionListener(MediaPlayer::release);
    }
}
package io.brangpd.ui.metronome;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MetronomeViewModel extends ViewModel {
    private final MutableLiveData<Integer> bpm;
    private final MutableLiveData<Boolean> isPlaying;

    public MetronomeViewModel() {
        bpm = new MutableLiveData<>(MetronomeFragment.kInitBpm);
        isPlaying = new MutableLiveData<>(false);
    }

    @NonNull
    public LiveData<Integer> getBpmData() {
        return bpm;
    }

    public int getBpm() {
        Integer value = bpm.getValue();
        return value == null ? 0 : value;
    }

    public void setBpm(int v) {
        v = Math.min(v, MetronomeFragment.kMaxBpm);
        v = Math.max(v, MetronomeFragment.kMinBpm);
        bpm.setValue(v);
    }

    @NonNull
    public LiveData<Boolean> isPlayingData() {
        return isPlaying;
    }

    public boolean isPlaying() {
        Boolean value = isPlaying.getValue();
        return value != null ? value : false;
    }

    public void setPlaying(boolean b) {
        isPlaying.setValue(b);
    }
}